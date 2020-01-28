package columnar

import kotlin.math.absoluteValue


// c[0,1,2]
// scalars[0,1,2],[3,4,5],[6,7,8]
//
//c[iy].second(7)
fun join(vararg v: Cursor): Cursor = join(vect0rOf(*v))

fun join(c: Vect0r<Cursor>): Cursor = run {

    val vargs = c α Cursor::scalars

    val (xsize, order) = vargs.toSequence().foldIndexed(0 t2 IntArray(vargs.first)) { vix, (acc, avec), vec ->
        val xsize = acc.plus(vec.first)
        xsize t2 avec.also { avec[vix] = xsize }
    }
    Cursor(c[0].first) { iy: Int ->
        RowVec(xsize) { ix: Int ->
            val slot = (1 + order.binarySearch(ix)).absoluteValue
            val cursor: Cursor = c[slot]
            val rowVec: RowVec = cursor.second(iy)

            val adjusted: Int = if (0 == slot) ix % order[slot]
            else {//the usual case
                var p = slot - 1
                val prev = order[p++]
                val lead = order[p]
                ix % lead - prev
            }
            rowVec[adjusted]
        }
    }
}
