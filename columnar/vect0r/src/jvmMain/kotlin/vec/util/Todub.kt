package vec.util

import vec.macros.Pai2
import vec.macros.t2
import java.lang.ref.SoftReference
import java.util.*
import kotlin.jvm.JvmName

/** really really wants to produce a Double
 */
@JvmName("todubd")
fun todub(f: Any?, d: Double) = ((f as? Double ?: f as? Number)?.toDouble() ?: "$f".let {
    cheapDubCache.getOrPut(it) { SoftReference(it t2 it.toDoubleOrNull()) }
}.get()?.second)?.takeIf { it.isFinite() } ?: d

val cheapDubCache = WeakHashMap<String, SoftReference<Pai2<String, Double?>>>(0)

@JvmName("todub")
fun todubneg(f: Any?) = todub(f, -1e300)

@JvmName("todub0")
fun todub(f: Any?) = todub(f, .0)
