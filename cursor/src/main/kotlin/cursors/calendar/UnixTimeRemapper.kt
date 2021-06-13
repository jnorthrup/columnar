package cursors.calendar

import cursors.Cursor
import cursors.at
import cursors.context.Scalar
import cursors.get
import cursors.io.IOMemento
import cursors.io.RowVec
import cursors.io.Vect02_.Companion.left
import cursors.io.colIdx
//import cursors.io.left
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

        @JvmStatic
        fun timestampFromIoLong(vararg timestampColumnNames: String): (Cursor) -> Cursor = { c0: Cursor ->
            val newKeys: IntArray = c0.colIdx.get(*timestampColumnNames)
            val s = timestampColumnNames.map(String::unaryMinus).toTypedArray()
            val thinned = c0.colIdx.get(*s)
            val leftovers: Cursor = c0[thinned]

            val c2 :Cursor =
                Cursor(c0.size) { y: Int ->
                    val rv1=c0 at  y
                    val vals  = rv1.left

                    RowVec(newKeys.size) { x: Int ->
                        val i = newKeys[x]
                        val any = vals[i]
                        val v2  = (any as? Long) ?: any.toString().toLong()
                        Instant.ofEpochMilli(v2) t2 { Scalar(IOMemento.IoInstant, timestampColumnNames[x]) }
                    }
                }


            /*
            c0[newKeys] α { (a, b) ->
                a t2 b α { (valueStringOrLong, bbContext) ->
                    Instant.ofEpochMilli((valueStringOrLong as? Long) ?: valueStringOrLong.toString().toLong()) t2 {
                        val (_, f) = bbContext().get(Arity.arityKey) as Scalar;
                        Scalar(IOMemento.IoInstant, f)
                    }
                }
            }
*/
            join( c2, leftovers)
        }
    }
}

