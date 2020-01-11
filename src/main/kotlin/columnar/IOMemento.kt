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
        try {
            res = LocalDate.parse(it)
        } catch (e: Exception) {
            val parseBest = DateTimeFormatter.ISO_DATE.parseBest(it)
            res = LocalDate.from(parseBest)
        }
        res
    } ?: LocalDate.EPOCH
}
val instantMapper = { s: String ->
    s.let {
        var res: Instant?
        try {
            res = Instant.parse(it)
        } catch (e: Exception) {
            val parseBest = DateTimeFormatter.ISO_DATE_TIME.parseBest(it)
            res = Instant.from(parseBest)
        }
        res
    } ?: Instant.EPOCH
}

enum class IOMemento {
    IoInt,
    IoLong,
    IoFloat,
    IoDouble,
    IoString,
    IoLocalDate,
    IoInstant,
    IoNothing
}

val sumReducer: Map<IOMemento, (Any?,Any?) -> Any?> = mapOf(
    IOMemento.IoInt to { acc: Any?, any2: Any? ->   ((acc as? Int) ?: 0) + ((any2 as? Int) ?: 0) }  ,
    IOMemento.IoLong to { acc: Any?, any2: Any? ->  ((acc as? Long) ?: 0.toLong()) + ((any2 as? Long) ?: 0.toLong()) }  ,
    IOMemento.IoFloat to { acc: Any?, any2: Any? -> ((acc as? Float) ?: 0.toFloat()) + ((any2 as? Float) ?: 0.toFloat()) }  ,
    IOMemento.IoDouble to { acc: Any?, any2: Any? -> ((acc as? Double) ?: 0.toDouble()) + ((any2 as? Double) ?: 0.toDouble()) }  ,
    IOMemento.IoString to { acc: Any?, any2: Any? -> ((acc as? String) ?: 0.toString()) + ((any2 as? String) ?: 0.toString())  }
)