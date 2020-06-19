package cursors

import cursors.io.right
import cursors.io.scala2s
import vec.macros.*

inline operator fun <reified X, reified T> Vect02<X, T?>.get(vararg s: T) = right.toList().run {
    s.map {
        val indexOf = this.indexOf(it)
        if (-1 == indexOf) throw Exception("$it not found in meta")
        indexOf
    }.toIntArray()
}

/**
 * cursor["f1","f2","f3"] to auto map the indexes
 */
operator fun Cursor.get(vararg s: String): Cursor = this[scala2s.get(*s)]

