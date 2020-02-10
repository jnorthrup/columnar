package columnar

import kotlin.math.absoluteValue

@JvmName("combine_VecVec")
fun <T> combine(vargs:  Vect0r<Vect0r<T>>): Vect0r<T> = combine( *vargs.toArray() )

@JvmName("combine_Vect0r")
fun <T> combine(vararg vargs: Vect0r<T>): Vect0r<T> {
    val (size, order) = vargs.asIterable().foldIndexed(0 t2 IntArray(vargs.size)) { vix, (acc, avec), vec ->
        val size = acc.plus(vec.first)
        avec[vix] = size
        size t2 avec
    }

    return Vect0r(size) { ix: Int ->
        val slot = (1 + order.binarySearch(ix)).absoluteValue
        vargs[slot].second(
            if (0 == slot)
                ix % order[slot]
            else {//the usual case
                var p = slot - 1
                val prev = order[p++]
                val lead = order[p]
                ix % lead - prev
            }
        )
    }
}
