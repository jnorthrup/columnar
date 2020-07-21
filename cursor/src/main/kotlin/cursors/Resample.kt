package cursors

import cursors.calendar.daySeq
import cursors.calendar.feature_range
import cursors.io.scalars
import vec.macros.*
import java.time.LocalDate


/**
 * creates new cursor with synthetic rows combined at end with ordered missing index localdates
 * @param indexcol the column with dates to resample
 */
fun Cursor.resample(indexcol: Int): Cursor = let {
    val curs: Cursor = this[indexcol]
    val indexValues = curs.narrow().map { it.first() as LocalDate }.toSequence()
    val (min, max) = feature_range<LocalDate>(indexValues, LocalDate.MAX t2 LocalDate.MIN)
    val scalars = this.scalars
    val sequence = daySeq(min, max) - indexValues
    val indexVec = sequence.toVect0r()
    val cursor: Cursor = Cursor(indexVec.first) { iy: Int ->
        Pai2(scalars.first) { ix: Int ->
            val any = when (ix == indexcol) {
                true -> indexVec[iy]
                else -> null
            }
            any t2 { scalars[ix] }
        }
    }
    combine(this, cursor)
}
