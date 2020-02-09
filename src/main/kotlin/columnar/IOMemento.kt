package columnar

import java.nio.ByteBuffer
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val xInsertString = { a: ByteBuffer, b: String? ->
    a.put(b?.toByteArray(Charsets.UTF_8))
    while (a.hasRemaining()) a.put(' '.toByte())
}
val dateMapper = { s: String ->
    s.let {
        var res: LocalDate?
        res = try {
            LocalDate.parse(it)
        } catch (e: Exception) {
            val parseBest = DateTimeFormatter.ISO_DATE.parseBest(it)
            LocalDate.from(parseBest)
        }
        res
    } ?: LocalDate.EPOCH
}
val instantMapper = { s: String ->
    s.let {
        var res: Instant?
        res = try {
            Instant.parse(it)
        } catch (e: Exception) {
            val parseBest = DateTimeFormatter.ISO_DATE_TIME.parseBest(it)
            Instant.from(parseBest)
        }
        res
    } ?: Instant.EPOCH
}

enum class IOMemento(override val networkSize: Int? = null) : TypeMemento {
    IoInt(4),
    IoLong(8),
    IoFloat(4),
    IoDouble(8),
    IoString,
    IoLocalDate(8),
    IoInstant(8),
    IoNothing
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
    val fl = (acc as? Float) ?: 0.toFloat()
    val fl1 = (any2 as? Float) ?: 0.toFloat()
    fl + fl1
}


val sumReducer: Map<IOMemento, (Any?, Any?) -> Any?> = mapOf(
    IOMemento.IoInt to { acc: Any?, any2: Any? -> ((acc as? Int) ?: 0) + ((any2 as? Int) ?: 0) },
    IOMemento.IoLong to { acc: Any?, any2: Any? -> ((acc as? Long) ?: 0.toLong()) + ((any2 as? Long) ?: 0.toLong()) },
    IOMemento.IoFloat to floatSum,
    IOMemento.IoDouble to { acc: Any?, any2: Any? ->
        ((acc as? Double) ?: 0.toDouble()) + ((any2 as? Double) ?: 0.toDouble())
    },
    IOMemento.IoString to { acc: Any?, any2: Any? ->
        ((acc as? String) ?: 0.toString()) + ((any2 as? String) ?: 0.toString())
    }
)