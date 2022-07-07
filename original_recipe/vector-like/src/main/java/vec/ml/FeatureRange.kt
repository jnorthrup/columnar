@file:Suppress("NOTHING_TO_INLINE")

package vec.ml

import vec.macros.Pai2
import vec.macros.Tw1n
import vec.macros.t2
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * returns min,max of Iterable given
 */
@JvmName("FRComparable")
inline fun <reified T : Comparable<T>> featureRange(seq: Iterable<T>, maxMinTwin: Tw1n<T>): Tw1n<T>  =
    featureRange_(seq, maxMinTwin)

/**
 * returns min,max of Iterable given
 */
@JvmName("FRComparable_")
inline fun <reified T : Comparable<T>> featureRange_(seq: Iterable<T>, maxMinTwin: Tw1n<T>): Tw1n<T> =
    seq.fold(maxMinTwin) { incumbent, candidate: T ->
        val (incumbentMin, incumbentMax) = incumbent
        val cmin = minOf(incumbentMin, candidate)
        val cmax = maxOf(candidate, incumbentMax)
        when {
            incumbentMax !== candidate && candidate === cmax || incumbentMin !== candidate && candidate === cmin -> Tw1n(
                cmin,
                cmax
            )
            else -> incumbent
        }
    }

/**
 * this is a min-max in that order
 */
inline fun Tw1n<Double>.normalize(d: Double): Double = let { (min, max) -> ((d - min) / (max - min)).toDouble() }
inline fun Tw1n<Float>.normalize(d: Float): Double = let { (min, max) -> ((d - min) / (max - min)).toDouble() }
inline fun Tw1n<Int>.normalize(d: Int): Double = let { (min, max) -> ((d - min) / (max - min)).toDouble() }
inline fun Tw1n<Long>.normalize(d: Long): Double = let { (min, max) -> ((d - min) / (max - min)) .toDouble() }
inline fun Tw1n<Double>.deNormalize(d: Double): Double = let { (min, max) -> (d * (max - min) + min) }
inline fun Tw1n<Float>.deNormalize(d: Double): Float = let { (min, max) -> (d * (max - min) + min) }.toFloat()
inline fun Tw1n<Int>.deNormalize(d: Double): Int = let { (min, max) -> (d * (max - min) + min) }.roundToInt()
inline fun Tw1n<Long>.deNormalize(d: Double): Long = let { (min, max) -> (d * (max - min) + min) }.roundToLong()

@JvmName("FrInt")
fun featureRange(
    seq: Iterable<Int>,
    maxMinTwin: Tw1n<Int> = Int.MAX_VALUE t2 Int.MIN_VALUE,
) = featureRange_(seq, maxMinTwin)


@JvmName("FrLong")
fun featureRange(
    seq: Iterable<Long>,
    maxMinTwin: Tw1n<Long> = Long.MAX_VALUE t2 Long.MIN_VALUE,
) = featureRange_(seq, maxMinTwin)

@JvmName("FrDouble")
fun featureRange(
    seq: Iterable<Double>,
    maxMinTwin: Tw1n<Double> = Double.MAX_VALUE t2 Double.MIN_VALUE,
) = featureRange_(seq, maxMinTwin)

@JvmName("FrFloat")
fun featureRange(
    seq: Iterable<Float>,
    maxMinTwin: Tw1n<Float> = Float.MAX_VALUE t2 Float.MIN_VALUE,
) = featureRange_(seq, maxMinTwin)

