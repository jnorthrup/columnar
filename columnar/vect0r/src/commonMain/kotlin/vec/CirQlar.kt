package vec

import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.sync.Mutex
import vec.macros.Vect0r
import vec.macros.size
import vec.macros.t2
import kotlin.math.min


/**

stripped down  circular

only mutability is offer(T)

has cheap direct toVect0r with live properties
has more expensive toList/iterator by copy/concat
 */
class CirQlar<T>(
    private val maxSize: Int,
) : Iterable<T> {
    private val mutex: Mutex = Mutex(false)
    private val al = arrayOfNulls<Any?>(maxSize)
    private var tail = 0
    val size get() = min(tail, maxSize)
    val full get() = tail >= maxSize


    //todo: lockless dequeue here ?
    @OptIn(InternalCoroutinesApi::class)
    suspend fun offer(e: T): Boolean = let {
        ports.synchronized(this.mutex) {
            val i = tail % maxSize
            al[i] = e
            tail++
            if (tail == 2 * maxSize) tail = maxSize
            true
        }
    }

    fun toList(): List<T> {
        val iterator = iterator()
        return List(size) {
            val next = iterator.next()
            next
        }
    }
    @Suppress("OVERRIDE_BY_INLINE", "UNCHECKED_CAST")
    fun toVect0r():Vect0r<T> = this@CirQlar.size t2 { x: Int ->
        al[if (tail >= maxSize) {
            (tail + x) % maxSize
        } else x] as T
    }

    override fun iterator(): Iterator<T> = object : Iterator<T> {
        val v = toVect0r()
        var i = 0
        override fun hasNext(): Boolean = i < v.size
        override fun next(): T = v.second(i++)
    }
}
