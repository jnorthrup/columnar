@file:Suppress("NOTHING_TO_INLINE")

package cursors.ml

import vec.macros.Tw1n
import vec.macros.component1
import vec.macros.component2

/**
 * returns min,max of sequence given
 */
@JvmName("FRComparable")
inline fun <reified T : Comparable<T>> featureRange(seq: Sequence<T>, maxMinTwin: Tw1n<T>) =
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
inline fun   Tw1n< Double>.normalize(d:  Double):  Double = let { (min, max) -> ((d - min) / (max - min)) as  Double }
inline fun   Tw1n< Float>.normalize(d:  Float):  Float = let { (min, max) -> ((d - min) / (max - min)) as  Float }
inline fun   Tw1n< Int>.normalize(d:  Int):  Int = let { (min, max) -> ((d - min) / (max - min)) as  Int }
inline fun   Tw1n< Long>.normalize(d:  Long):  Long = let { (min, max) -> ((d - min) / (max - min)) as  Long }
inline fun   Tw1n< Double>.deNormalize(d:  Double):  Double = let { (min, max) -> (d * (max - min) + min) as  Double }
inline fun   Tw1n< Float>.deNormalize(d:  Float):  Float = let { (min, max) -> (d * (max - min) + min) as  Float }
inline fun   Tw1n< Int>.deNormalize(d:  Int):  Int = let { (min, max) -> (d * (max - min) + min) as  Int }
inline fun   Tw1n< Long>.deNormalize(d:  Long):  Long = let { (min, max) -> (d * (max - min) + min) as  Long }
