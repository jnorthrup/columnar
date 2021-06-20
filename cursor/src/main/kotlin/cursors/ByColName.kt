package cursors

import cursors.io.Vect02_.Companion.right
import cursors.io.colIdx
import vec.macros.Vect02
import vec.macros.size
import vec.macros.toList
import javax.management.openmbean.InvalidKeyException

/**
 * colIdx lands here
 */
operator fun <X, T> Vect02<X, T?>.get(vararg s: T) = right.toList().let { list ->
    s.map {
        val indexOf = list.indexOf(it)
        if (-1 == indexOf)
            throw InvalidKeyException("$it not found in meta among ${list.toString()}" )
        indexOf
    }.toIntArray()
}

/**
 * colIdx lands here
 */
operator fun <X> Vect02<X, String?>.get(vararg s: NegateColumn) =
    ((0 until size).toList() - get(*s.map { it.negated }.toTypedArray()).toList()).toIntArray()


/**
 * cursor["f1","f2","f3"] to auto map the indexes
 */
operator fun Cursor.get(vararg s: String): Cursor = this[colIdx.get(*s)]

