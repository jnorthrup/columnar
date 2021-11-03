/**
 * what library doesn't have at least one util for all the evils of getting work done outside the elegant showroom code?
 */
@file:Suppress("NOTHING_TO_",
    "UNCHECKED_CAST",
    "ClassName",
    "HasPlatformType",
    "NOTHING_TO_INLINE",
    "UnclearPrecedenceOfBinaryExpression")

package vec.util

import ports.ByteBuffer
import vec.macros.*
import kotlin.jvm.JvmOverloads
import kotlin.math.min

val Pair<Int, Int>.span get() = let { (a: Int, b: Int) -> b - a }
val Pai2<Int, Int>.span get() = let { (a: Int, b: Int) -> b - a }
fun Int.toArray(): IntArray = _a[this]
fun btoa(ba: ByteArray) = ba.decodeToString()
fun trim(it: String) = it.trim()
infix fun ByteBuffer.at(start: Int): ByteBuffer =
    apply { (if (limit() > start)clear() else this).position(start) }

operator fun ByteBuffer.get(start: Int, end: Int): ByteBuffer = apply { this.at(start).limit(end) }
fun bb2ba(bb: ByteBuffer) = ByteArray(bb.remaining()).also {
    bb[it]
}

//infix fun Any?.debug(message: String) = kotlin.io.println(message)


val IntProgression.indices
    get() = map { it }

var logReuseCountdown = 0

/**
 * missing stdlib list operator https://github.com/Kotlin/KEEP/pull/112
 */
object _v {
    operator fun <T> get(vararg t: T): Vect0r<T> = t.size t2 t::get
}

/**
 * missing stdlib list operator https://github.com/Kotlin/KEEP/pull/112
 */
object _l {
    operator fun <T> get(vararg t: T) = listOf(*t)
}

/**
 * missing stdlib array operator https://github.com/Kotlin/KEEP/pull/112
 */
object _a {
    operator fun get(vararg t: Boolean) = t
    operator fun get(vararg t: Byte) = t
    operator fun get(vararg t: Char) = t
    operator fun get(vararg t: Short) = t
    operator fun get(vararg t: Int) = t
    operator fun get(vararg t: Long) = t
    operator fun get(vararg t: Float) = t
    operator fun get(vararg t: Double) = t
    @OptIn(ExperimentalUnsignedTypes::class) operator fun get(vararg t: UInt) = t
    @OptIn(ExperimentalUnsignedTypes::class) operator fun get(vararg t: UByte) = t
    @OptIn(ExperimentalUnsignedTypes::class) operator fun get(vararg t: ULong) = t
    @OptIn(ExperimentalUnsignedTypes::class) operator fun get(vararg t: UShort) = t
    operator fun <T> get(vararg t: T) = t as Array<T>
}

/**
 * missing stdlib set operator https://github.com/Kotlin/KEEP/pull/112
 */
object _s {
    operator fun <T> get(vararg t: T) = setOf(*t)
}

/**
 * missing stdlib map convenience operator
 */
object _m {
    operator fun <K, V, P : Pair<K, V>> get(p: List<P>) = (p).toMap()
    operator fun <K, V, P : Pair<K, V>> get(p: Vect0r<Pai2<K, V>>) = p.`➤`.associate(Pai2<K, V>::pair)
    operator fun <K, V, P : Pair<K, V>> get(vararg p: P) = mapOf(*p)
}

/**
 * ternary --- (B) % t ?: f
 */
infix operator fun <T> Boolean.rem(block: () -> T) = block.takeIf { this }?.invoke()

fun main() {
    logDebug { "this ought not be visible" }
}

infix fun Any?.println(x: Any?) {
    kotlin.io.println("$x")
}

@JvmOverloads
tailrec fun fib(n: Int, a: Int = 0, b: Int = 1): Int = when (n) {
    0 -> a
    1 -> b
    else -> fib(n - 1, b, a + b)
}

@Deprecated("causes cursors to miss left,right,combine cues")
// fun < T, P : Pai2<T, T>, R : Vect0r<T>> Vect0r(p: P) = _v[p.first, p.second] as R

//the values repeat until the lower limit is reached providing cheap dummy row context.
@JvmOverloads
fun <T> moireValues(
    inVec: Vect0r<T>,
    limit: Int,
    initialOneOrMore: Int = inVec.first,
    x: (Int) -> T = inVec.second,
) = min(initialOneOrMore, limit).let { min ->
    combine(min t2 x, (limit - min) t2 { i: Int ->
        x(i.rem(min))
    } //dummy loop rows
    )
}

