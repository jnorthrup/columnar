package columnar


import arrow.core.some
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.nio.ByteBuffer
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.reflect.KClass
import kotlin.text.Charsets.UTF_8

val KeyRow.f get() = second.first

val Pair<Int, Int>.size: Int get() = let { (a, b) -> b - a }

val xInsertString = { a: ByteBuffer, b: String? ->
    a.put(b?.toByteArray(UTF_8)); when {
    a.hasRemaining() -> a.put(ByteArray(a.remaining()) { ' '.toByte() })
    else -> a
}
}

val binInsertionMapper = mapOf(

    Any::class to (null to { b, a: Any? -> xInsertString(b, a.toString()) }),
    Int::class to (4 to { a: ByteBuffer, b: Int? -> a.putInt(b ?: 0) }),
    Long::class to (8 to { a: ByteBuffer, b: Long? -> a.putLong(b ?: 0) }),
    Float::class to (4 to { a: ByteBuffer, b: Float? -> a.putFloat(b ?: 0f) }),
    Double::class to (8 to { a: ByteBuffer, b: Double? -> a.putDouble(b ?: 0.0) }),
    String::class to (null to xInsertString),
    Instant::class to (8 to { a: ByteBuffer, b: Instant? -> a.putLong((b ?: Instant.EPOCH).toEpochMilli()) }),
    LocalDate::class to (8 to { a: ByteBuffer, b: LocalDate? -> a.putLong((b ?: LocalDate.EPOCH).toEpochDay()) }),
    ByteArray::class to (null to { a: ByteBuffer, b: ByteArray? -> a.put(b) }),
    ByteBuffer::class to (null to { a: ByteBuffer, b: ByteBuffer? -> a.put(b) })
)

operator fun Table1.get(vararg reorder: Int): Table1 = {
    this(it).let { arrayOfFlows ->
        Array(reorder.size) { i ->
            flowOf(arrayOfFlows[reorder[i]].first())
        }
    }
}

inline class BinInt  (val default:Int=0)
inline class BinLong  (val default:Long=0L)
inline class BinFloat  (val default:Float=0F)
inline class BinDouble  (val default:Double=0.0)
inline class BinLocalDate   (val default:LocalDate= LocalDate.EPOCH)

fun ByteBufferNormalizer.decode(buf: ByteBuffer) = let { (coords, mapper) ->
    ByteArray(coords.size).also { buf.get(it) }.let(
        mapOf<KClass<out Any>, Function1<*, Any>>(
            BinInt::class to { buf: ByteBuffer -> buf.getInt() },
            BinLong::class to { buf: ByteBuffer -> buf.getLong() },
            BinFloat::class to { buf: ByteBuffer -> buf.getFloat() },
            BinDouble::class to { buf: ByteBuffer -> buf.getDouble() },
            BinLocalDate::class to { buf: ByteBuffer -> buf.getLong().let(LocalDate::ofEpochDay) },
            Int::class to intMapper,
            Long::class to longMapper,
            Float::class to floatMapper,
            Double::class to doubleMapper,
            String::class to stringMapper,
            LocalDate::class to dateMapper
        )[mapper]!! as (ByteArray) -> Any?
    )
}

infix fun RowNormalizer.from(rs: RowStore<Flow<ByteBuffer>>): Table1 = { row ->
    rs.values(row).let { values ->
        val first = values.first()
        first.let { buf ->
            Array(this.size) { index ->
                val (_, convertor) = this[index]
                flowOf(convertor.decode(buf))
            }
        }
    }
}


val stringMapper: (Any?) -> String = { i ->
    (i as? ByteArray)?.let {
        val string = String(it)
        string.takeIf(
            String::isNotBlank
        )?.trim()
    } ?: ""
}

fun btoa(i: Any?) = (i as? ByteArray)?.let {
    val stringMapper1 = stringMapper(it)
    stringMapper1.toString()
}

val intMapper = { i: ByteArray -> btoa(i)?.toInt() ?: 0 }
val floatMapper = { i: ByteArray -> btoa(i)?.toFloat() ?: 0f }
val doubleMapper = { i: ByteArray -> btoa(i)?.toDouble() ?: 0.0 }
val longMapper = { i: ByteArray -> btoa(i)?.toLong() ?: 0L }
val dateMapper = { i: ByteArray ->
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
    } ?: LocalDate.EPOCH
}


/**
 * reassign columns
 */
@ExperimentalCoroutinesApi
@JvmName("getKRVA")
operator fun KeyRow.get(vararg axis: Int): KeyRow = get(axis)

@ExperimentalCoroutinesApi
operator fun KeyRow.get(axis: IntArray): KeyRow = this.let { (cols, data) ->
    cols[axis] to
            data.let { (rows, sz) ->
                rows.map { r -> r[axis] } to sz
            }
}

infix fun RowNormalizer.reify(r: FixedRecordLengthFile): KeyRow = this to (r.map { fb ->
    (fb.first()).let { buf ->
        Array(size) {
            this[it].let { (a, b) ->
                b.decode(buf)
            }
        }
    }
} to r.size)


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
        } else deepTrim(inbound.filterNotNull())
    } else if (inbound is Iterable<*>) deepTrim(inbound.filterNotNull().toTypedArray())
    else inbound


@ExperimentalCoroutinesApi
suspend fun KeyRow.pivot(lhs: IntArray, axis: IntArray, vararg fanOut: Int): KeyRow =
    this.let { (nama, data) ->
        distinct(*axis).let { keys ->
            val xHash = keys.mapIndexed { xIndex, any -> any.contentDeepHashCode() to xIndex }.toMap()
            this.run {

                val (xSize, synthNames) = pivotOutputColumns(fanOut, nama, axis, keys)
                val get = nama.get(lhs)
                val synthNames1 = synthNames
                val synthMasterCopy = combine(get, synthNames1)
                synthMasterCopy to data.let { (rows, sz) ->
                    pivotRemappedValues(
                        rows,
                        lhs,
                        xHash,
                        xSize,
                        axis,
                        fanOut,
                        synthMasterCopy
                    ) to sz
                }
            }
        }
    }

@ExperimentalCoroutinesApi
suspend fun KeyRow.pivot2(lhs: IntArray, axis: IntArray, vararg fanOut: Int): RoutedRows =
    this.let { (nama, data) ->
        distinct(*axis).let { keys ->
            val xHash = keys.mapIndexed { xIndex, any -> any.contentDeepHashCode() to xIndex }.toMap()
            this.run {
                val (xSize, synthNames) = pivotOutputColumns(fanOut, nama, axis, keys)
                val synthMasterCopy = combine(nama.get(lhs), synthNames)
                synthMasterCopy to data.let { (rows, sz) ->
                    pivotRemappedValues(
                        rows,
                        lhs,
                        xHash,
                        xSize,
                        axis,
                        fanOut,
                        synthMasterCopy
                    ).map {
                        it.asSequence()
                    } to sz
                }
            }
        }
    }


suspend fun KeyRow.distinct(vararg axis: Int) =
    get(axis).let { (arrayOfPairs, pair) ->
        pair.let { (flow1, sz) ->
            flow1.toList().map(::arrayOfAnys).distinctBy { it.contentDeepHashCode() }
        }
    }

operator fun Array<Any?>.invoke(c: RowNormalizer) =
    this.also {
        c.forEachIndexed { i, (a, c, b) ->
            b.fold({}) { function: xform ->
                this[i] = function(this[i])
            }
        }
    }


/**
 * cost of one full tablscan
 */
suspend fun KeyRow.group(vararg by: Int): KeyRow = let {
    val (columns, data) = this
    val (rows, d) = data
    val protoValues = (columns.indices - by.toTypedArray()).toIntArray()
    val clusters = mutableMapOf<Int, Pair<Array<Any?>, MutableList<Flow<Array<Any?>>>>>()
    rows.collect { row ->
        val key = arrayOfAnys(row.get(by))
        val keyHash = key.contentDeepHashCode()
        flowOf(row.get(protoValues)).let { f ->
            when {
                clusters.containsKey(keyHash) -> clusters[keyHash]!!.second += (f)
                else -> clusters[keyHash] = key to mutableListOf(f)
            }
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
                            cols[index][ix] = (columns[index].third.fold({ any }, { it(any) }))
                        }
                    }
                assert(cols.size == protoValues.size)
                cols
            }
            for ((index, i) in protoValues.withIndex()) finale[i] = groupedRow[index]

        }
    }.asFlow() to clusters.size)
}

/**
 * cost of one full tablscan
 */
suspend fun RoutedRows.group2(vararg by: Int) = let {
    val (columns, data) = this
    val (rows, d) = data
    val protoValues = (columns.indices - by.toTypedArray()).toIntArray()
    val clusters = mutableMapOf<Int, Pair<Array<Any?>, MutableList<Sequence<RouteHandle>>>>()
    rows.collect { row1 ->

        val row = row1.toList().toTypedArray()
        val key = arrayOfAnys(row[by])
        val keyHash = key.contentDeepHashCode()
        sequenceOf(row[protoValues].asSequence()).let { f ->
            if (clusters.containsKey(keyHash)) clusters[keyHash]!!.second += (f)
            else clusters[keyHash] = key to mutableListOf(f)
        }
    }
    columns to (clusters.map { (_, cluster1) ->
        val (key, cluster) = cluster1
        val chunky = cluster.map { it.iterator() }
        sequence {
            for ((index, column) in columns.withIndex()) {
                if (index in by)
                    yield(key[by.indexOf(index)])
                else
                    yield(chunky.map { it.next() })
            }
        }

    } to clusters.size)
}


infix fun KeyRow.with(that: KeyRow): KeyRow = let { (theseCols, theseData) ->
    theseData.let { (theseRows, theseSize) ->
        that.let { (thatCols, thatData) ->
            thatData.let { (thatRows, thatSize) ->
                assert(thatSize == theseSize) { "rows must be same -- ${theseSize}!=$thatSize" }
                val unionRows = theseRows.zip(thatRows) { a, b -> combine(a, b) }
                val unionCol = combine(theseCols, thatCols)
                val unionData = unionRows to theseSize
                unionCol to unionData
            }
        }
    }
}


@UseExperimental(ExperimentalCoroutinesApi::class)
suspend infix fun KeyRow.resample(indexcol: Int) = this[indexcol].let { (a, b) ->
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

suspend fun show(it: KeyRow) = it.let { (cols, b) ->
    b.let { (rows, sz) ->
        System.err.println(cols.contentDeepToString())
        rows.collect { ar ->
            ar(cols).let {
                println(deepArray(it.contentDeepToString()))
            }
        }
    }
}

private fun pivotRemappedValues(
    rows: Flow<Array<Any?>>,
    lhs: IntArray,
    xHash: Map<Int, Int>,
    xSize: Int,
    axis: IntArray,
    fanOut: IntArray,
    synthMasterCopy: RowNormalizer
) =
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

infix fun <A, B, C> Pair<A, B>.by(third: C) = this.let { (a, b) -> Triple(a, b, third) }


operator fun RowNormalizer.invoke(t: xform): RowNormalizer = Array(size) {
    this[it].let { (a, b, c) ->
        a to b by c.fold({ -> { any: Any? -> any.let(t) } }, { { any: Any? -> any.let(it).let(t) } }).some()
    }
}


operator fun KeyRow.invoke(t: xform): KeyRow = this.let { (a, b) ->
    a.map { (c, e, d) ->
        c to e by (d.fold({ t }, { dprime: xform ->
            { rowval: Any? ->
                t(dprime(rowval))
            }
        })).some()
    }.toTypedArray() to b
}


fun groupSumFloat(res: KeyRow, vararg exclusion: Int): KeyRow {
    val summationColumns = (res.first.indices - exclusion.toList()).toIntArray()
    return res.get(exclusion) with res.get(summationColumns).invoke { it: Any? ->
        when {
            it is Array<*> -> it.map { (it as? Float?) ?: 0f }.sum()
            it is List<*> -> it.map { (it as? Float?) ?: 0f }.sum()
            else -> it
        }
    }
}

private fun pivotOutputColumns(
    fanOut: IntArray,
    nama: RowNormalizer,
    axis: IntArray,
    keys: List<Array<Any?>>
): Pair<Int, RowNormalizer> {
    val xSize = fanOut.size
    val synthNames = nama.get(axis).let { axNam ->
        keys.map { key ->
            axNam.zip(key).let { keyPrefix ->

                fanOut.map { pos ->
                    val (aggregatedName, coord, xForm) = nama[pos]
                    "${keyPrefix.map { (col, imprintValue) ->
                        val (str, second, optXform) = col
                        "$str=${optXform.fold({ imprintValue }, { it(imprintValue) })}"
                    }.joinToString(":")}:${aggregatedName}" to coord by xForm
                }
            }
        }
    }.flatten().toTypedArray()
    return Pair(xSize, synthNames)
}
