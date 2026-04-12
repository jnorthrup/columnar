package cursors.effects

import cursors.Cursor
import cursors.at
import cursors.io.colIdx
import vec.macros.Vect02
import vec.macros.combine
import vec.macros.left
import vec.macros.right
import vec.macros.size
import vec.macros.toList
import kotlin.math.max
import kotlin.math.min
import kotlin.random.*

//import kotlin.random.Random breaks graalvm
val random: java.util.Random = java.util.Random()

/*simple printout macro*/
fun Cursor.show(range: IntProgression = 0 until size) {
    println("rows:$size" to (colIdx as Vect02<Nothing, String?>).right.toList())
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
        System.err.println("cannot fully access range $range")
    }
}

/**
 * head default 5 rows
 * just like unix head - print default 5 lines from cursor contents to stdout
 */
@JvmOverloads fun Cursor.head(last: Int = 5): Unit = show(0 until (max(0, min(last, size))))

/**
 * run head starting at random index
 */
fun Cursor.showRandom(n: Int = 5) {
    head(0);repeat(n) {
        if (size > 0) showValues(Random.nextInt(0, size).let { it..it })
    }
}
