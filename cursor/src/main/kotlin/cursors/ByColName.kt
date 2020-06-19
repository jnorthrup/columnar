package cursors

import cursors.io.right
import cursors.io.scala2s
import vec.macros.*
import kotlin.coroutines.CoroutineContext

inline operator fun <reified X, reified T> Vect02<X, T?>.get(vararg s: T) = right.toList().run {
    s.map {
        val indexOf = this.indexOf(it)
        if (-1 == indexOf) throw Exception("$it not found in meta")
        indexOf
    }.toIntArray()
}

operator fun String.unaryMinus() = NegateColumn(this)
public /*inline*/ class NegateColumn(val negated: String)

/**
 * cursor["f1","f2","f3"] to auto map the indexes
 */
operator fun Cursor.get(vararg s: String): Cursor = this[scala2s.get(*s)]
operator fun Cursor.get(vararg skip: NegateColumn): Cursor {
    val toTypedArray = (0 until scala2s.size).toList()
    val toTypedArray1 = (scala2s.get(*skip.map { it.negated }.toTypedArray())).toList()
    val toIntArray = (toTypedArray - toTypedArray1).toIntArray()
    return this[toIntArray]
}

