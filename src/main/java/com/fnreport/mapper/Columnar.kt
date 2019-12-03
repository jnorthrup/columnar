package com.fnreport.mapper

import arrow.core.Option
import arrow.core.Some
import arrow.core.none
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

inline operator fun <reified T> Array<T>.get(vararg index: Int) = index.map(::get).toTypedArray()
val Pair<Int, Int>.size: Int get() = let { (a, b) -> b - a }

typealias Table1 = suspend (Int) -> Array<Flow<Any?>>

typealias xform = (Any?) -> Any?
typealias ByteBufferNormalizer  = Pair<Pair<Int, Int>, xform>
typealias RowDecoder = Array<Pair<String, ByteBufferNormalizer >>
typealias Column = Pair<String, Option<xform>>
typealias DecodedRows = Pair<Array<Column>, Pair<Flow<Array<Any?>>, Int>>

operator fun Table1.get(vararg reorder: Int): Table1 = { row ->
    val arrayOfFlows = this(row)
    reorder.map { i ->
        flowOf(arrayOfFlows[i].first())
    }.toTypedArray()
}

fun   ByteBufferNormalizer .decodeLazy(buf: Lazy<ByteBuffer>) = let { (coords, mapper) ->
    ByteArray(coords.size).also { buf.value.get(it) }.let(mapper)
}


fun   ByteBufferNormalizer .decode(buf: ByteBuffer)  =
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
    override val size: Int = (buf.limit() / recordLen)//.also { assert(it != 0) { "bad size" } }
}


/**
 * reassign columns
 */
@ExperimentalCoroutinesApi
operator fun DecodedRows.get(vararg axis: Int): DecodedRows =
        this.let { (cols, data) ->
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

suspend fun DecodedRows.cluster(vararg keys: Int) = this.get(*keys).let { (_, remapped) ->
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
suspend fun DecodedRows.pivot(lhs: IntArray, axis: IntArray, vararg fanOut: Int): DecodedRows = this.let { (nama, data) ->
    val cluster: Map<Any?, Array<Int>> = this.cluster(*axis)
    val keys = cluster.keys
    val xcoord = keys.mapIndexed { index, any -> any to index }.toMap()
    val xsize = fanOut.size
    this.run {
        val arrayOfPairs = nama.get(*lhs)
        val pcol = pivotColumns(nama, axis, fanOut, keys).flatten().toTypedArray()
        val finalColumns = arrayOfPairs + pcol
        finalColumns to data.let { (data, sz) ->
            data.map { value ->
                arrayOfNulls<Any?>(+lhs.size + (xcoord.size * xsize)).also { grid ->
                    val key = axis.map { i -> value[i] }
                    val x = xcoord[key]!!
                    lhs.mapIndexed { index, i ->
                        grid[index] = value[i]
                    }
                    fanOut.mapIndexed { index, xcol ->

                        val x = lhs.size + (xsize * x + index)

                        grid[x] = value[xcol]
                    }
                    grid.mapIndexed {index, any ->
                        finalColumns[index].second.fold(  { any },{function -> function(any) })
                    }
                }
            } to sz
        }
    }
}

fun pivotColumns(legend: Array<Column>, axis: IntArray, rhs: IntArray, keys: Set<Any?>) =
        legend.get(*axis).let { axNam ->

            keys.map { key ->
                axNam.zip(key as List<*>).let { keyPrefix ->

                    rhs.map { i ->
                        val (f, g) = legend[i]

                        "${keyPrefix.map { (a, b) ->
                            val (c, t) = a

                            arrayOf(c, t.fold({ b }, { it(b) })).joinToString("=")
                        }.joinToString(":")}:${f}" to g
                    }
                }
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
                    group.collect  { row: Array<Any?> ->
                        assert(row.size == protoValues.size)
                        row.forEachIndexed { index, any -> cols[index].add(columns[index].second.fold({any},{it(any)})) }
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

infix suspend fun DecodedRows.with(that: DecodedRows): DecodedRows = let { (a, b) ->
    b.let { (c, d) ->
        val second1 = that.second.second
        assert(second1 == d) { "rows must be same -- ${d} !== $second1" }
        val toList = c.toList()
        val toList1 = that.second.first.toList()
        val x = toList.mapIndexed { index: Int, v: Array<Any?> ->
            val r = v.toList() + toList1[index].toList()
            r.toTypedArray()
        }.asFlow()
        (a + that.first) to (x to d)
    }
}

