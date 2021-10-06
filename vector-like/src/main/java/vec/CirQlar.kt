

package vec

import vec.macros.size
import vec.macros.t2
import vec.util.rem
import java.util.*
import kotlin.math.min

/**

stripped down  circular

only mutability is offer(T)

has cheap direct toVect0r with live properties
has more expensive toList/iterator by copy/concat
 */
class CirQlar<T>(
    val maxSize: Int,

    ) : AbstractQueue<T>() {
    private val al = arrayOfNulls<Any?>(maxSize)
    var tail = 0
    override val size get() = min(tail, maxSize)
    val full get() = tail >= maxSize


    //todo: lockless dequeue here ?
    override fun offer(e: T) = synchronized(this) {
        val i = tail % maxSize
        al[i] = e
        tail++
        if (tail == 2 * maxSize) tail = maxSize
        true
    }

    fun toList(): List<T> {
        val iterator = iterator()
        return List(size) {
            val next = iterator.next()
            next
        }
    }

    override fun poll(): T = TODO("Not yet implemented")
    override fun peek(): T = TODO("Not yet implemented")
    override fun add(k: T) = offer(k)
    operator fun <T> CirQlar<T>.plus(k: T) = offer(k)
    operator fun <T> CirQlar<T>.plusAssign(k: T) {
        offer(k)
    }

    @Suppress("OVERRIDE_BY_INLINE")
    fun toVect0r() = (this@CirQlar.size t2 { x: Int ->
        @Suppress("UNCHECKED_CAST")
        al[((tail >= maxSize) % ((tail + x) % maxSize)) ?: x] as T
    })

    override fun iterator(): MutableIterator<T> = object : MutableIterator<T> {
        val v = toVect0r()
        var i = 0
        override fun hasNext(): Boolean = i < v.size

        override fun next(): T = v.second(i++)

        override fun remove(): Unit = TODO("Not yet implemented")
    }

}

