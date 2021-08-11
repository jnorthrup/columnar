package vec

import vec.macros.Pai2
import vec.macros.Vect0r
import vec.macros.`➤`
import vec.macros.mapIndexedToList
import java.util.*
import kotlin.math.min

/**

stripped down  circular

only mutability is offer(T)

has cheap direct toVect0r with live properties
has more expensive toList/iterator by copy/concat
 */
class DoubleCirQlar(
    val maxSize: Int,
    val al: DoubleArray = DoubleArray(maxSize),
) : AbstractQueue<Double>() {
    var tail = 0
    val full get() = maxSize <= tail

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("gonna blow up on mutable ops")
    override fun iterator() = this.toVect0r().`➤`.iterator() as MutableIterator<Double>

    fun toList() = toVect0r().mapIndexedToList { _, t -> t }
    fun toVect0r(): Vect0r<Double> = object : Pai2<Int, (Int) -> Double> {
        @Suppress("OVERRIDE_BY_INLINE")
        override inline val first get()= min(tail,maxSize)
        override val second= { x: Int ->
            al[(tail + x) % maxSize]
        }
    }

    //todo: lockless dequeue here ?
    override fun offer(e: Double): Boolean =
        synchronized(this) {
            al[tail % maxSize] = e
            tail++
            if (tail == 2 * maxSize) tail = maxSize
            true
        }

    override fun poll(): Double = TODO("Not yet implemented")
    override fun peek(): Double = TODO("Not yet implemented")
    override fun add(k: Double) = offer(k)
    override val size: Int get() = kotlin.math.min(tail, maxSize)
}