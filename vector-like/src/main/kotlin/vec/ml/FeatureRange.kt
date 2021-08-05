@file:Suppress("NOTHING_TO_INLINE")

package vec.ml

import vec.macros.Tw1n
import vec.macros.component1
import vec.macros.component2

/**
 * returns min,max of Iterable given
 */
@JvmName("FRComparable")
inline fun <reified T : Comparable<T>> featureRange(seq: Iterable<T>, maxMinTwin: Tw1n<T>) =
    seq.fold(maxMinTwin) { incumbent, candidate: T ->
        val (incumbentMin, incumbentMax) = incumbent
        val cmin = minOf(incumbentMin, candidate)
        val cmax = maxOf(candidate, incumbentMax)
        when {
            incumbentMax !== candidate && candidate === cmax ||
                    incumbentMin !== candidate && candidate === cmin -> Tw1n(cmin, cmax)
            else -> incumbent
        }
    }

/**
 * this is a min-max in that order
 */
inline fun Tw1n<Double>.normalize(d: Double): Double = let { (min, max) -> ((d - min) / (max - min)) }
inline fun Tw1n<Float>.normalize(d: Float): Float = let { (min, max) -> ((d - min) / (max - min)) }
inline fun Tw1n<Int>.normalize(d: Int): Int = let { (min, max) -> ((d - min) / (max - min)) }
inline fun Tw1n<Long>.normalize(d: Long): Long = let { (min, max) -> ((d - min) / (max - min)) }
inline fun Tw1n<Double>.deNormalize(d: Double): Double = let { (min, max) -> (d * (max - min) + min) }
inline fun Tw1n<Float>.deNormalize(d: Float): Float = let { (min, max) -> (d * (max - min) + min) }
inline fun Tw1n<Int>.deNormalize(d: Int): Int = let { (min, max) -> (d * (max - min) + min) }
inline fun Tw1n<Long>.deNormalize(d: Long): Long = let { (min, max) -> (d * (max - min) + min) }

@JvmName("FrInt")
fun featureRange(
    seq: Iterable<Int>,
    maxMinTwin: Tw1n<Int> = Tw1n(
        Int.MAX_VALUE,
        Int.MIN_VALUE
    ),
): Tw1n<Int> = featureRange(seq, maxMinTwin)


@JvmName("FrLong")
fun featureRange(
    seq: Iterable<Long>,
    maxMinTwin: Tw1n<Long> = Tw1n(
        Long.MAX_VALUE,
        Long.MIN_VALUE
    ),
): Tw1n<Long> = featureRange(seq, maxMinTwin)

@JvmName("FrDouble")
fun featureRange(
    seq: Iterable<Double>,
    maxMinTwin: Tw1n<Double> = Tw1n(
        Double.MAX_VALUE,
        Double.MIN_VALUE
    ),
): Tw1n<Double> = featureRange(seq, maxMinTwin)

@JvmName("FrFloat")
fun featureRange(
    seq: Iterable<Float>,
    maxMinTwin: Tw1n<Float> = Tw1n(
        Float.MAX_VALUE,
        Float.MIN_VALUE
    ),
): Tw1n<Float> = featureRange(seq, maxMinTwin)

