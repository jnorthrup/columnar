package cursors.calendar

//import cursors.io.left
import cursors.Cursor
import cursors.context.Arity
import cursors.context.Scalar
import cursors.context.Scalar.Companion.Scalar
import cursors.get
import cursors.io.IOMemento.IoInstant
import cursors.io.RowVec
import cursors.io.colIdx
import cursors.macros.join
import cursors.unaryMinus
import vec.macros.*
import java.time.Instant

class UnixTimeRemapper {
    companion object {
        /**
         * will select IoLong columns by name and project java.time.Instant column reordered into index 0..n joined
         * with the original cursor
         */

        
        fun timestampFromIoLong(vararg timestampColumnNames: String): (Cursor) -> Cursor = { c0: Cursor ->
            val newKeys: IntArray = c0.colIdx.get(*timestampColumnNames)
            val thinned: IntArray = c0.colIdx.get(*timestampColumnNames.map(String::unaryMinus).toTypedArray()) //-"column"
            val leftovers: Cursor = c0[thinned]
            val c2: Cursor = c0[newKeys] α { rv: RowVec ->
                rv.size t2 rv.second α { (unixtime, desc) ->
                    Instant.ofEpochMilli((unixtime as? Long) ?: unixtime.toString().toLongOrNull()?:0L) t2 {
                        val (_, f) = desc().get(Arity.arityKey) as Scalar
                        Scalar(IoInstant, f)
                    }
                }
            }
            join(c2, leftovers)
        }
    }
}

