package cursors

import cursors.io.right
import cursors.io.colIdx
import vec.macros.*

/**
 * colIdx lands here
 */
 operator fun < X,  T> Vect02<X, T?>.get(vararg s: T) = right.toList().run {
    s.map {
        val indexOf = this.indexOf(it)
        if (-1 == indexOf) throw Exception("$it not found in meta")
        indexOf
    }.toIntArray()
}

 /**
 * colIdx lands here
 */
 operator fun < X > Vect02<X,String?>.get(vararg s: NegateColumn) = ((0 until size).toList() -    get(*s.map { it.negated }.toTypedArray()) .toList()).toIntArray()





/**
 * cursor["f1","f2","f3"] to auto map the indexes
 */
operator fun Cursor.get(vararg s: String): Cursor = this[colIdx.get(*s)]

