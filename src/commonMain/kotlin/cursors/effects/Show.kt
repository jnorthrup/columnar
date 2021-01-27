package cursors.effects

import cursors.Cursor
import cursors.at
import cursors.io.colIdx
import cursors.io.left
import cursors.io.right
import vec.macros.size
import vec.macros.toList
import vec.util.debug
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

//import kotlin.random.Random breaks graalvm
val random = Random.Default

/*simple printout macro*/
fun Cursor.show(range: IntProgression = 0 until size) {
    val right = colIdx.right
    println("rows:$size" to right.toList())
    showValues(range)
}

fun Cursor.showValues(range: IntProgression) {
    try {
        (range).forEach {
            println((this at it).left.toList())
        }
    } catch (e: NoSuchElementException) {
        e debug "cannot fully access range $range"
    }
}

fun Cursor.head(last: Int = 5) = show(0 until (max(0, min(last, size))))


fun Cursor.showRandom(n: Int = 5) {
    head(0);repeat(n) {
        showValues(random.nextInt(size).let { it..it })
    }
}