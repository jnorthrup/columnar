package vec

import vec.macros.Pai2
import vec.macros.mapIndexedToList
import vec.macros.t2
import vec.macros.`➤`
import java.util.*

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
    var tail: Int = 0
    val full: Boolean get() = maxSize <= tail

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("gonna blow up on mutable ops")
    override fun iterator(): MutableIterator<Double> = this.toVect0r().`➤`.iterator() as MutableIterator<Double>

    fun toList(): List<Double> = toVect0r().mapIndexedToList { _, t -> t }

    fun toVect0r(): Pai2<Int, (Int) -> Double> = (this@DoubleCirQlar.size t2 { x: Int ->
        al[if (tail >= maxSize) {
            (tail + x) % maxSize
        } else x]
    })

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
    override fun add(k: Double): Boolean = offer(k)
    override val size: Int get() = kotlin.math.min(tail, maxSize)
}