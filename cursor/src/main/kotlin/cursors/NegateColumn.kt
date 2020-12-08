package cursors

import cursors.io.colIdx
import vec.util.logDebug

class NegateColumn(val negated: String)

/**
 * be careful in client code with importing Cursor.get and not Vect0r.get
 */
operator fun Cursor.get(vararg skip: NegateColumn): Cursor {

    logDebug { "solving for direct-reindexing negation ${skip.map { it.negated }}" }
    val indexes = this.colIdx.get(*skip)
    logDebug { "direct-reindexing negation indexes are ${indexes.toList()}" }
    return this[indexes]
}

operator fun String.unaryMinus() = NegateColumn(this)

