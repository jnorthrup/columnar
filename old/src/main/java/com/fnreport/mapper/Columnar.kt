package com.fnreport.mapper

import arrow.core.Option
import arrow.core.Some
import arrow.core.none
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.io.Closeable
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.lang.model.type.UnionType

@JvmName("getVA")
inline operator fun <reified T> List<T>.get(vararg index: Int) = get(index)
inline operator fun <reified T> List<T>.get(index: IntArray) = List(index.size) { i: Int -> this[index[i]] }

@JvmName("getVA")
inline operator fun <reified T> Array<T>.get(vararg index: Int) = get(index)
inline operator fun <reified T> Array<T>.get(index: IntArray) = Array(index.size) { i: Int -> this[index[i]] }

inline fun <reified T> union(a: Array<T>, b: Array<T>): Array<T> = Array(a.size + b.size) { i ->
    when (i < a.size) {
        true -> a[i];else -> b[i - a.size]
    }
}

inline val DecodedRows.f
    get() = let { (_,
                          b) ->
        b.let { (c,
                        _) ->
            c
        }
    }

val Pair<Int, Int>.size: Int get() = let { (a, b) -> b - a }

typealias Table1 = suspend (Int) -> Array<Flow<Any?>>
typealias xform = (Any?) -> Any?
typealias ByteBufferNormalizer = Pair<Pair<Int, Int>, xform>
typealias RowDecoder = Array<Pair<String, ByteBufferNormalizer>>

typealias Column = Pair<String, Option<xform>>
typealias RowHandle=UnionType

typealias DecodedRows = Pair<Array<Column>, Pair<Flow<RowHandle>, Int>>

operator fun Table1.get(vararg reorder: Int): Table1 = {
    this(it).let { arrayOfFlows ->
        Array(reorder.size) { i ->
            flowOf(arrayOfFlows[reorder[i]].first())
        }
    }
}

fun ByteBufferNormalizer.decodeLazy(buf: Lazy<ByteBuffer>) = let { (coords, mapper) ->
    ByteArray(coords.size).also { buf.value.get(it) }.let(mapper)
}

fun ByteBufferNormalizer.decode(buf: ByteBuffer) =
        let { (coords, mapper) ->
            ByteArray(coords.size).also { buf.get(it) }.let(mapper)
        }

infix fun RowDecoder.from(rs: RowStore<Flow<ByteBuffer>>): Table1 = { row: Int ->
    val cols = this
    val values = rs.values(row)
    val first = lazyOf(values.first())
    first.let { buf ->
        Array(this.size)
        /*  this.mapIndexed*/ { index ->
            val (_, convertor) = this[index]
            flowOf(convertor.decodeLazy(buf))
        }
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
        for (it in 0 until size) {
            collector.emit(values(it))
        }
    }
}


interface FixedLength {
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
        FixedLength {
    override var values: suspend (Int) -> Flow<ByteBuffer> = { row -> flowOf(buf.position(recordLen * row).slice().limit(recordLen)) }
    override val recordLen = buf.duplicate().clear().run {
        while (hasRemaining() && get() != '\n'.toByte());
        position()
    }
    override val size: Int = (buf.limit() / recordLen)
}


/**
 * reassign columns
 */
@ExperimentalCoroutinesApi
@JvmName("getVA")
operator fun DecodedRows.get(vararg axis: Int): DecodedRows = get(axis)

@ExperimentalCoroutinesApi
operator fun DecodedRows.get(axis: IntArray): DecodedRows = this.let { (cols, data) ->
    cols[axis] to
            data.let { (rows, sz) ->
                rows.map { r -> r[axis] } to sz
            }
}

infix fun RowDecoder.reify(r: FixedRecordLengthFile): DecodedRows =
        Array(this.size) { this[it].let { (name) -> name to none<xform>() } } to (r.map { fb ->
              lazyOf(fb.first()).let { buf->
            Array(size) {
                this[it].let { (a, b) ->
                    b.decodeLazy(buf)
                }
            }
        }} to r.size)

fun arrayOfAnys(it: Array<Any?>): Array<Any?> = deepArray(it) as Array<Any?>

tailrec fun deepArray(inbound: Any?): Any? =
        if (inbound is Array<*>) inbound.also<Any?> { ar ->
            inbound.forEachIndexed { i, v ->
                (inbound as Array<Any?>)[i] = deepArray(inbound[i])
            }
        }
        else if (inbound is Iterable<*>) deepArray(inbound.map { it })
        else inbound

tailrec fun deepTrim(inbound: Any?): Any? =
        if (inbound is Array<*>) {
            if (inbound.all { it != null }) inbound.also {
                it.forEachIndexed { ix, any ->
                    @Suppress("UNCHECKED_CAST")
                    (inbound as Array<Any?>)[ix] = deepTrim(any)
                }
            } else deepTrim(  inbound.filterNotNull ())
        } else if (inbound is Iterable<*>) deepTrim(inbound.filterNotNull().toTypedArray())
        else inbound


@ExperimentalCoroutinesApi
suspend fun DecodedRows.pivot(lhs: IntArray, axis: IntArray, vararg fanOut: Int): DecodedRows = this.let { (nama, data) ->
    distinct(*axis).let { keys ->
        val xHash = keys.mapIndexed { xIndex, any -> any.contentDeepHashCode() to xIndex }.toMap()
        this.run {
            val (xSize, synthNames) = pivotOutputColumns(fanOut, nama, axis, keys)
            val synthMasterCopy = union(nama.get(lhs), synthNames)
            synthMasterCopy to data.let { (rows, sz) ->
                pivotRemappedValues(rows, lhs, xHash, xSize, axis, fanOut, synthMasterCopy) to sz
            }
        }
    }
}

private fun pivotRemappedValues(rows: Flow<Array<Any?>>, lhs: IntArray, xHash: Map<Int, Int>, xSize: Int, axis: IntArray, fanOut: IntArray, synthMasterCopy: Array<Column>) =
        rows.map { row ->
            arrayOfNulls<Any?>(+lhs.size + (xHash.size * xSize)).also { grid ->
                val key = row.get(axis).let(::arrayOfAnys)
                for ((index, i) in lhs.withIndex()) {

                    grid[index] = row[i]
                }
                val x = xHash[key.contentDeepHashCode()]!!

                for ((index, xcol) in fanOut.withIndex()) {
                    val x1 = lhs.size + (xSize * x + index)
                    grid[x1] = row[xcol]
                }
                grid(synthMasterCopy)
            }
        }

private fun pivotOutputColumns(fanOut: IntArray, nama: Array<Column>, axis: IntArray, keys: List<Array<Any?>>): Pair<Int, Array<Pair<String, Option<xform>>>> {
    val xSize = fanOut.size
    val synthNames = nama.get(axis).let { axNam ->
        keys.map { key ->
            axNam.zip(key).let { keyPrefix ->

                for (pos in fanOut) {

                }
                fanOut.map { pos ->
                    val (aggregatedName, xForm) = nama[pos]
                    "${keyPrefix.map { (col, imprintValue) ->
                        val (str, optXform) = col
                        "$str=${optXform.fold({ imprintValue }, { it(imprintValue) })}"
                    }.joinToString(":")}:${aggregatedName}" to xForm
                }
            }
        }
    }.flatten().toTypedArray()
    return Pair(xSize, synthNames)
}


suspend fun DecodedRows.distinct(vararg axis: Int) =
        get(axis).let { (arrayOfPairs, pair) ->
            pair.let { (flow1, sz) ->
                flow1.toList().map(::arrayOfAnys).distinctBy { it.contentDeepHashCode() }
            }
        }

operator fun Array<Any?>.invoke(c: Array<Column>) = this.also { c.forEachIndexed { i, (a, b) -> b.fold({}) { function: xform -> this[i] = function(this[i]) } } }


/**
 * cost of one full tablscan
 */
suspend fun DecodedRows.group(vararg by: Int): DecodedRows = let {
    val (columns, data) = this
    val (rows, d) = data
    val protoValues = (columns.indices - by.toTypedArray()).toIntArray()
    val clusters = mutableMapOf<Int, Pair<Array<Any?>, MutableList<Flow<Array<Any?>>>>>()
    rows.collect { row ->
        val key = arrayOfAnys(row.get(by))
        val keyHash = key.contentDeepHashCode()
        flowOf(row.get(protoValues)).let { f ->
            if (clusters.containsKey(keyHash)) clusters[keyHash]!!.second += (f)
            else clusters[keyHash] = key to mutableListOf(f)
        }
    }
    columns to (clusters.map { (_, cluster1) ->
        val (key, cluster) = cluster1
        assert(key.size == by.size)
        arrayOfNulls<Any?>(columns.size).also { finale ->

            by.forEachIndexed { index, i ->
                finale[i] = key[index]
            }
            val groupedRow = protoValues.map { arrayOfNulls<Any?>(cluster.size) }.let { cols ->
                for ((ix, group) in cluster.withIndex())
                    group.collectIndexed { index, row ->
                        assert(row.size == protoValues.size)
                        for ((index, any) in row.withIndex()) {
                            cols[index][ix] = (columns[index].second.fold({ any }, { it(any) }))
                        }
                    }
                assert(cols.size == protoValues.size)
                cols
            }
            for ((index, i) in protoValues.withIndex()) finale[i] = groupedRow[index]

        }
    }.asFlow() to clusters.size)
}


operator fun RowDecoder.invoke(t: xform): RowDecoder = map { (a, b) ->
    val (c, d) = b
    a to (c to { any: Any? -> t(d(any)) })
}.toTypedArray()


operator fun DecodedRows.invoke(t: xform): DecodedRows = this.let { (a, b) ->
    a.map { (c, d) ->
        c to Some(d.fold({ t }, { dprime: xform ->
            { rowval: Any? ->
                t(dprime(rowval))
            }
        })) as Option<xform>
    }.toTypedArray() to b
}


infix fun DecodedRows.with(that: DecodedRows): DecodedRows = let { (theseCols, theseData) ->
    theseData.let { (theseRows, theseSize) ->
        that.let { (thatCols, thatData) ->
            thatData.let { (thatRows, thatSize) ->
                assert(thatSize == theseSize) { "rows must be same -- ${theseSize}!=$thatSize" }
                val unionRows = theseRows.zip(thatRows) { a, b -> union(a, b) }
                val unionCol = union(theseCols, thatCols)
                val unionData = unionRows to theseSize
                unionCol to unionData
            }
        }
    }
}


suspend fun show(it: DecodedRows) = it.let { (cols, b) ->
    b.let { (rows, sz) ->
        System.err.println(cols.contentDeepToString())
        rows.collect { ar ->
            ar(cols).let {
                println(deepArray(it.contentDeepToString()))
            }
        }
    }
}

fun groupSumFloat(res: DecodedRows, vararg exclusion: Int): DecodedRows {
    val summationColumns = (res.first.indices - exclusion.toList()).toIntArray()
    return res.get(exclusion) with res.get(summationColumns).invoke { it: Any? ->
        when {
            it is Array<*> -> it.map { (it as? Float?) ?: 0f }.sum()
            it is List<*> -> it.map { (it as? Float?) ?: 0f }.sum()
            else -> it
        }
    }
}

@UseExperimental(ExperimentalCoroutinesApi::class)
suspend infix fun DecodedRows.resample(indexcol: Int) = this[indexcol].let { (a, b) ->
    val (c, d) = b
    val indexValues = c.toList().mapNotNull {
        (it.first() as? LocalDate?)
    }
    val min = indexValues.min()!!
    val max = indexValues.max()!!
    var size = 0
    val empties = (daySeq(min, max) - indexValues).mapIndexed { index, localDate ->
        size = index
        arrayOfNulls<Any?>(first.size).also { row ->
            row[indexcol] = localDate
        }
    }.asFlow()

    let {
        val (a, b) = this
        val (c, d) = b

        a to (flowOf(c, empties).flattenConcat() to d + size)
    }
}


fun daySeq(min: LocalDate, max: LocalDate): Sequence<LocalDate> {
    var cursor = min
    return sequence {
        while (max > cursor) {
            yield(cursor)
            cursor = cursor.plusDays(1)
        }
    }
}
