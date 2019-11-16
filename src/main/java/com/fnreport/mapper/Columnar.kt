package com.fnreport.mapper

import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import java.io.Closeable
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.time.LocalDate
import java.time.format.DateTimeFormatter


fun byteDecoder(): (Any?) -> Any? = { i ->
    (i as? ByteBuffer)?.let { ByteArray(it.remaining()).also { i.get(it) } }
}

fun stringMapper(): (Any?) -> Any? = { i -> (i as? ByteArray)?.let { String(it).takeIf(String::isNotBlank)?.trim() } }
fun btoa(i: Any?) = (i as? ByteArray)?.let { stringMapper()(it)?.toString() }


fun intMapper(): (Any?) -> Any? = { i -> btoa(i)?.toInt() }
fun floatMapper(): (Any?) -> Any? = { i -> btoa(i)?.toFloat() }
fun doubleMapper(): (Any?) -> Any? = { i -> btoa(i)?.toDouble() }
fun longMapper(): (Any?) -> Any? = { i -> btoa(i)?.toLong() }


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


interface RowStore<T> {
    /**
     * seek to row
     */
    fun values(row: Int): T

    val size: Int
}

interface FlowStore<T> {
    /**
     * seek to row
     */
    suspend fun values(row: Int): T

    val size: Int
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
) : RowStore<ByteBuffer>, FileAccess(
        filename), Closeable by randomAccessFile {
    override fun values(row: Int): ByteBuffer = mappedByteBuffer.apply { position(row) }.slice()
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
class FixedRecordLengthFile(filename: String, origin: MappedFile = MappedFile(filename)) : Closeable by origin,
        FixedRecordLengthBuffer(buf = origin.mappedByteBuffer)

open class FixedRecordLengthBuffer(val buf: ByteBuffer

) : LineBuffer(), RowStore<ByteBuffer>, FixedLength<Flow<ByteBuffer>> {
    override fun get(vararg rows: Int) = rows.map { buf.position(recordLen * it).slice().apply { limit(recordLen) } }.asFlow()
    override fun values(row: Int): ByteBuffer = buf.position(recordLen * row).slice().limit(recordLen)
    override val recordLen: Int = buf.duplicate().clear().run {
        var c = 0.toByte()
        do c = get() while (c != '\n'.toByte())
        position()
    }
    override val size: Int get()= (recordLen / buf.limit())
}

/**
 * our tight recursive object with flexible column features
 */
@InternalCoroutinesApi
open class Columnar(var rs: RowStore<ByteBuffer>, val columns: List<Pair<String, Pair<Pair<Int, Int>, (Any?) -> Any?>>>) : FlowStore<Flow<List<*>>> {

    override val size: Int
        get() = rs.size

    operator fun get(cols: List<Int>): Columnar {
        return Columnar(this.rs, cols.map { columns[it] })
    }

    override suspend fun values(row: Int) =
            rs.values(row).let { rs1 ->
                flowOf(columns.map { (a, mapper) ->
                    val (coor, conv) = mapper
                    val (begin, end) = coor
                    val len = end - begin
                    val fb = rs1.position(begin).slice().limit(len)
                    conv(ByteArray(len).also { fb.get(it) })
                })
            }

    suspend fun pivot(untouched: IntArray, lhs: Int, vararg rhs: Int): Columnar =
            linkedMapOf<Any?, LinkedHashSet<Int>>().let { lhsIndex ->
                (0 until size).map { rowIndex ->
                    val values = values(rowIndex)
                    values.collect { row ->
                        val key = row[lhs]
                        lhsIndex[key] = ((lhsIndex[key] ?: linkedSetOf()) + rowIndex) as LinkedHashSet<Int>
                    }
                }

                untouched.map { columns[it] }.toMutableList().let { pivotColumns ->


                    columns[lhs].let { (keyPrefix) ->
                        lhsIndex.entries.map { (k, v) ->
                            val keyname = "$keyPrefix:${(k as? ByteArray)?.let { it -> String(it) } ?: k}"
                            pivotColumns += rhs.map { rhsCol ->
                                columns[rhsCol].let { (rhsName, decode) ->
                                    val (coords, mapper) = decode
                                    val function = { input: Any? ->
                                        val block: (Pair<Int, Any?>) -> Any? = { (row, value) ->
                                            value.takeIf { row in v }?.let(mapper)
                                        }
                                        (input as? Pair<Int, Any?>)?.let(block)
                                    }
                                    val pair = (keyPrefix + rhsName) to (coords to function)
                                    pair
                                }
                            }

                        }
                        val rs2 = this.rs
                        return object : Columnar(rs2, pivotColumns) {
                            override suspend fun values(row: Int): Flow<List<Any?>> {
                                return rs2.values(row).let { rs1 ->
                                    flowOf(super.columns.map { (a, mapper) ->
                                        val (coor, conv: (Any?) -> Any?) = mapper
                                        val (begin, end) = coor
                                        val len = end - begin
                                        val fb = rs1.position(begin).slice().limit(len)
                                        conv(row to ByteArray(len).also { fb.get(it) })
                                    })
                                }
                            }
                        }
                    }
                }
            }

    suspend fun group(by: List<Int>): Columnar {
        val origin = this
        val linearIndex = arrayListOf<List<Any?>>()
        this[by].run {
            (0 until size).map { rownum ->
                val values = values(rownum)
                values.collect { theList ->
                    linearIndex += theList
                }
            }
        }
        val collate = mutableMapOf<Int, List<Int>>()
        linearIndex.mapIndexed { index, list ->
            val hashCode = list.hashCode()
            collate[hashCode] = (collate[hashCode] ?: emptyList()) + index
        }
        val originClusters = collate.values.toTypedArray()
        return object : Columnar(rs, columns) {
            override val size: Int
                get() = collate.size

            override suspend fun values(row: Int): Flow<List<Any?>> {
                val flowStar = originClusters[row].let { originCLuster ->
                    val listAny = originCLuster.first().let { clusterKey ->
                        columns.indices.map { indice ->
                            if (indice in by) {
                                origin.values(row).collect { keyR -> keyR[indice] }
                            } else {
                                originCLuster.map { originRow ->
                                    origin.values(originRow).collect { keyR -> keyR[indice] }
                                }
                            }
                        }

                    }
                    listAny.asFlow() as Flow<List<Any?>>
                }
                return flowStar
            }
        }
    }
}


open class VariableRecordLengthFile(filename: String, origin: MappedFile = MappedFile(filename)) : Closeable by origin, VariableRecordLengthBuffer(buf = origin.mappedByteBuffer)

open class VariableRecordLengthBuffer(val buf: ByteBuffer, val header: Boolean = false, val eor: Char = '\n', val index: IntArray = buf.duplicate().clear().run {
    val list = mutableListOf<Int>()
    if (!header) list += position()

    var c = 0.toChar()
    while (hasRemaining()) {
        c = get().toChar()
        if (hasRemaining() && c == eor)
            list += position()
    }
    list.toIntArray()

}, override val size: Int = index.size
) : LineBuffer(), RowStore<ByteBuffer> {
    override fun get(vararg rows: Int) =
            rows.map { row: Int ->
                this.values(row)
            }.asFlow()

    override fun values(row: Int): ByteBuffer = buf.position(index[row]).slice().also {
        if (row != index.size - 1) {
            val i = index[row + 1] - index[row] - 1
            it.limit(i)
        }
    }
}


/* tedious for now.   will revisit.

@UseExperimental(InternalCoroutinesApi::class)
class CsvFile(
        fn: String,
        delim: CharArray = charArrayOf('\n', ','),
        val header: Boolean = true,
        val fileBuf: VariableRecordLengthFile = VariableRecordLengthFile(fn)
) : Indexed<Flow<VariableRecordLengthBuffer>>, RowStore<ByteBuffer> by fileBuf {

    lateinit var columns: List<String>

    init {
        runBlocking {
            val codexBuf = fileBuf(0)
            val byteArray = ByteArray(codexBuf.limit())
            codexBuf.duplicate().get(byteArray)
            val string = String(byteArray)
//            val headerRow = VariableRecordLengthBuffer(codexBuf, false, delim[1])
//            val size1 = headerRow.size
//            columns = headerRow[0 until size1].map { b ->
//                String(ByteArray(b.remaining()).also { z -> b.get(z) })
//            }.toList()
            columns=string.split("\\W+")
        }
    }

    override operator fun get(vararg rows: Int) = fileBuf.get(*rows).map { flowOf(VariableRecordLengthBuffer(it, eor = ',')) }
}




*/
