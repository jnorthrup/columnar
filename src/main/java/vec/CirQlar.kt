package vec

import vec.macros.Pai2
import vec.macros.size
import vec.macros.t2
import java.util.*

/**

stripped down  circular

only mutability is offer(T)

has cheap direct toVect0r with live properties
has more expensive toList/iterator by copy/concat
 */
open class CirQlar<T>(
        val maxSize: Int,
        val evict: ((T) -> Unit)? =null
    ) : AbstractQueue<T>() {
    private val al = arrayOfNulls<Any?>(maxSize)
    var tail: Int = 0
    override val size: Int get() = kotlin.math.min(tail, maxSize)
    val full: Boolean get() = tail >= maxSize

    override fun offer(e: T): Boolean = synchronized(this) {
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
    override fun add(k: T): Boolean = offer(k)
    operator fun CirQlar<T>.plus(k: T): Boolean = offer(k)
    operator fun CirQlar<T>.plusAssign(k: T) {
        offer(k)
    }

    fun toVect0r(): Pai2<Int, (Int) -> T> = ( size t2 { x: Int ->
        al[if (tail >= maxSize) {
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

