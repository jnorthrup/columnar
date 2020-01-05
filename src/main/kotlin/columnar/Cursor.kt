package columnar

import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import kotlin.coroutines.CoroutineContext

typealias Cursor = Vect0r<RowVec>
fun daySeq(min: LocalDate, max: LocalDate): Sequence<LocalDate> {
    var cursor = min
    return sequence {
        while (max > cursor) {
            yield(cursor)
            cursor = cursor.plusDays(1)
        }
    }
}
fun Cursor.resample(indexcol:Int)=let{
    val curs = this[indexcol]
    val indexValues = curs.narrow().toList().map {it.first() as LocalDate}
    val max = indexValues.toList().max()!!
    val min = indexValues.toList().min()!!

    val scalars = this.scalars
    val rowVecSize = scalars.size


    val indexVec = (daySeq(min, max) - indexValues).toVect0r()
    val cursor = Cursor (  indexVec.first   ){ iy: Int ->
        RowVec(rowVecSize) { ix: Int ->
            val any = when (ix == indexcol) {
                true -> indexVec[iy]
                else -> null as Any?
            }
            any t2 (scalars[ix]as CoroutineContext).`⟲`
        }
    }as Cursor
    combine(this as Cursor, cursor  as Cursor ) as  Cursor


}