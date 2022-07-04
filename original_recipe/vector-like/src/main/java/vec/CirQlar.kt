package vec

import vec.macros.size
import vec.macros.t2
import java.util.*

/**

stripped down  circular

only mutability is offer(T)

has cheap direct toVect0r with live properties
has more expensive toList/iterator by copy/concat
 */
public open class CirQlar<T>(
        val maxSize: Int,
        val evict: ((T) -> Unit)? =null
    ) : AbstractQueue<T>() {
    private val al = arrayOfNulls<Any?>(maxSize)
    var tail = 0
    override val size get() = kotlin.math.min(tail, maxSize)
    val full get() = tail >= maxSize


    //todo: lockless dequeue here ?
    override fun offer(e: T) = synchronized(this) {
        val i = tail % maxSize
        val tmp =evict?.run { al.takeIf { it.size < i }?.get(i) }
        al[i] = e
        tail++
        if (tail == 2 * maxSize) tail = maxSize
        tmp?.let {t-> evict?.invoke(t as T)    }
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
    operator fun CirQlar<T>.plus(k: T) = offer(k)
    operator fun CirQlar<T>.plusAssign(k: T) {
        offer(k)
    }

    @Suppress("OVERRIDE_BY_INLINE")
    fun toVect0r() = ( size t2 { x: Int ->
        @Suppress("UNCHECKED_CAST") al[if (tail >= maxSize) {
            (tail + x) % maxSize
        } else x] as T
    })

    override fun iterator(): MutableIterator<T> = object : MutableIterator<T> {
        val v = toVect0r()
        var i = 0
        override fun hasNext(): Boolean = i < v.size

        override fun next(): T = v.second(i++)

        override fun remove(): Unit = TODO("Not yet implemented")
    }
}

