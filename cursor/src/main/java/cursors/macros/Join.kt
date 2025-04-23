package cursors.macros

import cursors.Cursor
import cursors.io.RowVec
import cursors.io.scalars
// Make sure all necessary vec.macros imports are present
import cursors.io.CellMeta
import vec.macros.*

/**
 * Joins multiple Cursors column-wise. Assumes all cursors have the same number of rows.
 * This version takes a Vect0r of Cursors.
 */
fun join(vargs: Vect0r<Cursor>): Cursor {
    // Convert Vect0r to Array manually to avoid nullable spread operator issue
    val cursorArray = Array(vargs.size) { i -> vargs[i] }
    return join(*cursorArray)
}

/**
 * Joins multiple Cursors column-wise. Assumes all cursors have the same number of rows.
 * This version takes varargs Cursors.
 */
fun join(vararg vargs: Cursor): Cursor {
    // Check if there's anything to join
    if (vargs.isEmpty()) {
        throw IllegalArgumentException("Cannot join an empty list of cursors")
    }
    if (vargs.size == 1) {
        return vargs[0] // No need to join if only one cursor
    }

    // Original logic using muxIndexes
    return muxIndexes(vargs.map(Cursor::scalars)).let { (totalWidth, tails) ->
        val numRows = vargs[0].size // Assuming all cursors have the same number of rows

        object : Cursor {
            override val first: Int
                get() = numRows
                
            override val second: (Int) -> RowVec
                get() = { rowIndex: Int ->
                    object : RowVec {
                        override val first: Int
                            get() = totalWidth
                            
                        override val second: (Int) -> Pai2<Any?, CellMeta>
                            get() = { columnIndex: Int ->
                                val (sourceCursorIndex, indexInSourceCursor) = demuxIndex(tails, columnIndex)
                                val sourceCursor = vargs[sourceCursorIndex]
                                val sourceRow = sourceCursor.second(rowIndex)
                                val cell = sourceRow.second(indexInSourceCursor)
                                Pai2(cell.first, cell.second)
                            }
                    }
                }
        }
    }
}
