package cursors.effects

import cursors.Cursor
import cursors.at
import cursors.io.Vect02_.Companion.left
import cursors.io.Vect02_.Companion.right
import cursors.io.colIdx
import vec.macros.size
import vec.macros.toList
import vec.util.debug
import kotlin.math.max
import kotlin.math.min

//import kotlin.random.Random breaks graalvm
val random = java.util.Random()

/*simple printout macro*/
fun Cursor.show(range: IntProgression = 0 until size) {
    val right = colIdx.right
    val     toList = right.toList()
    println("rows:$size" to toList)
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

/**
 * head default 5 rows
 */
fun Cursor.head(last: Int = 5) = show(0 until (max(0, min(last, size))))

/**
 * run head at random index
 */
fun Cursor.showRandom(n: Int = 5) {
    head(0);repeat(n) {
        showValues(random.nextInt(size).let { it..it })
    }
}