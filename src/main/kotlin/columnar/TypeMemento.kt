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
typealias writefn = Function2<ByteBuffer, *, Unit>
typealias readfn = Function1<ByteBuffer, *>

val bb2ba: (ByteBuffer) -> ByteArray = { bb: ByteBuffer -> ByteArray(bb.remaining()).also { bb[it] } }
val btoa: (ByteArray) -> String = { ba: ByteArray -> String(ba, Charsets.UTF_8) }
val trim: (String) -> String = String::trim
infix fun <O, R,F: Function1<O, R>> O.`•`(f:F): R = this.let(f)
infix fun <A, B, R, O : Function1<A, B>,G:Function1<B, R>> O.`•`(b: G): (A) -> R = this*b
operator fun <A, B, R, G:Function1<B, R>,O : Function1<A, B>> O.times(b: Function1<B, R>): (A) -> R = { a: A -> a `•` this `•` (b) }
enum class TypeMemento(val bytes: Int?, val read: readfn, val write: writefn) {
    txtInt(4, (bb2ba `•` btoa `•` trim * String::toInt), { a: ByteBuffer, b: Int? -> a.putInt(b ?: 0) } as writefn),
    txtLong(8, (bb2ba * btoa * trim * String::toLong), { a: ByteBuffer, b: Long? -> a.putLong(b ?: 0) } as writefn),
    txtFloat(
        4,
        (bb2ba * btoa * trim * String::toFloat),
        { a: ByteBuffer, b: Float? -> a.putFloat(b ?: 0f) } as writefn),
    txtDouble(
        8,
        (bb2ba * btoa * trim * String::toDouble),
        { a: ByteBuffer, b: Double? -> a.putDouble(b ?: 0.0) } as writefn),
    txtString(null, bb2ba * btoa * String::trim, xInsertString as writefn),
    txtLocalDate(
        8,
        bb2ba * btoa * trim * dateMapper,
        { a: ByteBuffer, b: LocalDate? -> a.putLong((b ?: LocalDate.EPOCH).toEpochDay()) } as writefn),
    txtInstant(
        8,
        bb2ba * btoa * trim * instantMapper,
        { a: ByteBuffer, b: Instant? -> a.putLong((b ?: Instant.EPOCH).toEpochMilli()) } as writefn),
    bInt(4, ByteBuffer::getInt, { a: ByteBuffer, b: Int? -> a.putInt(b ?: 0);Unit } as writefn),
    bLong(8, ByteBuffer::getLong, { a: ByteBuffer, b: Long? -> a.putLong(b ?: 0);Unit } as writefn),
    bFloat(4, ByteBuffer::getFloat, { a: ByteBuffer, b: Float? -> a.putFloat(b ?: 0f);Unit } as writefn),
    bDouble(8, ByteBuffer::getDouble, { a: ByteBuffer, b: Double? -> a.putDouble(b ?: 0.0);Unit } as writefn),
    bString(null, bb2ba * btoa * trim, xInsertString),
    bLocalDate(
        8,
        { buf: ByteBuffer -> buf.long.let(LocalDate::ofEpochDay) } as readfn,
        { a: ByteBuffer, b: LocalDate? -> a.putLong((b ?: LocalDate.EPOCH).toEpochDay()) } as writefn),
    bInstant(
        8,
        { buf: ByteBuffer -> buf.long.let(Instant::ofEpochMilli) },
        { a: ByteBuffer, b: Instant? -> a.putLong((b ?: Instant.EPOCH).toEpochMilli()) } as writefn);

}