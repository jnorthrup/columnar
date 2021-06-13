package cursors.calendar

import cursors.Cursor
import cursors.context.Arity
import cursors.context.Scalar
import cursors.get
import cursors.io.IOMemento
import cursors.io.colIdx
import cursors.macros.join
import cursors.unaryMinus
import vec.macros.t2
import vec.macros.α
import java.time.Instant

class UnixTimeRemapper {
    companion object {
        /**
         * will select IoLong columns by name and project java.time.Instant column reordered into index 0..n joined
         * with the original cursor
         */

        @JvmStatic
        fun timestampFromIoLong(vararg timestampColumnNames: String): (Cursor) -> Cursor = { c0: Cursor ->
            val newKeys: IntArray = c0.colIdx.get(*timestampColumnNames)
            val leftovers: Cursor = c0[c0.colIdx.get(*timestampColumnNames.map(String::unaryMinus).toTypedArray())]

            join(
                c0[newKeys] α { (a, b) ->
                    a t2 b α { (valueStringOrLong, bbContext) ->
                        Instant.ofEpochMilli(valueStringOrLong.toString().toLong()) t2 {  val (_, f) = bbContext().get(Arity.arityKey) as Scalar;Scalar(IOMemento.IoInstant, f)}
                     }
               } , leftovers)
        }
    }
}

