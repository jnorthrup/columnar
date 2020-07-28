package cursors.io

import cursors.TypeMemento
import vec.macros.Vect0r
import vec.macros.α
import java.nio.ByteBuffer
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

const val SPACE: Byte = ' '.toByte()
const val ZERO: Float = 0.toFloat()

val xInsertString = { a: ByteBuffer, b: String? ->
    a.put(b?.toByteArray(Charsets.UTF_8))
    while (a.hasRemaining()) {
        a.put(SPACE)
    }
}
val dateMapper = { s: String ->
    s.let {
        val res: LocalDate?
        res = try {
            LocalDate.parse(it)
        } catch (e: Exception) {
            LocalDate.from(DateTimeFormatter.ISO_DATE.parseBest(it))
        }
        res
    } ?: LocalDate.EPOCH
}
val instantMapper = { s: String ->
    s.let {
        val res: Instant? = try {
            Instant.parse(it)
        } catch (e: Exception) {
            Instant.from(DateTimeFormatter.ISO_DATE_TIME.parseBest(it))
        }
        res
    } ?: Instant.EPOCH
}

enum class IOMemento(override val networkSize: Int? = null) : TypeMemento {
    IoBoolean(1),
    IoInt(4),
    IoLong(8),
    IoFloat(4),
    IoDouble(8),
    IoString,
    IoLocalDate(8),
    IoInstant(8),
    IoNothing
    ;

    companion object {
        var cmpMap: MutableMap<TypeMemento, (Any?, Any?) -> Int> = linkedMapOf(
                IoLocalDate to { o1, o2 -> (o1 as LocalDate).compareTo(o2 as LocalDate) },
                IoInstant to { o1, o2 -> (o1 as Instant).compareTo(o2 as Instant) },
                IoBoolean to { o1, o2 -> (o1 as Boolean).compareTo(o2 as Boolean) },
                IoInt to { o1, o2 -> (o1 as Int).compareTo(o2 as Int) },
                IoLong to { o1, o2 -> (o1 as Long).compareTo(o2 as Long) },
                IoFloat to { o1, o2 -> (o1 as Float).compareTo(o2 as Float) },
                IoDouble to { o1, o2 -> (o1 as Double).compareTo(o2 as Double) }
        )

        fun cmp(t: TypeMemento) = cmpMap[t] ?: { o1: Any?, o2: Any? ->
            o1.toString().compareTo(o2.toString())
        }
    }
}


fun floatFillNa(fill: Float): (Any?) -> Any? {
    lateinit var x: (Any?) -> Any?
    @Suppress("UNCHECKED_CAST") val bound: (Any?) -> Any? = { testMe: Any? ->
        (testMe as? Vect0r<*>)?.α(x) ?: (testMe as? Iterable<*>)?.α(x) ?: (testMe as? Array<*>)?.α(x) ?: when (testMe) {
            Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, null -> fill
            else -> testMe
        }
    }
    x = bound
    return bound
}


val floatSum: (Any?, Any?) -> Any? = { acc: Any?, any2: Any? ->
    val fl = (acc as? Float) ?: ZERO
    val fl1 = (any2 as? Float) ?: ZERO
    fl + fl1
}


val sumReducer: Map<IOMemento, (Any?, Any?) -> Any?> = mapOf(
        IOMemento.IoInt to { acc: Any?, any2: Any? -> ((acc as? Int) ?: 0) + ((any2 as? Int) ?: 0) },
        IOMemento.IoLong to { acc: Any?, any2: Any? ->
            ((acc as? Long) ?: 0.toLong()) + ((any2 as? Long) ?: 0.toLong())
        },
        IOMemento.IoFloat to floatSum,
        IOMemento.IoDouble to { acc: Any?, any2: Any? ->
            ((acc as? Double) ?: 0.toDouble()) + ((any2 as? Double) ?: 0.toDouble())
        },
        IOMemento.IoString to { acc: Any?, any2: Any? ->
            ((acc as? String) ?: 0.toString()) + ((any2 as? String) ?: 0.toString())
        }
)