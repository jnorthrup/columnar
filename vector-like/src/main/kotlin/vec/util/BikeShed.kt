/**
 * what library doesn't have at least one util for all the evils of getting work done outside the elegant showroom code?
 */
@file:Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")

package vec.util
import vec.macros.Pai2
import vec.macros.Vect0r
import vec.macros.toVect0r
import java.io.BufferedReader
import java.io.InputStreamReader
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
object _v {
    inline operator fun <reified T> get(vararg t: T): Vect0r<T> = (t as Array<T>).toVect0r()
}

/**
 * missing stdlib list operator https://github.com/Kotlin/KEEP/pull/112
 */
object _l {
    inline operator fun <reified T> get(vararg t: T) = listOf(*t)
}

/**
 * missing stdlib array operator https://github.com/Kotlin/KEEP/pull/112
 */
object _a {
    inline operator fun get(vararg t: Int) = t
    inline operator fun get(vararg t: Double) = t
    inline operator fun get(vararg t: Short) = t
    inline operator fun get(vararg t: Byte) = t
    inline operator fun get(vararg t: Boolean) = t
    inline operator fun get(vararg t: Long) = t
    inline operator fun <reified T> get(vararg t: T) = t as Array<T>


}

/**
 * missing stdlib set operator https://github.com/Kotlin/KEEP/pull/112
 */
object _s {
    inline operator fun <reified T> get(vararg t: T) = setOf(*t)
}


fun main() {
    logDebug { "this ought not be visible" }
}

fun fileSha256Sum(pathname: String): String {
    val command = ProcessBuilder().command("sha256sum", pathname)

    val process = command.start()
    val reader = BufferedReader(InputStreamReader(process.inputStream))
    val builder = StringBuilder()
    var line: String? = null
    while (reader.readLine().also { line = it } != null) {
        builder.append(line)
        builder.append(System.getProperty("line.separator"))
    }
    val result = builder.toString()
    return result
}