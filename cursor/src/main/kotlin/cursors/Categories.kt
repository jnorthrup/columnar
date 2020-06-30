package cursors

import cursors.context.Scalar
import cursors.io.IOMemento.IoInt
import cursors.io.colIdx
import cursors.io.left
import cursors.io.scalars
import cursors.macros.join
import cursors.ml.DummySpec
import vec.macros.*

/***
 *
 * this creates a one-hot encoding set of categories for each value in each column.
 *
 * every distinct (column,value) permutation is reified as (as of this comment, expensive) in-place pivot
 *
 * TODO: staple the catx values to the cursor foreheads
 *
 *
 * maybe this is faster if each (column,value) pair was a seperate 1-column cursor.  first fit for now.
 *
 */

fun Cursor.categories(
        /**
        if this is a index value, that value is omitted from columns. by default null is an omitted value.  if this is a DummySpec, the DummySpec specifies the index.
         */
        dummySpec: Any? = null,
): Cursor {
    val cols = let { parent ->
        val cols = parent.scalars.toList().mapIndexed { i, (a, b) ->
            b t2 mutableMapOf<String, Int>() t3 mutableListOf<Int>()
        }
        for (iy in 0 until parent.size) {
            val row = (parent at iy)
            cols.toList().mapIndexed { i, (_, index, journal) ->
                val v = row.left[i].toString()
                journal += index.getOrPut(v, index::size)
            }
        }
        cols.map { (nama, index, journal) ->

            nama t2 index.keys.toTypedArray() t3 journal.toIntArray()
        }
    }
    val tbj:List<Cursor> = cols.mapIndexed {

        i, (nama, index, journal) ->

        this.size t2 { iy: Int ->
            index.size t2 { ix: Int ->
                (if (journal[iy] == ix) 1 else 0) t2 { Scalar(IoInt, "$nama=${index[ix]}") }
            }
        }
    }
    val map:List<Cursor> = tbj.map { curs: Cursor ->
        val s = curs.scalars.size
        curs[(0 until s) -
                when (dummySpec) {
                    is String -> {
                        curs.colIdx[dummySpec][0]
                    }
                    is Int -> {
                        dummySpec
                    }
                    DummySpec.Last -> {
                        curs.scalars.size - 1
                    }
                    else -> 0
                }]

    }
    return join(*map.toTypedArray())

}