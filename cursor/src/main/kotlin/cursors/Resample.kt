package cursors

import cursors.calendar.daySeq
import cursors.calendar.feature_range
import cursors.io.scalars
import vec.macros.*
import vec.macros.Vect02_.left
import java.time.LocalDate


/**
 * creates new cursor with synthetic rows combined at end with ordered missing index localdates
 * @param indexcol the column with dates to resample
 */
fun Cursor.resample(indexcol: Int): Cursor = let {
    val indexValues = (combine(this[indexcol]).left α { it as LocalDate }).`➤`
    val (min, max) = feature_range(indexValues, LocalDate.MAX t2 LocalDate.MIN)
    val scalars = this.scalars
    val sequence: Sequence<LocalDate> = daySeq(min, max) - indexValues
    val indexVec = sequence.toVect0r()
    val cursor: Cursor =
        indexVec.size t2 { iy: Int ->
            scalars.size t2 { ix: Int ->
                ix.takeIf { it == indexcol }?.let { indexVec[iy] } t2 { scalars[ix] }
            }
        }
    combine(this, cursor)
}
