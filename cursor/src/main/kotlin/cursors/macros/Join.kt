package cursors.macros

import cursors.Cursor
import cursors.at
import cursors.io.RowVec
import cursors.io.scalars
import vec.macros.*
import kotlin.math.absoluteValue

// c[0,1,2]
// scalars[0,1,2],[3,4,5],[6,7,8]
//
//c[iy] at (7)

fun join(vararg v: Cursor): Cursor = join(vect0rOf(*v as Array<Cursor>))

fun join(c: Vect0r<Cursor>): Cursor {

    val vargs = c α Cursor::scalars

    val (xsize, order) = vargs.toSequence().foldIndexed(0 t2 IntArray(vargs.size)) { vix, (acc, avec), vec ->
        val xsize = acc + vec.size
        avec[vix] = xsize
        xsize t2 avec
    }
    val distortRowIndex = { iy: Int ->
        val distortColumnIndex = { ix: Int ->
            val slot = (1 + order.binarySearch(ix)).absoluteValue
            val cursor: Cursor = c[slot]
            val rowVec: RowVec = cursor at (iy)

            val adjusted: Int = if (0 == slot)
                ix % order[slot]
            else {//the usual case
                var p = slot - 1
                val prev = order[p++]
                val lead = order[p]
                ix % lead - prev
            }
            rowVec[adjusted]
        }
        xsize t2 distortColumnIndex
    }
    return  c[0].size t2 distortRowIndex
}
