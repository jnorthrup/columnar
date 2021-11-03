package cursors.calendar

import cursors.Cursor
import cursors.context.Arity
import cursors.context.Scalar
import cursors.get
import cursors.io.IOMemento
import cursors.io.RowVec
import cursors.io.colIdx
import cursors.macros.join
import cursors.unaryMinus
import kotlinx.datetime.Instant
import vec.macros.size
import vec.macros.t2
import vec.macros.α

class UnixTimeRemapper {
    companion object {
        /**
         * will select IoLong columns by name and project java.time.Instant column reordered into index 0..n joined
         * with the original cursor
         */


        fun timestampFromIoLong(vararg timestampColumnNames: String): (Cursor) -> Cursor = { c0: Cursor ->
            val newKeys: IntArray = c0.colIdx.get(*timestampColumnNames)
            val thinned: IntArray =
                c0.colIdx.get(*timestampColumnNames.map(String::unaryMinus).toTypedArray()) //-"column"
            val leftovers: Cursor = c0[thinned]
            val c2: Cursor = c0[newKeys] α { rv: RowVec ->
                rv.size t2 rv.second α { (unixtime, desc) ->
                    Instant.fromEpochMilliseconds(unixtime as? Long ?: unixtime.toString().toLongOrNull() ?: 0L) t2 {
                        val (_, f) = desc().get(Arity.arityKey) as Scalar
                        Scalar.Scalar(IOMemento.IoInstant, f)
                    }
                }
            }
            join(c2, leftovers)
        }
    }
}