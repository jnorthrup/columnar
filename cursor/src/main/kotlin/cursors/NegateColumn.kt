package cursors

import cursors.io.scala2s
import vec.macros.size

operator fun Cursor.get(vararg skip: NegateColumn): Cursor {
    val toTypedArray = (0 until scala2s.size).toList()
    val toTypedArray1 = (scala2s.get(*skip.map { it.negated }.toTypedArray())).toList()
    val toIntArray = (toTypedArray - toTypedArray1)//.toIntArray()
    return this[toIntArray]
}

operator fun String.unaryMinus() = NegateColumn(this)
public /*inline*/ class NegateColumn(val negated: String)