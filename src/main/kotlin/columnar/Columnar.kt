package columnar


import arrow.core.some
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import java.nio.ByteBuffer
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.text.Charsets.UTF_8

val KeyRow.f get() = second.first

val Pair<Int, Int>.size: Int get() = let { (a, b) -> b - a }

val xInsertString = { a: ByteBuffer, b: String? ->
    a.put(b?.toByteArray(UTF_8))
    while (a.hasRemaining()) a.put(' '.toByte())
    a

}

inline class BinInt(val default: Int = Int.MIN_VALUE)
inline class BinLong(val default: Long = Long.MIN_VALUE)
inline class BinFloat(val default: Float = Float.NaN)
inline class BinDouble(val default: Double = Double.NaN)
inline class BinLocalDate(val default: LocalDate = LocalDate.MIN)
inline class BinString(val default: String = "<default>")
inline class BinInstant(val default: Instant = Instant.MIN)
inline class BinByteArray(val default: ByteArray = def) {
    companion object {
        val def by lazy { byteArrayOf() }
    }
}

inline class BinByteBuffer(val default: ByteBuffer = def) {
    companion object {
        val def by lazy { ByteBuffer.allocate(0) }
    }
}

inline class BinAny(val default: Any = def) {
    companion object {
        val def by lazy { object : Any() {} }
    }
}
typealias RowBinEncoder = Pair<RowNormalizer, Array<Pair<Int?,   Function2<ByteBuffer,*,ByteBuffer>>>>

@Serializable
data class RowBinMeta(val name: String, val coord: Array<Int>, val typ: TypeMemento) {
    companion object {
        fun RowBinMetaList(t: RowBinEncoder) = t.let { (a, _) ->
            //fake ctor
            fun RowBinMeta(t: Triple<String, Array<Int>, TypeMemento>) = t.let { (name, coord, memento) ->
                val typ = memento.let {
                    it.takeIf { it.name.startsWith("mBin") }
                        ?: TypeMemento.valueOf(it.name.replace(Regex("^m"), "mBin"))
                }
                RowBinMeta(name, coord, typ)
            }

            val coords1 = t.coords
            a.mapIndexed { ix, (name, normalizer) ->
                normalizer.let { (_, kClass) ->
                    coords1[ix].let { (start, end) ->
                        RowBinMeta(name to arrayOf(start, end) by TypeMemento[kClass])
                    }
                }
            }
        }
    }
}

enum class TypeMemento(val t: KClass<*>) {
    mAny(Any::class),
    mInt(Int::class),
    mLong(Long::class),
    mFloat(Float::class),
    mDouble(Double::class),
    mString(String::class),
    mInstant(Instant::class),
    mLocalDate(LocalDate::class),
    mByteArray(ByteArray::class),
    mByteBuffer(ByteBuffer::class),
    mBinAny(BinAny::class),
    mBinInt(BinInt::class),
    mBinLong(BinLong::class),
    mBinFloat(BinFloat::class),
    mBinDouble(BinDouble::class),
    mBinString(BinString::class),
    mBinInstant(BinInstant::class),
    mBinLocalDate(BinLocalDate::class),
    mBinByteArray(BinByteArray::class),
    mBinByteBuffer(BinByteBuffer::class);

    companion object {
        private val reverse by lazy { values().associateBy(TypeMemento::t) }
        operator fun get(t: KClass<*>): TypeMemento = reverse[t]!!
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
    ByteBuffer::class to (null to { a: ByteBuffer, b: ByteBuffer? -> a.put(b) }),
    BinAny::class to (null to { b, a: Any? -> xInsertString(b, a.toString()) }),
    BinInt::class to (4 to { a: ByteBuffer, b: Int? -> a.putInt(b ?: 0) }),
    BinLong::class to (8 to { a: ByteBuffer, b: Long? -> a.putLong(b ?: 0) }),
    BinFloat::class to (4 to { a: ByteBuffer, b: Float? -> a.putFloat(b ?: 0f) }),
    BinDouble::class to (8 to { a: ByteBuffer, b: Double? -> a.putDouble(b ?: 0.0) }),
    BinString::class to (null to xInsertString),
    BinInstant::class to (8 to { a: ByteBuffer, b: Instant? -> a.putLong((b ?: Instant.EPOCH).toEpochMilli()) }),
    BinLocalDate::class to (8 to { a: ByteBuffer, b: LocalDate? -> a.putLong((b ?: LocalDate.EPOCH).toEpochDay()) }),
    BinByteArray::class to (null to { a: ByteBuffer, b: ByteArray? -> a.put(b) }),
    BinByteBuffer::class to (null to { a: ByteBuffer, b: ByteBuffer? -> a.put(b) })
)

operator fun Table1.get(vararg reorder: Int): Table1 = {
    this(it).let { arrayOfFlows ->
        Array(reorder.size) { i ->
            flowOf(arrayOfFlows[reorder[i]].first())
        }
    }
}

fun ByteBufferNormalizer.decode(buf: ByteBuffer) = let { (coords, mapper) ->
    ByteArray(coords.size).also { buf.get(it) }.let(
        mapOf<KClass<out Any>, Function1<*, Any>>(
            BinInt::class to { buf: ByteBuffer -> buf.int },
            BinLong::class to { buf: ByteBuffer -> buf.long },
            BinFloat::class to { buf: ByteBuffer -> buf.float },
            BinDouble::class to { buf: ByteBuffer -> buf.double },
            BinLocalDate::class to { buf: ByteBuffer -> buf.long.let(LocalDate::ofEpochDay) },
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


infix fun RowNormalizer.reify(r: FixedRecordLengthFile): KeyRow = this to (r.map { fb ->
    (fb.first()).let { buf ->
        Array(size) {
            this[it].let { (_, b) ->
                b.decode(buf)
            }
        }
    }
} to r.size)


fun arrayOfAnys(it: Array<Any?>): Array<Any?> = deepArray(it) as Array<Any?>

tailrec fun deepArray(inbound: Any?): Any? =
    if (inbound is Array<*>) inbound.also {
        it.forEachIndexed { i, v ->
            (it as Array<Any?>)[i] = deepArray(v)
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


operator fun Array<Any?>.invoke(c: RowNormalizer) =
    this.also {
        c.forEachIndexed { i, (_, _, b) ->
            b.fold({}) { function: xform ->
                this[i] = function(this[i])
            }
        }
    }


/**
 * cost of one full tablscan
 */
suspend fun RoutedRows.group2(vararg by: Int) = let {
    val (columns, data) = this
    val (rows, _) = data
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
            for (index in columns.indices) {
                if (index in by)
                    yield(key[by.indexOf(index)])
                else
                    yield(chunky.map { it.next() })
            }
        }

    } to clusters.size)
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
    b.let { (rows) ->
        System.err.println(cols.contentDeepToString())
        rows.collect { ar ->
            ar(cols).let {
                println(deepArray(it.contentDeepToString()))
            }
        }
    }
}

fun pivotRemappedValues(
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

fun pivotOutputColumns(
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
                        val (str, _, optXform) = col
                        "$str=${optXform.fold({ imprintValue }, { it(imprintValue) })}"
                    }.joinToString(":")}:${aggregatedName}" to coord by xForm
                }
            }
        }
    }.flatten().toTypedArray()
    return Pair(xSize, synthNames)
}


val RowBinEncoder.recordLen
    get() = coords.last().second

val RowBinEncoder.coords
    get() = this.let { (rowNormTriple, binWriter) ->
        var acc = 0
        Array(binWriter.size) { ix ->
            binWriter[ix].let { (hint) ->
                val size = hint ?: rowNormTriple[ix].let { (_, b) -> b.let { (a) -> a.size } }
                acc to (acc + size).also { acc = it }
            }
        }


    }
