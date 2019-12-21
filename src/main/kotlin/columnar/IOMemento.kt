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
infix fun <O, R, F : (O) -> R> O.`•`(f: F) = this.let(f)
infix fun <A, B, R, O : (A) -> B, G : (B) -> R> O.`•`(b: G) = this * b
operator fun <A, B, R, O : (A) -> B> O.times(b: (B) -> R) =     { a: A -> a `•` this `•` (b) }

/**
 * IOMemento is a helper to simplify common bounded width bytebuffer FWF and CSV field usecases.
 *
 * the KClass type of value is part of the implementations but has not been added to the tuples
 */
sealed class IOMemento(

    /** this operation takes a bytebuffer and returns a value leaving the bytebuffer in an undefined state.
     * The bytebuffers are assumed to be defensive slices that contain only one value and the value returned reads the whole buffer into conversion.
     */
    val read: readfn,
    /**
     * this operation uses a bytebuffer and an input value and writes the value into the bytebuffer leaving it in an undefined state.
     */
    val write: writefn
) {
    open class Tokenized(read: readfn, write:writefn) : IOMemento(read, write )
  open  class Fixed(    /**
                     * if this is not null then we can use these values for binary bytebuffer operations
                     */
                    val bytes: Int?,read: readfn,write:writefn)   : IOMemento(read, write )
    object txtInt : Tokenized(
      (bb2ba `•` btoa `•` trim * String::toInt), { a: ByteBuffer, b: Int? -> a.putInt(b ?: 0) } as writefn)

    object txtLong : Tokenized(

        (bb2ba `•` btoa `•` trim * String::toLong),
        { a: ByteBuffer, b: Long? -> a.putLong(b ?: 0) } as writefn)

    object txtFloat : Tokenized(

        (bb2ba `•` btoa `•` trim * String::toFloat),
        { a: ByteBuffer, b: Float? -> a.putFloat(b ?: 0f) } as writefn)

    object txtDouble : Tokenized(

        (bb2ba `•` btoa `•` trim * String::toDouble),
        { a: ByteBuffer, b: Double? -> a.putDouble(b ?: 0.0) } as writefn)

    object txtString : Tokenized( bb2ba * btoa * String::trim, xInsertString as writefn)
    object txtLocalDate : Tokenized(

        bb2ba `•` btoa `•` trim * dateMapper,
        { a: ByteBuffer, b: LocalDate? -> a.putLong((b ?: LocalDate.EPOCH).toEpochDay()) } as writefn)

    object txtInstant : Tokenized(

        bb2ba `•` btoa `•` trim * instantMapper,
        { a: ByteBuffer, b: Instant? -> a.putLong((b ?: Instant.EPOCH).toEpochMilli()) } as writefn)

    object bInt : Fixed(4, ByteBuffer::getInt, { a: ByteBuffer, b: Int? -> a.putInt(b ?: 0);Unit } as writefn)
    object bLong : Fixed(8, ByteBuffer::getLong, { a: ByteBuffer, b: Long? -> a.putLong(b ?: 0);Unit } as writefn)
    object bFloat :
        Fixed(4, ByteBuffer::getFloat, { a: ByteBuffer, b: Float? -> a.putFloat(b ?: 0f);Unit } as writefn)

    object bDouble :
        Fixed(8, ByteBuffer::getDouble, { a: ByteBuffer, b: Double? -> a.putDouble(b ?: 0.0);Unit } as writefn)

    object bString : Tokenized( bb2ba `•` btoa `•` trim, xInsertString)
    object bLocalDate : Fixed(
        8,
        { buf: ByteBuffer -> buf.long `•` LocalDate::ofEpochDay } as readfn,
        { a: ByteBuffer, b: LocalDate? -> a.putLong((b ?: LocalDate.EPOCH).toEpochDay()) } as writefn)

    object bInstant : Fixed(
        8,
        { buf: ByteBuffer -> buf.long `•` Instant::ofEpochMilli },
        { a: ByteBuffer, b: Instant? -> a.putLong((b ?: Instant.EPOCH).toEpochMilli()) } as writefn)
}