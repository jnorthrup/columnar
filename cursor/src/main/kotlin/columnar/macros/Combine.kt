package columnar.macros

import kotlin.math.absoluteValue

@JvmName("combine_VecVec")
inline fun <reified T> combine(vargs: Vect0r<Vect0r<T>>): Vect0r<T> = combine(*vargs.toArray())

@JvmName("combine_Vect0r")


/**
 * Vect0r combine
 *
 * synthesize a Vect0r to enable linear access to Vector(s) stacked end to end.
 *
 *
 *
 * Synth Vector creates an array of "tails" from original sizes. Binary search finds the index without spilling over.
 */
inline fun <reified T> combine(vararg srcVecs: Vect0r<T>): Vect0r<T> {

    /**
     * fold operation - converts individual 0-based sizes to interval markers on one dimension
     */
    val (size, tails) = srcVecs.asIterable().foldIndexed(0 t2 IntArray(srcVecs.size)) { vix, (acc, srcVec), vec ->
        val size = acc.plus(vec.size)
        srcVec[vix] = size
        size t2 srcVec
    }

    /**
     * synthesized vec to find the nearest tail without going over the desired index. this indicates the slot of the
     * varargs above.
     */
    val targetVec = Vect0r(size) { ix: Int ->
        val slot = (1 + tails.binarySearch(ix)).absoluteValue
        val i1 = when (slot) {
            0 -> ix % tails[0]
            else -> {//the usual case
                var p = slot - 1
                val prev = tails[p++]
                val lead = tails[p]
                val i = ix % lead
                i - prev
            }
        }
        srcVecs[slot][i1]
    }
    return targetVec
}
