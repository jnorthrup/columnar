package cursors

import cursors.io.colIdx
import vec.macros.Vect02
import vec.macros.Vect02_.right
import vec.macros.size
import vec.macros.toList
import javax.management.openmbean.InvalidKeyException

/**
 * colIdx lands here
 */
inline operator fun <reified X, reified T> Vect02<X, T?>.get(vararg s: T) = right.toList().let { list ->
    s.map {
        val indexOf = list.indexOf(it)
        if (-1 == indexOf)
            throw InvalidKeyException("$it not found in meta among $list")
        indexOf
    }.toIntArray()
}

/**
 * colIdx lands here
 */
inline operator fun <reified X> Vect02<X, String?>.get(vararg s: NegateColumn): IntArray =
    ((0 until size).toSet() - get(*s.map { it.negated }.toTypedArray()).toSet()).toIntArray()


/**
 * cursor["f1","f2","f3"] to auto map the indexes
 */
operator fun Cursor.get(vararg s: String): Cursor = this[colIdx.get(*s)]
