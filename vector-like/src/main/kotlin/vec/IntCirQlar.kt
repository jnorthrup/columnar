package vec

import vec.macros.`➤`
import vec.macros.mapIndexedToList
import vec.macros.t2
import vec.util.rem
import java.util.*

/**

stripped down  circular

only mutability is offer(T)

has cheap direct toVect0r with live properties
has more expensive toList/iterator by copy/concat
 */
class IntCirQlar(
    val maxSize: Int,
    val al: IntArray = IntArray(maxSize),
) : AbstractQueue<Int>() {
    var tail = 0
    val full get() = maxSize <= tail

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("gonna blow up on mutable ops")
    override fun iterator() = this.toVect0r().`➤`.iterator() as MutableIterator<Int>

    fun toList() = toVect0r().mapIndexedToList { _, t -> t }
    @Suppress("OVERRIDE_BY_INLINE")
    fun toVect0r() = this@IntCirQlar.size t2 { x: Int ->
        @Suppress("UNCHECKED_CAST")
        al[((tail >= maxSize) % ((tail + x) % maxSize)) ?: x]
    }


    //todo: lockless dequeue here ?
    override fun offer(e: Int): Boolean =
        synchronized(this) {
            al[tail % maxSize] = e
            tail++
            if (tail == 2 * maxSize) tail = maxSize
            true
        }

    override fun poll(): Int = TODO("Not yet implemented")
    override fun peek(): Int = TODO("Not yet implemented")
    override fun add(k: Int) = offer(k)
    override val size: Int get() = kotlin.math.min(tail, maxSize)
}