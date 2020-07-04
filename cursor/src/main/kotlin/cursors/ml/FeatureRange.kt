package cursors.ml

import vec.macros.Tw1n

/**
 * returns min,max of sequence given
 */
@JvmName("FRComparable")
inline fun < reified T : Comparable<T>> featureRange(seq: Sequence<T>, maxMinTwin: Tw1n<T>) =
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
 fun < T : Double> Tw1n<T>.normalize(d: T): T = let { (min, max) -> ((d - min) / (max - min)) as T }
 fun < T : Float>  Tw1n<T>.normalize(d: T): T = let { (min, max) -> ((d - min) / (max - min)) as T }
 fun < T : Int>    Tw1n<T>.normalize(d: T): T = let { (min, max) -> ((d - min) / (max - min)) as T }
 fun < T : Long>   Tw1n<T>.normalize(d: T): T = let { (min, max) -> ((d - min) / (max - min)) as T }

 fun < T : Double> Tw1n<T>.deNormalize(d: T): T = let { (min, max) ->( d*(max-min)+min) as T }
 fun < T : Float>  Tw1n<T>.deNormalize(d: T): T = let { (min, max) ->( d*(max-min)+min) as T }
 fun < T : Int>    Tw1n<T>.deNormalize(d: T): T = let { (min, max) ->( d*(max-min)+min) as T }
 fun < T : Long>   Tw1n<T>.deNormalize(d: T): T = let { (min, max) ->( d*(max-min)+min) as T }
