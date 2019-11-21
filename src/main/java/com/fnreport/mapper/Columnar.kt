package com.fnreport.mapper

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.Closeable
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.max
import kotlin.math.min


fun stringMapper(): (Any?) -> Any? = { i -> (i as? ByteArray)?.let { String(it).takeIf(String::isNotBlank)?.trim() } }
fun btoa(i: Any?) = (i as? ByteArray)?.let { stringMapper()(it)?.toString() }


fun intMapper(): (Any?) -> Any? = { i -> btoa(i)?.toInt() ?: 0 }
fun floatMapper(): (Any?) -> Any? = { i -> btoa(i)?.toFloat() ?: 0f }
fun doubleMapper(): (Any?) -> Any? = { i -> btoa(i)?.toDouble() ?: 0.0 }
fun longMapper(): (Any?) -> Any? = { i -> btoa(i)?.toLong() ?: 0L }


fun dateMapper(): (Any?) -> Any? = { i ->
    btoa(i)?.let {
        var res: LocalDate?
        try {
            res = LocalDate.parse(it)
        } catch (e: Exception) {
            val parseBest = DateTimeFormatter.ISO_DATE.parseBest(it)
            res = LocalDate.from(parseBest)
        }
        res
    }
}

interface RowStore<T> : Flow<T> {
    /**
     * seek to row
     */
    var values: suspend (Int) -> T

    val size: Int
    @InternalCoroutinesApi
    override suspend fun collect(collector: FlowCollector<T>) {
        val availableProcessors = Runtime.getRuntime().availableProcessors()
        val chunksize = max(size, availableProcessors) / min(size, availableProcessors)
        coroutineScope {
            (0 until size).chunked(chunksize).forEach { range ->
                launch {
                    coroutineScope {
                        range.forEach { launch { collector.emit(values(it)) } }
                    }
                }
            }
        }
    }
}

interface FixedLength<T> : Indexed<T> {
    val recordLen: Int
}

abstract class FileAccess(open val filename: String) : Closeable


//todo: map multiple segments for a very big file
open class MappedFile(
        filename: String,
        randomAccessFile: RandomAccessFile = RandomAccessFile(filename, "r"),
        channel: FileChannel = randomAccessFile.channel,
        length: Long = randomAccessFile.length(),
        val mappedByteBuffer: MappedByteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, length),
        override val size: Int = mappedByteBuffer.limit()
) : FileAccess(filename), RowStore<Flow<ByteBuffer>>, Closeable by randomAccessFile {
    override var values: suspend (Int) -> Flow<ByteBuffer> = { row -> flowOf(mappedByteBuffer.apply { position(row) }.slice()) }
    @InternalCoroutinesApi
    override suspend fun collect(collector: FlowCollector<Flow<ByteBuffer>>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

/**
One-dimensional ndarray with axis labels (including time series).

Labels need not be unique but must be a hashable type. The object supports both integer- and label-based indexing and provides a host of methods for performing operations involving the index. Statistical methods from ndarray have been overridden to automatically exclude missing data (currently represented as NaN).

Operations between Series (+, -, /, , *) align values based on their associated index values– they need not be the same length. The result index will be the sorted union of the two indexes.
 */
interface Indexed<T> {
    operator fun get(vararg rows: Int): T
    operator fun get(rows: IntRange): T = get(* rows.toList().toIntArray())

}
//todo: map multiple segments for a very big file

abstract class LineBuffer : Indexed<Flow<ByteBuffer>>

class FixedRecordLengthFile(filename: String, origin: MappedFile = MappedFile(filename)) : FixedRecordLengthBuffer(origin.mappedByteBuffer),
        Closeable by origin

open class FixedRecordLengthBuffer(val buf: ByteBuffer) : LineBuffer(),
        RowStore<Flow<ByteBuffer>>,
        FixedLength<Flow<ByteBuffer>> {
    override fun get(vararg rows: Int) = rows.map { buf.position(recordLen * it).slice().apply { limit(recordLen) } }.asFlow()
    override var values: suspend (Int) -> Flow<ByteBuffer> = { row -> flowOf(buf.position(recordLen * row).slice().limit(recordLen)) }
    override val recordLen: Int = buf.duplicate().clear().run {
        var c = 0.toByte()
        do c = get() while (c != '\n'.toByte())
        position()
    }
    override val size: Int = (buf.limit() / recordLen)//.also { assert(it != 0) { "bad size" } }
}


suspend fun <A, B> Iterable<A>.pmap(f: suspend (A) -> B): List<B> = coroutineScope {
    map { async { f(it) } }.awaitAll()
}

/**
 * our tight recursive object with flexible column features
 */
@InternalCoroutinesApi
open class Columnar(var rs: RowStore<Flow<ByteBuffer>>, val columns: Array<Pair<String, Pair<Pair<Int, Int>, (Any?) -> Any?>>>) : RowStore<List<*>> {

    override val size: Int = rs.size
    operator fun get(vararg cols: Int): Columnar = this[cols.toList()]
    operator fun get(cols: List<Int>) = Columnar(this.rs, cols.map { columns[it] }.toTypedArray())
    operator fun get(xform: Pair<Iterable<Int>, (Any?) -> Any?>): Unit {
        xform.let { (indices, transform) ->
            indices.map { indice ->
                columns[indice].let { (a, b) ->
                    b.let { (c, rowMapper) ->
                        columns[indice] = a to (c to
                                { any -> transform(rowMapper(any)) })
                    }
                }
            }
        }
    }


    override var values: suspend (Int) -> List<*> = { row: Int ->
        rs.values(row).first().let { rs1 ->
            (columns.map { (a, mapper) ->
                val (coor, conv) = mapper
                val (begin, end) = coor
                val len = end - begin
                val fb = rs1.position(begin).slice().limit(len)
                conv(ByteArray(len).also { fb.get(it) })
            })
        }
    }


    suspend fun pivot(untouched: IntArray, lhs: Int, vararg rhs: Int): Columnar {
        val sets1 = linkedMapOf<Any?, SortedSet<Int>>()

        run {
            this[lhs].run {
                this.collectIndexed { index, row ->
                    val key = row.first()
                    val cluster = sets1[key]
                    when (cluster) {
                        null -> sets1[key] = (sortedSetOf(index))
                        else -> cluster.add(index)
                    }
                }
            }
        }

        val sets = sets1.map { (k, v) -> k to (v to IntArray(1)) }.toMap()
        var c = 0
        untouched.map { columns[it] }.toMutableList().let { revisedColumns ->
            this.columns[lhs].let { (keyPrefix) ->
                sets.entries.map { (k, v) ->
                    v.second[0] = c
                    c++
                    "$keyPrefix:${(k as? ByteArray)?.let { it -> String(it) } ?: k}".let { keyName ->
                        revisedColumns += rhs.map { rhsCol ->

                            this.columns[rhsCol].let { (rhsName, decode) ->
                                val (coords, mapper) = decode


                                ("$keyName,$rhsName") to (coords to mapper)
                            }
                        }
                    }
                }
                val origin = this
                val clusters = sets.map { (k, v) -> k.hashCode() to v.let { (a, b) -> a.toIntArray() to b.first() } }.toMap()
                return object : Columnar(rs, revisedColumns.toTypedArray()) {
                    override var values: suspend (Int) -> List<*> = { row ->
                        //  rs.values(row).let { rs1 ->
                        //      super.columns.mapIndexed { ix, (_, mapper) ->
                        //          val (coor, conv: (Any?) -> Any?) = mapper
                        //          val (begin, end) = coor
                        //          val len = end - begin
                        //          val fb = rs1.first().position(begin).slice().limit(len)
                        //          if (ix < untouched.size) conv(ByteArray(len).also { fb.get(it) })
                        //          else conv(row to ByteArray(len).also { fb.get(it) })
                        //      }
                        //  }
                        val values1 = origin.values(row)

                        val front = untouched.size
                        lateinit var cluster: Pair<IntArray, Int>
                        coroutineScope {
                            sequence {
                                columns.indices.forEach { colNum ->

                                    when {
                                        colNum < front -> yield(values1[untouched[colNum]])
                                        colNum == front -> {
                                            val any = values1[lhs]
                                            val hashCode = any.hashCode()
                                            val pair = clusters[hashCode]
                                            cluster = pair!!
                                        }
                                        colNum > front -> {
                                            val lower = cluster.second * rhs.size
                                            val intRange = lower until (lower + rhs.size)
                                            val adjusted = colNum - front
                                            yield(if (adjusted in intRange) {
                                                values1[rhs[(adjusted - intRange.first)]]
                                            } else null)
                                        }
                                    }
                                }
                            }.toList()
                        }
                    }
                }
            }
        }
    }

    @InternalCoroutinesApi
    class GroupColumnar(private val origin: Columnar, override val size: Int, private val originClusters: Array<IntArray>,
                        private val gby: IntArray) : Columnar(origin.rs, origin.columns) {
        override var values: suspend (Int) -> List<*> = { row ->
            originClusters[row].let { cluster ->
                cluster.first().let { keyRowNum ->
                    (origin.values(keyRowNum) to (cluster.map { gRow ->
                        origin.values(gRow)
                    }).map { it }).let { (keyRow, aggvals) ->
                        columns.indices.map { colNum ->
                            if (colNum in gby)
                                keyRow[colNum]
                            else
                                aggvals.map { it[colNum] }
                        }
                    }
                }
            }
        }
    }

    @InternalCoroutinesApi
    suspend fun group(vararg by: Int) = sortedMapOf<Int, MutableSet<Int>>().let { collate ->
        this.get(*by).collectIndexed { index, list ->
            val hashCode = list.hashCode()
            val v = collate[hashCode]
            if (v != null) v.add(index) else collate[hashCode] = sortedSetOf(index)
        }
        GroupColumnar(this, collate.size, collate.values.map { it.toIntArray() }.toTypedArray(), by)
    }
}

//todo: csv analogs.  for the same file we would have recordlen as Array<Int>, and our columns
// would have to parse and escape tokens for each cell value.  the group/pivot logic would
// be affected by column structure changes.

