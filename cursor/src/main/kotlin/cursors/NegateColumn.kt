package cursors

import cursors.io.colIdx
import vec.macros.size

class NegateColumn(val negated: String)

/**
 * be careful in client code with importing Cursor.get and not Vect0r.get
 */
operator fun Cursor.get(vararg skip: NegateColumn): Cursor {
    val toIntArray = ((0 until colIdx.size).toList() - colIdx.get(*skip.map(NegateColumn::negated).toTypedArray()).toList()).toIntArray()
    return (this)[toIntArray]
}
operator fun String.unaryMinus() = NegateColumn(this)

