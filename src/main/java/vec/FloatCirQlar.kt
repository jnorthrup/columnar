package vec

import vec.macros.*
import java.util.*

/**

stripped down  circular

only mutability is offer(T)

has cheap direct toVect0r with live properties
has more expensive toList/iterator by copy/concat
 */
class FloatCirQlar(
    val maxSize: Int,
    val al: FloatArray = FloatArray(maxSize),
) : AbstractQueue<Float>() {
    var tail: Int = 0
    val full: Boolean get() = maxSize <= tail

    override fun iterator(): MutableIterator<Float> = this.toVect0r().  iterator() as MutableIterator<Float>

    fun toList(): List<Float> = toVect0r().mapIndexedToList { _, t -> t }

    fun toVect0r(): Pai2<Int, (Int) -> Float> = this@FloatCirQlar.size t2 { x: Int ->
        al[if (tail >= maxSize) {
            (tail + x) % maxSize
        } else x]
    }

    //todo: lockless dequeue here ?
    override fun offer(e: Float): Boolean =
        synchronized(this) {
            al[tail % maxSize] = e
            tail++
            if (tail == 2 * maxSize) tail = maxSize
            true
        }

    override fun poll(): Float = TODO("Not yet implemented")
    override fun peek(): Float = TODO("Not yet implemented")
    override fun add(k: Float): Boolean = offer(k)
    override val size: Int get() = kotlin.math.min(tail, maxSize)
}