/**
 * what library doesn't have at least one util for all the evils of getting work done outside the elegant showroom code?
 */
@file:Suppress("NOTHING_TO_INLINE")

package vec.util


import vec.macros.Pai2
import java.nio.ByteBuffer
import kotlin.text.Charsets.UTF_8


inline infix fun ByteBuffer.at(start: Int): ByteBuffer = (if (limit() > start) clear() else this).position(start)
inline operator fun ByteBuffer.get(start: Int, end: Int): ByteBuffer = limit(end).position(start)
inline val Pair<Int, Int>.size get() = let { (a: Int, b: Int) -> b - a }
inline val Pai2<Int, Int>.size get() = let { (a: Int, b: Int) -> b - a }

inline fun Int.toArray(): IntArray = intArrayOf(this)
inline fun bb2ba(bb: ByteBuffer) = ByteArray(bb.remaining()).also { bb[it] }
inline fun btoa(ba: ByteArray) = String(ba, UTF_8)
inline fun trim(it: String) = it.trim()
inline fun logDebug(debugTxt: () -> String) {
    try {
        assert(false, debugTxt)
    } catch (a: AssertionError) {
        System.err.println(debugTxt())
    }
}

var logReuseCountdown = 0

/**
 * missing stdlib list operator https://github.com/Kotlin/KEEP/pull/112
 */
object _l {
    inline operator fun <T> get(vararg t: T) = listOf(*t)
}

/**
 * missing stdlib array operator https://github.com/Kotlin/KEEP/pull/112
 */
object _a {
    operator fun get(vararg t: Int) = t
    operator fun get(vararg t: Double) = t
    operator fun get(vararg t: Short) = t
    operator fun get(vararg t: Byte) = t
    operator fun get(vararg t: Boolean) = t
    operator fun get(vararg t: Long) = t
    operator fun <T> get(vararg t: T) = t

}

/**
 * missing stdlib set operator https://github.com/Kotlin/KEEP/pull/112
 */
object _s {
    inline operator fun <T> get(vararg t: T) = setOf(*t)
}


fun main() {
    logDebug { "this ought not be visible" }
}