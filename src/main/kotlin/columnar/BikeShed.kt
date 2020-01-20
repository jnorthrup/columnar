/**
 * what library doesn't have at least one util for all the evils of getting work done outside the elegant showroom code?
 */
package columnar


import java.nio.ByteBuffer


infix fun ByteBuffer.at(start: Int): ByteBuffer = (if (limit() > start) clear() else this).position(start)
operator fun ByteBuffer.get(start: Int, end: Int): ByteBuffer = limit(end).position(start)
val Pair<Int, Int>.size: Int get() = let { (a, b) -> b - a }

fun Int.toArray(): IntArray = intArrayOf(this)
val bb2ba: (ByteBuffer) -> ByteArray = { bb: ByteBuffer -> ByteArray(bb.remaining()).also { bb[it] } }
val btoa: (ByteArray) -> String = { ba: ByteArray -> String(ba, Charsets.UTF_8) }
val trim: (String) -> String = String::trim
fun logDebug(debugTxt: () -> String) {
    try {
        assert(false) {}
    } catch (a: AssertionError) {
        System.err.println(debugTxt())
    }
}

var logReuseCountdown = 0
fun main() {
    logDebug { "this ought not be visible" }
}