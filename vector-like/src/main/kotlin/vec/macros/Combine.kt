package vec.macros

import kotlin.math.absoluteValue

@JvmName("combine_Vect0r ")
inline fun <reified T> combine(vargs: Vect0r<Vect0r<T>>) = combine(*vargs.toArray())

@JvmName("combine_VecVa")
inline fun <reified T> combine(vararg vecArgs: Vect0r<T>) = vecArgs.filterNot { it.size == 0 }.let { vex ->
    when (vex.size) {
        0 -> vecArgs[0]
        1 -> vex.first()
        else -> vex.let { rows ->
            muxIndexes(rows).let { (isize, tails) ->
                isize t2 { ix: Int ->
                    val (slot, i1) = demuxIndex(tails, ix)
                    rows[slot][i1]
                }
            }
        }
    }
}

inline fun <reified T> muxIndexes(vargs: Collection<Vect0r<T>>) =
    vargs.foldIndexed(0 t2 IntArray(vargs.size)) { vix, (acc: Int, srcVec: IntArray), (vecSize) ->
        (acc + vecSize).let { nsize ->
            srcVec[vix] = nsize
            nsize t2 srcVec
        }
    }

fun demuxIndex(tails: IntArray, ix: Int) =
    (1 + tails.binarySearch(ix)).absoluteValue.let { source ->
        Tw1n(
            source, if (source != 0) (ix % tails[source]) - tails[source - 1]
            else ix % tails[0]
        )
    }
