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

enum class IOMemento{
    IoInt,
    IoLong,
    IoFloat,
    IoDouble,
    IoString,
    IoLocalDate,
    IoInstant
}