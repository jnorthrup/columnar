package vec.macros

import kotlin.math.absoluteValue

@JvmName("combine_Vect0r ")
inline fun <reified T> combine(vargs: Vect0r<Vect0r<T>>) = combine(*vargs.toArray())

@JvmName("combine_VecVa")
inline fun <reified T> combine(vararg vargs: Vect0r<T>): Vect0r<T> {
    vargs.filter { it.size>0 }.toVect0r().let {vargs1->
        return muxIndexes(vargs1).let { (isize, tails) ->
            isize t2 { ix: Int ->
                val (slot, i1) = demuxIndex(tails, ix)
                vargs1[slot][i1]
            }
        }
    }
}

inline fun <reified T> muxIndexes(vargs: Vect0r<Vect0r<T>>) =
    vargs.toArray()/*.filter { it.size>0 }*/.foldIndexed(0 t2 IntArray(vargs.size)) {
            vix: Int, (acc: Int, srcVec: IntArray): Pai2<Int, IntArray>, (vecSize): Vect0r<T> ->
            (acc + (vecSize)).let { nsize ->
                srcVec[vix] = nsize
                nsize t2 srcVec
            }
        }

fun demuxIndex(tails: IntArray, ix: Int)  =
    (1 + tails.binarySearch(ix)).absoluteValue.let { source ->
        Tw1n(source, if (source != 0) (ix % tails[source]) - tails[source - 1]
        else ix % tails[0])
    }