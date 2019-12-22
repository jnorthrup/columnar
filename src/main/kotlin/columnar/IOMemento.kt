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

val bb2ba: (ByteBuffer) -> ByteArray = { bb: ByteBuffer -> ByteArray(bb.remaining()).also { bb[it] } }
val btoa: (ByteArray) -> String = { ba: ByteArray -> String(ba, Charsets.UTF_8) }
val trim: (String) -> String = String::trim
infix fun <O, R, F : (O) -> R> O.`•`(f: F) = this.let(f)
infix fun <A, B, R, O : (A) -> B, G : (B) -> R> O.`•`(b: G) = this * b
operator fun <A, B, R, O : (A) -> B> O.times(b: (B) -> R) = { a: A -> a `•` this `•` (b) }

enum class IOMemento{
    txtInt,
    txtLong,
    txtFloat,
    txtDouble,
    txtString,
    txtLocalDate,
    txtInstant,
    bInt,
    bLong,
    bFloat,
    bDouble,
    bString,
    bLocalDate,
    bInstant;
}