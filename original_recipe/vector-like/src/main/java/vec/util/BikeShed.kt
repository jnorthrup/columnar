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

import vec.macros.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.ref.SoftReference
import java.nio.ByteBuffer
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import kotlin.math.min
import kotlin.text.Charsets.UTF_8

val Pair<Int, Int>.span: Int get() = let { (a: Int, b: Int) -> b - a }
val Pai2<Int, Int>.span: Int get() = let { (a: Int, b: Int) -> b - a }
fun Int.toArray(): IntArray = _a[this]
fun btoa(ba: ByteArray): String = String(ba, UTF_8)
fun trim(it: String): String = it.trim()
infix fun ByteBuffer.at(start: Int): ByteBuffer = apply { (if (limit() > start) clear() else this).position(start) }
operator fun ByteBuffer.get(start: Int, end: Int): ByteBuffer = apply { this.at(start).limit(end) }
fun bb2ba(bb: ByteBuffer): ByteArray = ByteArray(bb.remaining()).also { bb[it] }

//infix fun Any?.debug(message: String) = kotlin.io.println(message)


val IntProgression.indices: List<Int>
    get() = map { it }

var logReuseCountdown: Int = 0

/**
 * missing stdlib list operator https://github.com/Kotlin/KEEP/pull/112
 */
object _v {
    inline operator fun <reified T> get(vararg t: T): Vect0r<T> = t.size t2 t::get
}

/**
 * missing stdlib list operator https://github.com/Kotlin/KEEP/pull/112
 */
object _l {
    inline operator fun <T> get(vararg t: T): List<T> = if (t.size == 1) Collections.singletonList(t[0]) else listOf(*t)
}

/**
 * missing stdlib array operator https://github.com/Kotlin/KEEP/pull/112
 */
object _a {
    inline operator fun get(vararg t: Boolean): BooleanArray = t
    inline operator fun get(vararg t: Byte): ByteArray = t
    inline operator fun get(vararg t: UByte): UByteArray = t
    inline operator fun get(vararg t: Char): CharArray = t
    inline operator fun get(vararg t: Short): ShortArray = t
    inline operator fun get(vararg t: UShort): UShortArray = t
    inline operator fun get(vararg t: Int): IntArray = t
    inline operator fun get(vararg t: UInt): UIntArray = t
    inline operator fun get(vararg t: Long): LongArray = t
    inline operator fun get(vararg t: ULong): ULongArray = t
    inline operator fun get(vararg t: Float): FloatArray = t
    inline operator fun get(vararg t: Double): DoubleArray = t
    inline operator fun <T> get(vararg t: T): Array<T> = t as Array<T>
}

/**
 * missing stdlib set operator https://github.com/Kotlin/KEEP/pull/112
 */
object _s {
    inline operator fun <T> get(vararg t: T): Set<T> = if (t.size == 1) Collections.singleton(t[0]) else setOf(*t)
}

/**
 * missing stdlib map convenience operator
 */
object _m {
    inline operator fun <K, V, P : Pair<K, V>> get(p: List<P>): Map<K, V> = (p).toMap()
    inline operator fun <K, V, P : Pair<K, V>> get(p: Vect0r<Pai2<K, V>>): Map<K, V> = p.`➤`.associate(Pai2<K, V>::pair)
    inline operator fun <K, V, P : Pair<K, V>> get(vararg p: P): Map<K, V> = mapOf(*p)
}

fun logDebug(debugTxt: () -> String) {
    if (`debug status matching -ea jvm switch`) System.err.println(debugTxt())
}

@Suppress("ObjectPropertyName")
val `debug status matching -ea jvm switch`: Boolean = try {
    assert(false)
    false
} catch (e: AssertionError) {
    true
}
//@[JvmSynthetic JvmField]
//val `debug status matching -ea jvm switch` = Pai2::class.java.desiredAssertionStatus()

inline fun <T> T.debug(block: (T) -> Unit): T = also { lmbda ->
    if (`debug status matching -ea jvm switch`) block(lmbda)
}

/**
 * ternary --- (B) % t ?: f
 */
inline infix operator fun <reified T> Boolean.rem(block: () -> T): T? = block.takeIf { this }?.invoke()

fun main() {
    logDebug { "this ought not be visible" }
}

val eol: String = System.getProperty("line.separator")

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

val String.path: Path get() = Paths.get(this)

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
//inline fun <reified T, P : Pai2<T, T>, R : Vect0r<T>> Vect0r(p: P) = _v[p.first, p.second] as R

//the values repeat until the lower limit is reached providing cheap dummy row context.
@JvmOverloads
inline fun <reified T> moireValues(
        inVec: Vect0r<T>,
        limit: Int,
        initialOneOrMore: Int = inVec.first,
        noinline x: (Int) -> T = inVec.second,
): Pai2<Int, (Int) -> T> = min(initialOneOrMore, limit).let { min ->
    combine(min t2 x, (limit - min) t2 { i: Int ->
        x(i.rem(min))
    } //dummy loop rows
    )
}

@JvmName("todub")
inline fun todubneg(f: Any?): Double = todub(f, -1e300)

@JvmName("todub0")
inline fun todub(f: Any?): Double = todub(f, .0)

@JvmName("tof0")
inline fun tofneg(f: Any?): Float = tof(f, (-1e300).toFloat())

@JvmName("tof1")
inline fun tof(f: Any?): Float = tof(f, .0f)


val cheapDubCache: WeakHashMap<String, SoftReference<Pai2<String, Double?>>> = WeakHashMap<String, SoftReference<Pai2<String, Double?>>>(0)

val cheapFCache: WeakHashMap<String, SoftReference<Pai2<String, Float?>>> = WeakHashMap<String, SoftReference<Pai2<String, Float?>>>(0)

/** really really wants to produce a Double
 */
@JvmName("todubd")
inline fun todub(f: Any?, d: Double): Double = ((f as? Double ?: f as? Number)?.toDouble() ?: "$f".let {
    cheapDubCache.getOrPut(it) { SoftReference(it t2 it.toDoubleOrNull()) }
}.get()?.second)?.takeIf { it.isFinite() } ?: d


@JvmName("todubf")
inline fun tof(f: Any?, d: Float): Float = ((f as? Float ?: f as? Number)?.toFloat() ?: "$f".let {
    cheapFCache.getOrPut(it) { SoftReference(it t2 it.toFloatOrNull()) }
}.get()?.second)?.takeIf { it.toDouble().isFinite() } ?: d


