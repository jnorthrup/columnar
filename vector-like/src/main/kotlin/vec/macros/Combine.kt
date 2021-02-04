package vec.macros

import kotlin.math.absoluteValue

@JvmName("combine_Vect0r ")
inline fun <reified T> combine(vargs: Vect0r<Vect0r<T>>) = combine(*vargs.toArray())

@JvmName("combine_VecVa")
inline fun <reified T> combine(vararg vargs: Vect0r<T>): Vect0r<T> =
        muxIndexes(vargs.size t2 { it: Int -> vargs[it] }).let { (isize, tails) ->
            isize t2 { ix: Int ->
                val (slot, i1) = demuxIndex(tails, ix)
                vargs[slot][i1]
            }
        }

inline fun <reified T> muxIndexes(vargs: Vect0r<Vect0r<T>>) =
        vargs.toArray().foldIndexed(0 t2 IntArray(vargs.size)) { vix: Int, (acc: Int, srcVec: IntArray): Pai2<Int, IntArray>, (vecSize): Vect0r<T> ->
            (acc + (vecSize)).let { nsize ->
                srcVec[vix] = nsize
                nsize t2 srcVec
            }
        }

@Suppress("NOTHING_TO_INLINE")
inline fun demuxIndex(tails: IntArray, ix: Int) =
        (1 + tails.binarySearch(ix)).absoluteValue.let { source ->
            source t2 (if (source != 0) (ix % tails[source]) - tails[source - 1]
            else ix % tails[0])
        }