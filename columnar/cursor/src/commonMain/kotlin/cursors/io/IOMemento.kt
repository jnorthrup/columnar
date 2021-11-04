@file:OptIn(ExperimentalTime::class)

package cursors.io

import cursors.TypeMemento
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import ports.ByteBuffer
import vec.macros.*
import kotlin.time.ExperimentalTime

const val SPACE: Byte = ' '.code.toByte()
const val ZERO: Float = 0.toFloat()
val DT_EPOCH = LocalDate(1970, 1, 1)
val INSTANT_EPOCH = DT_EPOCH.atStartOfDayIn(TimeZone.UTC)

val EMPTY = ByteArray(0)
val xInsertString = { a: ByteBuffer, b: String? ->
    a.put(b?.encodeToByteArray() ?: EMPTY)
    while (a.hasRemaining()) {
        a.put(SPACE)
    }
}
val dateMapper = { s: String ->
    s.let {
        val res = try {
            LocalDate.parse(it)
        } catch (e: RuntimeException) {
            Unit
        } catch (e: Throwable) {
            Unit
        } finally {
        }
        res as? LocalDate
    } ?: DT_EPOCH

}

val instantMapper = { s: String ->
    s.let {
        val res = try {
            Instant.parse(it)
        } catch (e: RuntimeException) {
            Unit
        } catch (e: Throwable) {
            Unit
        } finally {
        }
        res as? Instant
    } ?: INSTANT_EPOCH
}

enum class IOMemento(override val networkSize: Int? = null) : TypeMemento {
    IoBoolean(1),
    IoByte(1),
    IoInt(4),
    IoLong(8),
    IoFloat(4),
    IoDouble(8),
    IoLocalDate(8),
    IoInstant(12),
    IoString,
    IoNothing
    ;

    companion object {
        private var cmpMap: MutableMap<TypeMemento, (Any?, Any?) -> Int> = linkedMapOf(
            IoInt to { o1, o2 -> (o1 as Int).compareTo(o2 as Int) },
            IoLong to { o1, o2 -> (o1 as Long).compareTo(o2 as Long) },
            IoByte to { o1, o2 -> (o1 as Int and 0xff).compareTo(o2 as Int and 0xff) },
            IoFloat to { o1, o2 -> (o1 as Float).compareTo(o2 as Float) },
            IoDouble to { o1, o2 -> (o1 as Double).compareTo(o2 as Double) },
            IoInstant to { o1, o2 -> (o1 as Instant).compareTo(o2 as Instant) },
            IoBoolean to { o1, o2 -> (o1 as Boolean).compareTo(o2 as Boolean) },
            IoLocalDate to { o1, o2 -> (o1 as LocalDate).compareTo(o2 as LocalDate) },
        )
        val stringCmp = { o1: Any?, o2: Any? -> "$o1".compareTo("$o2") }
        var cmpCache: Array<(Any?, Any?) -> Int> =( Array(values().size) { stringCmp }).also {
            cmpMap.forEach { (a,b)->it[(a as IOMemento).ordinal]=b }
        }

    fun cmp(t: TypeMemento) =(t as? IOMemento)?. let { cmpCache[t.ordinal] } ?: stringCmp

    fun listComparator(progression: Vect0r<out TypeMemento>) = Comparator<List<*>> { o1, o2 ->
        val comp = progression.map<TypeMemento, (Any?, Any?) -> Int, Vect0r<out TypeMemento>>(::cmp)
        var res = 0
        var idx = 0
        while (res == 0 && idx < o1.size) {
            val compare = comp[idx]
            res = compare(o1[idx], o2[idx])
            idx++
        }
        res
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