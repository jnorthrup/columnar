package cursors.effects

import cursors.Cursor
import cursors.at
import cursors.io.colIdx
import vec.macros.Vect02_.left
import vec.macros.Vect02_.right
import vec.macros.combine
import vec.macros.size
import vec.macros.toList
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

//import kotlin.random.Random breaks graalvm
val random = java.util.Random()

/*simple printout macro*/
fun Cursor.show(range: IntProgression = 0 until size) {
    println("rows:$size" to colIdx.right.toList())
    showValues(range)
}

fun Cursor.showValues(range: IntProgression) {
    try {
        (range).forEach {
            val pai2 = this at it
            val combine = combine(pai2.left)
            println(combine.toList())
        }
    } catch (e: NoSuchElementException) {
//        e
        System.err.println("cannot fully access range $range")
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
        if (size > 0) showValues(Random.nextInt(0, size).let { it..it })
    }
}
