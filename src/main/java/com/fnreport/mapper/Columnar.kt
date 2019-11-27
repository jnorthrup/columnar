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

val Pair<Int, Int>.size: Int get() = let { (a, b) -> b - a }
typealias ByteBufferNormalizer<T> = Pair<Pair<Int, Int>, (Any?) -> T>
typealias RowDecoder = List<Pair<String, ByteBufferNormalizer<*>>>
typealias Table1 = suspend (Int) -> Array<Flow<Any?>>

operator fun Table1.get(vararg reorder: Int): Table1 = { row ->
    val arrayOfFlows = this(row)
    reorder.map { i ->
        flowOf(arrayOfFlows[i].first())
    }.toTypedArray()
}

fun <T> ByteBufferNormalizer<T>.decodeLazy(buf: Lazy<ByteBuffer>) = let { (coords, mapper) ->
    ByteArray(coords.size).also { buf.value.get(it) }.let(mapper)
}


fun <T> ByteBufferNormalizer<T>.decode(buf: ByteBuffer): T =
        let { (coords, mapper) ->
            ByteArray(coords.size).also { buf.get(it) }.let(mapper)
        }

infix fun RowDecoder.from(rs: RowStore<Flow<ByteBuffer>>): suspend (Int) -> Array<Flow<Any?>> = { row: Int ->
    val cols = this
    val values = rs.values(row)
    val first = lazyOf(values.first())
    first.let { buf ->
        this.mapIndexed { index,
                          (_, convertor) ->
            flowOf(convertor.decodeLazy(buf))
        }.toTypedArray()
    }
}


val stringMapper: (Any?) -> Any? = { i ->
    (i as? ByteArray)?.let {
        val string = String(it)
        string.takeIf(
                String::isNotBlank
        )?.trim()
    }
}

fun btoa(i: Any?) = (i as? ByteArray)?.let {
    val stringMapper1 = stringMapper(it)
    stringMapper1?.toString()
}

val intMapper: (Any?) -> Any? = { i -> btoa(i)?.toInt() ?: 0 }
val floatMapper: (Any?) -> Any? = { i -> btoa(i)?.toFloat() ?: 0f }
val doubleMapper: (Any?) -> Any? = { i -> btoa(i)?.toDouble() ?: 0.0 }
val longMapper: (Any?) -> Any? = { i -> btoa(i)?.toLong() ?: 0L }


val dateMapper: (Any?) -> Any? = { i ->
    val btoa = btoa(i)
    btoa?.let {
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
          (0 until size).forEach{  collector.emit(values(it)) }

/*
        val availableProcessors = Runtime.getRuntime().availableProcessors()
        val chunksize = max(size, availableProcessors) / min(size, availableProcessors)
*/
        /*
        coroutineScope {
            (0 until size).chunked(chunksize).forEach { range ->
                launch {
                    coroutineScope {
                        range.forEach { launch { collector.emit(values(it)) } }
                    }
                }
            }
        }*/
    }
}


interface FixedLength<T> {
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
        override val size: Int = mappedByteBuffer.limit(),
        /**default returns a line seeked EOL buffer.*/
        override var values: suspend (Int) -> Flow<ByteBuffer> = { row ->
            flowOf(mappedByteBuffer.apply { position(row) }.slice().also {
                while (it.hasRemaining() && it.get() != '\n'.toByte());
                (it as ByteBuffer).flip()
            })
        }
) : FileAccess(filename), RowStore<Flow<ByteBuffer>>, Closeable by randomAccessFile


class FixedRecordLengthFile(filename: String, origin: MappedFile = MappedFile(filename)) : FixedRecordLengthBuffer(origin.mappedByteBuffer),
        Closeable by origin

open class FixedRecordLengthBuffer(val buf: ByteBuffer) :
        RowStore<Flow<ByteBuffer>>,
        FixedLength<Flow<ByteBuffer>> {
    override var values: suspend (Int) -> Flow<ByteBuffer> = { row -> flowOf(buf.position(recordLen * row).slice().limit(recordLen)) }
    override val recordLen: Int = buf.duplicate().clear().run {
        while (hasRemaining() && get() != '\n'.toByte());
        position()
    }
    override val size: Int = (buf.limit() / recordLen)//.also { assert(it != 0) { "bad size" } }
}

typealias Table2 = Pair<Array<String>, Pair<Flow<Array<Any?>>, Int>>

/**
 * reassign columns
 */
@ExperimentalCoroutinesApi
operator fun Table2.get(vararg axis: Int): Table2 =
        this.let { (cols, data) ->
            axis.map { ix -> cols[ix] }.toTypedArray() to
                    data.let { (rows, sz) ->
                        rows.take(sz).map { r -> axis.map { c -> r[c] }.toTypedArray() } to sz
                    }
        }

infix fun RowDecoder.reify(r: FixedRecordLengthFile): Table2 =
        this.map { (a) -> a }.toTypedArray() to (r.run {
            take(size)
        }.map { fb ->
            lazyOf(fb.first()).let { lb ->
                map { (_, b) ->
                    b.decodeLazy(lb)
                }
            }.toTypedArray()
        } to r.size)

suspend fun Table2.cluster(vararg keys: Int) = this.get(*keys).let { (_, remapped) ->
    linkedMapOf<Any?, SortedSet<Int>>().apply {
        val mappings = this
        remapped.let { (values, _) ->
            values.collectIndexed { ix, ar1 ->
                val ar = ar1.toList()
                val x = mappings[ar]
                x?.add(ix) ?: mappings.run { set(ar, sortedSetOf(ix)) }
            }
        }
    }.map { (k, v) -> k to v.toTypedArray() }.toMap()
}

@ExperimentalCoroutinesApi
suspend fun Table2.pivot(lhs: IntArray, axis: IntArray, vararg fanOut: Int): Table2 = this.let { (nama, data) ->
    val cluster: Map<Any?, Array<Int>> = this.cluster(*axis)
    val keys = cluster.keys
    val xcoord = keys.mapIndexed { index, any -> any to index }.toMap()
    val xsize = fanOut.size
    this.run {
        pivotColumns(nama, axis, fanOut, keys).flatten().toTypedArray() to data.let { (data, sz) ->
            pivotData(data, lhs, xcoord, xsize, axis, fanOut) to sz
        }
    }
}

fun pivotColumns(legend: Array<String>, lhs: IntArray, rhs: IntArray, keys: Set<Any?>) =
        lhs.mapIndexed { index: Int, i: Int ->
            legend[i]
        }.let { axNam ->
            keys.map { key ->
                axNam.zip((key as List<*>)).let { keyPrefix ->
                    rhs.map { i ->
                        "${keyPrefix.map { (a, b) ->
                            a + "=" + b
                        }.joinToString(":")}:${legend[i]}"
                    }
                }
            }
        }


private fun pivotData(data: Flow<Array<Any?>>, lhs: IntArray, xcoord: Map<Any?, Int>, xsize: Int, axis: IntArray, fanOut: IntArray): Flow<Array<Any?>> {
    return data.map { value ->
        arrayOfNulls<Any?>(+lhs.size + (xcoord.size * xsize)).also { grid ->
            val key = axis.map { i -> value[i] }
            val x = xcoord[key]!!
            lhs.mapIndexed { index, i ->
                grid[index] = value[i]
            }
            fanOut.mapIndexed { index, xcol ->
                grid[lhs.size + (xsize * x + index)] = value[xcol]
            }
        }
    }
}
/*

*/
/**
 * our tight recursive object with flexible column features
 *//*

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

                        val second = cluster.second
                        val lower = second * rhs.size
                        val intRange = lower until (lower + rhs.size)
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
*/

//todo: csv analogs.  for the same file we would have recordlen as Array<Int>, and our columns
// would have to parse and escape tokens for each cell value.  the group/pivot logic would
// be affected by column structure changes.

