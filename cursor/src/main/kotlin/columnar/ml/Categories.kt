package columnar.ml

import columnar.Cursor
import columnar.at
import columnar.context.Scalar
import columnar.io.*
import columnar.macros.*

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
    if this is a value, that value is omitted from columns. by default null is an omitted value.  if this is a DummySpec, the DummySpec specifies the index
     */
    dummySpec: Any? = null
): Cursor = let { curs ->
    val origScalars = curs.scalars
    val xSize = origScalars.size
    val ySize = curs.size
/* todo: vector */
    val sequence = sequence<Cursor> {
        for (catx in 0 until xSize) {
            val cat2 = sequence/* todo: vector */ {
                for (iy in 0 until ySize)
                    yield((curs at (iy))[0].first)
            }.distinct().toList().let { cats ->
                val noDummies = onehot_mask(dummySpec, cats)
                if (noDummies.first > -1)
                    cats - cats[noDummies.first]
                else
                    cats
            }


            val catxScalar = origScalars[catx]
            yield(Cursor(curs.size) { iy: Int ->
                RowVec(cat2.size) { ix: Int ->
                    val cell = (curs at (iy))[catx]
                    val rowValue = cell.first
                    val diagonalValue = cat2[ix]
                    val cardinal = if (rowValue == diagonalValue) 1 else 0
                    cardinal t2 {
                        /**
                         * there may be context data other than simple scalars in this cell, so we will just replace the scalar key and pass it along.
                         */
                        /**
                         * there may be context data other than simple scalars in this cell, so we will just replace the scalar key and pass it along.
                         */
                        cell.second() + Scalar(
                            IOMemento.IoInt,
                            origScalars[catx].second + "_" + diagonalValue.toString()
                        )
                    }
                }
            })
        }
    }

    //widthwize join (90 degrees of combine, right?)
    join(sequence.toVect0r())
}