package vec

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
class FloatCirQlar(
    val maxSize: Int,
    val al: FloatArray = FloatArray(maxSize),
) : AbstractQueue<Float>() {
    var tail = 0
    val full get() = maxSize <= tail

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("gonna blow up on mutable ops")
    override fun iterator() = this.toVect0r().`➤`.iterator() as MutableIterator<Float>

    fun toList() = toVect0r().mapIndexedToList { _, t -> t }

    @Suppress("OVERRIDE_BY_INLINE")
    fun toVect0r() = (this@FloatCirQlar.size t2 { x: Int ->
        @Suppress("UNCHECKED_CAST")
        al[if (tail >= maxSize) {
            (tail + x) % maxSize
        } else x]
    })

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
    override fun add(k: Float) = offer(k)
    override val size: Int get() = kotlin.math.min(tail, maxSize)
}