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

inline operator fun <reified T> Array<T>.get(vararg index: Int) = index.map(::get).toTypedArray()
val Pair<Int, Int>.size: Int get() = let { (a, b) -> b - a }

typealias Table1 = suspend (Int) -> Array<Flow<Any?>>

typealias xform = (Any?) -> Any?
typealias ByteBufferNormalizer = Pair<Pair<Int, Int>, xform>
typealias RowDecoder = Array<Pair<String, ByteBufferNormalizer>>
typealias Column = Pair<String, Option<xform>>

operator fun Table1.get(vararg reorder: Int): Table1 = { row ->
    val arrayOfFlows = this(row)
    reorder.map { i ->
        flowOf(arrayOfFlows[i].first())
    }.toTypedArray()
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
        (0 until size).forEach { collector.emit(values(it)) }
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
    override val size: Int = (buf.limit() / recordLen)
}


/**
 * reassign columns
 */
@ExperimentalCoroutinesApi
operator fun DecodedRows.get(vararg axis: Int): DecodedRows = this.let { (cols, data) ->
    axis.map { ix -> cols[ix] }.toTypedArray() to
            data.let { (rows, sz) ->
                rows.take(sz).map { r -> axis.map { c -> r[c] }.toTypedArray() } to sz
            }
}

infix fun RowDecoder.reify(r: FixedRecordLengthFile): DecodedRows {
    val map = map { (a, b) ->
        val x: Option<xform> = none<xform>()
        a to x
    }
    val map1 = r.run {
        take(size)
    }.map { fb ->
        lazyOf(fb.first()).let { lb ->
            map { (a, b) ->
                b.decodeLazy(lb)
            }
        }.toTypedArray()
    }
    return (map.toTypedArray()) to (map1 to r.size)
}

fun arrayOfAnys(it: Array<Any?>): Array<Any?> = deepArray(it) as Array<Any?>

tailrec fun deepArray(it: Any?): Any? =
        if (it is Array<*>) it.map(::deepArray).toTypedArray()
        else if (it is Iterable<*>) deepArray(it.map { it }.toTypedArray())
        else it

tailrec fun deepTrim(it: Any?): Any? =
        if (it is Array<*>) it.map(::deepTrim).filterNotNull().toTypedArray()
        else if (it is Iterable<*>) it.map { it }.toTypedArray().let(::deepTrim)
        else it


@ExperimentalCoroutinesApi
suspend fun DecodedRows.pivot(lhs: IntArray, axis: IntArray, vararg fanOut: Int): DecodedRows = this.let { (nama, data) ->
    get(*axis).let { (arrayOfPairs, pair) ->
        pair.let { (flow1, sz) ->
            flow1.toList().map(::arrayOfAnys).distinctBy { it.contentDeepHashCode() }
        }
    }.let { keys ->
        //        val xCoord = keys.mapIndexed { xIndex, any -> any to xIndex }.toMap()
        val xHash = keys.mapIndexed { xIndex, any -> any.contentDeepHashCode() to xIndex }.toMap()
        this.run {
            val xSize = fanOut.size
            val synthNames = nama.get(*axis).let { axNam ->
                keys.map { key ->
                    axNam.zip(key).let { keyPrefix ->
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
            val synthMaster = arrayOf(*nama.get(*lhs), * synthNames)

            val rerow = data.let { (data, sz) ->

                data.map { value ->
                    arrayOfNulls<Any?>(+lhs.size + (xHash.size * xSize)).also { grid ->
                        val key = value.get(*axis).let(::arrayOfAnys)
                        val x = xHash[key.contentDeepHashCode()]!!
                        lhs.mapIndexed { index, i ->
                            grid[index] = value[i]
                        }
                        fanOut.mapIndexed { index, xcol ->
                            val x = lhs.size + (xSize * x + index)
                            grid[x] = value[xcol]
                        }
                        grid.mapIndexed { index, any ->

                            synthMaster[index].second.fold({ any }, { function -> function(any) })
                        }
                    }
                } to sz
            }
            synthMaster to rerow
        }
    }
}

suspend fun DecodedRows.distinct(vararg axis: Int) =
        get(*axis).let { (arrayOfPairs, pair) ->
            pair.let { (flow1, sz) ->
                flow1.toList().map(::arrayOfAnys).distinctBy { it.contentDeepHashCode() }
            }
        }


/**
 * cost of one full tablscan
 */
suspend fun DecodedRows.group(vararg by: Int): DecodedRows = let {
    val (columns, data) = this
    val (rows, d) = data
    val protoValues = (columns.indices - by.toTypedArray()).toIntArray()
    val clusters = mutableMapOf<Int, Pair<Array<*>, MutableList<Flow<Array<Any?>>>>>()
    rows.collect { row ->
        val key = row.get(*by)
        val keyHash = key.contentDeepHashCode()
        flowOf(row.get(*protoValues)).let { f ->
            if (clusters.containsKey(keyHash)) clusters[keyHash]!!.second += (f)
            else clusters[keyHash] = key to mutableListOf(f)
        }
    }
    columns to (clusters.map { (k, cluster1) ->
        val (key, cluster) = cluster1
        assert(key.size == by.size)
        arrayOfNulls<Any?>(columns.size).also { finale ->
            by.forEachIndexed { index, i ->
                finale[i] = key[index]
            }
            val groupedRow = protoValues.map { arrayListOf<Any?>() }.let { cols ->

                cluster.forEach { group ->
                    group.collect { row: Array<Any?> ->
                        assert(row.size == protoValues.size)
                        row.forEachIndexed { index, any -> cols[index].add(columns[index].second.fold({ any }, { it(any) })) }
                    }
                }
                assert(cols.size == protoValues.size)
                cols.map {
                    assert(it.size == cluster.size)
                    it.toTypedArray()
                }
            }
            protoValues.forEachIndexed { index, i ->
                finale[i] = groupedRow[index]
            }

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


typealias DecodedRows = Pair<Array<Column>, Pair<Flow<Array<Any?>>, Int>>


infix fun DecodedRows.with(that: DecodedRows): DecodedRows = let { (theseCols, theseData) ->
    theseData.let { (theseRows, theseSize) ->
        that.let { (thatCols, thatData) ->
            thatData.let { (thatRows, thatSize) ->
                assert(thatSize == theseSize) { "rows must be same -- ${theseSize}!=$thatSize" }
                val unionRows =
                        theseRows.zip(thatRows) { a, b -> arrayOf(*a, *b) }
                val unoinCol = arrayOf(*theseCols, *thatCols)
                val unionData = unionRows to theseSize
                unoinCol to unionData
            }
        }
    }
}


suspend fun show(it: DecodedRows) = it.let { (cols, b) ->
    b.let { (rows, sz) ->
        System.err.println(cols.contentDeepToString())
        rows.collect { ar ->
            ar.mapIndexed { index, any ->
                assert(cols.size == ar.size)
                val (f, g) = cols[index]
                val fold = g.fold({ any }, { it(any) })
                fold
            }.let {
                println(deepArray(it.toTypedArray().contentDeepToString()))
            }
        }
    }
}

suspend fun groupSumFloat(res: DecodedRows, vararg exclusion: Int): DecodedRows {
    val summationColumns = (res.first.indices - exclusion.toList()).toIntArray()
    val pair3: DecodedRows = res.get(*exclusion) with res.get(*summationColumns).invoke { it: Any? ->
        when {
            it is Array<*> -> it.map { (it as? Float?) ?: 0f }.sum()
            it is List<*> -> it.map { (it as? Float?) ?: 0f }.sum()
            else -> it
        }
    }
    return pair3
}

suspend infix fun DecodedRows.resample(indexcol: Int) = this[indexcol].let { (a, b) ->
    val (c, d) = b
    val indexValues = c.toList().map {
        (it.first() as? LocalDate?)
    }.filterNotNull()
    val min = indexValues.min()!!
    val max = indexValues.max()!!
    var size: Int = 0
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