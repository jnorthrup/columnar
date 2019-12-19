package columnar

import java.nio.ByteBuffer
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val btoa ={i:ByteArray-> (i as ByteArray)?.let { val stringMapper1 = stringMapper(it); stringMapper1 }}
val bb2ba ={i:ByteBuffer-> (i as ByteBuffer)?.let {ByteArray(i.remaining()){i.get()} }}

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
val instantMapper = { i: ByteArray ->
 val btoa = btoa(i)
 btoa?.let {
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
typealias writefn = Function2<ByteBuffer, Any?, *>
typealias readfn = Function1<ByteBuffer, Any?>
operator fun< A,B,C > Function1<A,B>.times (r: Function1<B,C>) = { x:A->r(this(x)) }
typealias b2a=Function1<ByteArray,*>
enum class TypeMemento(val bytes: Int?, val write: writefn, val read:readfn) {
 mInt(4, { a: ByteBuffer, b: Int? -> a.putInt(b ?: 0) } as writefn, (bb2ba * btoa * (intMapper as Function1<String, *>)) ),
 mLong(8, { a: ByteBuffer, b: Long? -> a.putLong(b ?: 0) } as writefn, (bb2ba * btoa * (longMapper as Function1<String, *>))),
 mFloat(4, { a: ByteBuffer, b: Float? -> a.putFloat(b ?: 0f) } as writefn, (bb2ba * btoa * (floatMapper as Function1<String, *>))),
 mDouble(8, { a: ByteBuffer, b: Double? -> a.putDouble(b ?: 0.0) } as writefn, (bb2ba * btoa * (doubleMapper as Function1<String, *>))),
 mString(null, xInsertString as writefn, stringMapper),
 mLocalDate(
 8,
 { a: ByteBuffer, b: LocalDate? -> a.putLong((b ?: LocalDate.EPOCH).toEpochDay()) } as writefn,
 (bb2ba * btoa * ( dateMapper
 as Function1<String, *>))),
 mInstant(
 8,
 { a: ByteBuffer, b: Instant? -> a.putLong((b ?: Instant.EPOCH).toEpochMilli()) } as writefn,
 (bb2ba * btoa * ( instantMapper
 as Function1<String, *>))),
 mBinInt(4, { a: ByteBuffer, b: Int? -> a.putInt(b ?: 0) } as writefn, { buf: ByteBuffer -> buf.int }),
 mBinLong(8, { a: ByteBuffer, b: Long? -> a.putLong(b ?: 0) } as writefn, { buf: ByteBuffer -> buf.long }),
 mBinFloat(4, { a: ByteBuffer, b: Float? -> a.putFloat(b ?: 0f) } as writefn, { buf: ByteBuffer -> buf.float }),
 mBinDouble(8, { a: ByteBuffer, b: Double? -> a.putDouble(b ?: 0.0) } as writefn, { buf: ByteBuffer -> buf.double }),
 mBinString(null, xInsertString as writefn, { buf: ByteBuffer -> String(ByteArray(buf.remaining()).let(buf::get)) }),
 mBinLocalDate(8, { a: ByteBuffer, b: LocalDate? -> a.putLong((b ?: LocalDate.EPOCH).toEpochDay())} as writefn, { buf: ByteBuffer -> buf.long.let(LocalDate::ofEpochDay) }as readfn),
 mBinInstant(8, { a: ByteBuffer, b: Instant? -> a.putLong((b ?: Instant.EPOCH).toEpochMilli())}as writefn, { buf: ByteBuffer -> buf.long.let(LocalDate::ofInstant) }as readfn)

}