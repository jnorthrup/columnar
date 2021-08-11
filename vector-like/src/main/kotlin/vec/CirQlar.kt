package vec

import vec.macros.*
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
    val al: Array<Any?> = arrayOfNulls(maxSize),
) : AbstractQueue<T>() {
    var tail = 0
    override val size = al.size
    val full get() = tail>= maxSize

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("gonna blow up on mutable ops")
    override fun iterator() =( this.toVect0r() α {it as Any?}).`➤`.iterator() as MutableIterator<T>


    fun toList() = toVect0r().mapIndexedToList { _, t -> t }
    @Suppress("OVERRIDE_BY_INLINE")
    fun toVect0r(): Vect0r<T> = object : Pai2<Int, (Int) -> T> {
         override inline val first  get()= min(tail,maxSize)
        override   val second  = { x: Int ->
            @Suppress("UNCHECKED_CAST")
            al[(tail + x).rem(maxSize)]  as T
        }
    }

    //todo: lockless dequeue here ?
    override fun offer(e: T) = synchronized(this) {
            al[tail % maxSize] = e
            tail++
            if (tail == 2 * maxSize) tail = maxSize
            true
        }

    override fun poll(): T = TODO("Not yet implemented")
    override fun peek(): T = TODO("Not yet implemented")
    override fun add(k: T) = offer(k)
    operator fun <T> CirQlar<T>.plus(k: T) = offer(k)
    operator fun <T> CirQlar<T>.plusAssign(k: T) {
        offer(k)
    }
}

