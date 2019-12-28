/**
 * what library doesn't have at least one util for all the evils of getting work done outside the elegant showroom code?
 */
package columnar


import columnar.context.CellDriver
import java.nio.ByteBuffer


val Pair<Int, Int>.size: Int get() = let { (a, b) -> b - a }


typealias Matrix<T> = Pair<
        /**shape*/
        IntArray,
        /**accessor*/
            (IntArray) -> T>

infix fun <A, B, C> Pair<A, B>.by(t: C) = Triple(first, second, t)

infix fun ByteBuffer.at(start: Int): ByteBuffer = (if (limit() > start) clear() else this).position(start)
operator fun ByteBuffer.get(start: Int, end: Int): ByteBuffer = limit(end).position(start)

val bb2ba: (ByteBuffer) -> ByteArray = { bb: ByteBuffer -> ByteArray(bb.remaining()).also { bb[it] } }
val btoa: (ByteArray) -> String = { ba: ByteArray -> String(ba, Charsets.UTF_8) }
val trim: (String) -> String = String::trim
infix fun <O, R, F : (O) -> R> O.`•`(f: F) = this.let(f)
operator fun <A, B, R, O : (A) -> B, G : (B) -> R> O.times(b: G) = { a: A -> a `•` this `•` (b) }
infix fun <A, B, R, O : (A) -> B, G : (B) -> R> O.`•`(b: G) = this * b
operator fun<T> Matrix<T> .get(vararg c:Int): T =second(c)
