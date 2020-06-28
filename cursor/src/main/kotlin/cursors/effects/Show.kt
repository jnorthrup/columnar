package cursors.effects

import cursors.Cursor
import cursors.at
import cursors.io.colIdx
import cursors.io.left
import cursors.io.right
import vec.macros.size
import vec.macros.toList
import kotlin.math.min

/*simple printout macro*/
fun Cursor.show(range: IntProgression = 0 until size) {
    println("rows:$size" to colIdx.right.toList())
    showValues(range)
}

fun Cursor.showValues(range: IntProgression) {
    (range).forEach {
        println((this at it).left.toList())
    }
}

fun Cursor.head(last: Int = 5) = show(0 until (min(last, size)))