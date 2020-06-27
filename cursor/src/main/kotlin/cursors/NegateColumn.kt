package cursors

import cursors.io.scala2s
import vec.macros.size

class NegateColumn(val negated: String)

/**
 * be careful in client code with importing Cursor.get and not Vect0r.get
 */
operator fun Cursor.get(vararg skip: NegateColumn): Cursor {
    val toIntArray = ((0 until scala2s.size).toList() - scala2s.get(*skip.map(NegateColumn::negated).toTypedArray()).toList()).toIntArray()
    return (this)[toIntArray]
}
operator fun String.unaryMinus() = NegateColumn(this)

