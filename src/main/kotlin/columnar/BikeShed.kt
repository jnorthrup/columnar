/**
 * what library doesn't have at least one util for all the evils of getting work done outside the elegant showroom code?
 */
@file:Suppress("NOTHING_TO_INLINE")

package columnar


import java.nio.ByteBuffer
import kotlin.text.Charsets.UTF_8


inline infix fun ByteBuffer.at(start: Int): ByteBuffer = (if (limit() > start) clear() else this).position(start)
inline operator fun ByteBuffer.get(start: Int, end: Int): ByteBuffer = limit(end).position(start)
inline val Pair<Int, Int>.size get() = let { (a, b) -> b - a }
inline val Pai2<Int, Int>.size get() = let { (a, b) -> b - a }

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
fun main() {
    logDebug { "this ought not be visible" }
}