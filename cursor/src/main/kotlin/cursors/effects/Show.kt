package cursors.effects

import cursors.Cursor
import cursors.at
import cursors.io.left
import cursors.io.right
import cursors.io.scala2s
import vec.macros.size
import vec.macros.toList
import kotlin.math.min

/*simple printout macro*/
fun Cursor.show(range: IntProgression = 0 until size) {
    println("rows:$size" to scala2s.right.toList())
    showValues(range)
}

fun Cursor.showValues(range: IntProgression) {
    (range).forEach {
        println((this at it).left.toList())
    }
}
fun Cursor.head ( last:Int)=show(0 until (min(last,size)))