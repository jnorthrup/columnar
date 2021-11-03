package cursors

import vec.macros.*
import java.util.*
import kotlin.Comparator

fun Cursor.ordered(
    axis: IntArray,
    comparator: Comparator<List<Any?>> = Comparator(::cmpAny),
): Cursor = combine(
    (keyClusters(
        axis,
        comparator.run { TreeMap(comparator) }) `→` MutableMap<List<Any?>, MutableList<Int>>::values α
            (IntArray::toVect0r `⚬` MutableList<Int>::toIntArray))
).let {
    Cursor(it.size) { iy: Int ->
        val ix2 = it[iy]
        this at ix2
    }
}