@file:Suppress("NOTHING_TO_INLINE")

package vec.macros

import kotlin.math.absoluteValue

@JvmName("combine_Vect0r ")
fun <T> combine(vargs: Vect0r<Vect0r<T>>) = combine(*vargs.toArray())

@JvmName("combine_VecVa")
inline fun <T> combine(vararg vecArgs: Vect0r<T>) = vecArgs.filterNot { it.size == 0 }.let { vex ->
    when (vex.size) {
        0 -> vecArgs[0]
        1 -> vex.first()
        else -> vex.let { rows ->
            muxIndexes(rows).let { (isize, tails) ->
                isize t2 { ix: Int ->
                    val (slot, i1) = demuxIndex(ix, tails)
                    rows[slot][i1]
                }
            }
        }
    }
}


inline fun <T> muxIndexes(vargs: List<Vect0r<T>>): Pai2<Int, IntArray> {
    var acc = 0
    val a = IntArray(vargs.size) { it: Int ->
        acc += vargs[it].size
        acc
    }
    return acc t2 a
}

inline fun demuxIndex(ix: Int, tails: IntArray) =
    (1 + tails.binarySearch(ix)).absoluteValue.let { source ->
        Tw1n(
            source, if (source != 0) (ix % tails[source]) - tails[source - 1]
            else ix % tails[0]
        )
    }
