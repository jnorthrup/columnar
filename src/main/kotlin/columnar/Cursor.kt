package columnar

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

fun Cursor.resample(indexcol: Int) = let {
    val curs = this[indexcol]
    val indexValues = curs.narrow().map { it: List<Any?> -> it.first() as LocalDate }.toSequence()


    val (min, max) = feature_range(indexValues)

    val scalars = this.scalars
    val rowVecSize = scalars.size


    val sequence = daySeq(min, max) - indexValues
    val indexVec = sequence.toVect0r()
    val cursor: Cursor = Cursor(indexVec.first) { iy: Int ->
        RowVec(rowVecSize) { ix: Int ->
            val any = when (ix == indexcol) {
                true -> indexVec[iy]
                else -> null
            }
            any t2 (scalars[ix] as CoroutineContext).`⟲`
        }
    }
    combine(this, cursor)
}

fun feature_range(seq: Sequence<LocalDate>) = seq.fold(LocalDate.MAX t2 LocalDate.MIN) { (a, b), localDate ->
    minOf(a, localDate) t2 maxOf(b, localDate)
}
