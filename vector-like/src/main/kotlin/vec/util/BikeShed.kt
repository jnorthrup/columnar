/**
 * what library doesn't have at least one util for all the evils of getting work done outside the elegant showroom code?
 */
@file:Suppress("NOTHING_TO_", "UNCHECKED_CAST")

package vec.util

import vec.macros.Pai2
import vec.macros.Vect0r
import vec.macros.toVect0r
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.file.Paths
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin
import kotlin.text.Charsets.UTF_8


infix fun ByteBuffer.at(start: Int): ByteBuffer = (if (limit() > start) clear() else this).position(start)
operator fun ByteBuffer.get(start: Int, end: Int): ByteBuffer = limit(end).position(start)
val Pair<Int, Int>.span get() = let { (a: Int, b: Int) -> b - a }
val Pai2<Int, Int>.span get() = let { (a: Int, b: Int) -> b - a }
infix fun Any?.debug(message: String) = kotlin.io.println(message)
fun Int.toArray(): IntArray = _a[this]
fun bb2ba(bb: ByteBuffer) = ByteArray(bb.remaining()).also { bb[it] }
fun btoa(ba: ByteArray) = String(ba, UTF_8)
fun trim(it: String) = it.trim()
fun logDebug(debugTxt: () -> String) {
    try {
        assert(false, debugTxt)
    } catch (a: AssertionError) {
        System.err.println(debugTxt())
    }
}

val IntProgression.indices
    get() = map { it }

var logReuseCountdown = 0

/**
 * missing stdlib list operator https://github.com/Kotlin/KEEP/pull/112
 */
object _v {
    @JvmStatic
    operator fun <T> get(vararg t: T): Vect0r<T> = (t as Array<T>).toVect0r()
}

/**
 * missing stdlib list operator https://github.com/Kotlin/KEEP/pull/112
 */
object _l {
    @JvmStatic
    operator fun <T> get(vararg t: T) = listOf(*t)
}

/**
 * missing stdlib array operator https://github.com/Kotlin/KEEP/pull/112
 */
object _a {
    @JvmStatic
    operator fun get(vararg t: Int) = t

    @JvmStatic
    operator fun get(vararg t: Double) = t

    @JvmStatic
    operator fun get(vararg t: Short) = t

    @JvmStatic
    operator fun get(vararg t: Byte) = t

    @JvmStatic
    operator fun get(vararg t: Boolean) = t

    @JvmStatic
    operator fun get(vararg t: Long) = t

    @JvmStatic
    operator fun <T> get(vararg t: T) = t as Array<T>
}

/**
 * missing stdlib set operator https://github.com/Kotlin/KEEP/pull/112
 */
object _s {
    @JvmStatic
    operator fun <T> get(vararg t: T) = setOf(*t)
}

/**
 * missing stdlib map convenience operator
 */
object _m {
    @JvmStatic
    operator fun <K, V, P : Pair<K, V>> get(p: List<P>) = (p).toMap()

    @JvmStatic
    operator fun <K, V, P : Pair<K, V>> get(vararg p: P) = mapOf(*p)
}

fun main() {
    logDebug { "this ought not be visible" }
}

val eol = System.getProperty("line.separator")

fun fileSha256Sum(pathname: String): String {
    val command = ProcessBuilder().command("sha256sum", pathname)
    val process = command.start()
    val reader = BufferedReader(InputStreamReader(process.inputStream))
    val builder = StringBuilder()
    var line: String? = null
    while (reader.readLine().also { line = it } != null) {
        builder.append(line)
        builder.append(eol)
    }
    val result = builder.toString()
    return result
}

val String.path get() = Paths.get(this)

infix fun Any?.println(x: Any?) {
    kotlin.io.println("$x")
}

@JvmOverloads
tailrec fun fib(n: Int, a: Int = 0, b: Int = 1): Int =
    when (n) {
        0 -> a
        1 -> b
        else -> fib(n - 1, b, a + b)
    }


/** gradually compressed index accessor to underlying Cursor x values. */
fun horizon(a2: Int, datapoints: Int, viewPoints: Int) = max(a2.toInt(),
    (datapoints.toDouble() - sin(((viewPoints.toDouble() - a2) / viewPoints.toDouble()) * (PI / 2.0)) * datapoints.toDouble()).toInt())
