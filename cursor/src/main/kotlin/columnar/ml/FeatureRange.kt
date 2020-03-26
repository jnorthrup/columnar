package columnar.ml

import columnar.Tw1n
import columnar.component1
import columnar.component2

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