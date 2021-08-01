package vec

import vec.macros.Pai2
import vec.macros.Vect0r
import vec.macros.mapIndexedToList
import vec.macros.size
import java.util.*

/**

stripped down  circular

only mutability is offer(T)

has cheap direct toVect0r with live properties
has more expensive toList/iterator by copy/concat
 */
class CirQlar<T>(
    val maxSize: Int,
    val al: MutableList<T> = arrayListOf<T>().also { it.ensureCapacity(maxSize) },
) : AbstractQueue<T>() {
    var tail = 0
    override val size = al.size
    val full get() = maxSize == size

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("gonna blow up on mutable ops")
    override fun iterator(): MutableIterator<T> {
        val v = this.toVect0r()
        return object : Iterator<T> {
            var x = 0
            override inline fun hasNext(): Boolean {
                return x < v.size
            }

            override inline fun next() = v.second(x++)

        } as MutableIterator<T>
    }

    fun toList() = toVect0r().mapIndexedToList { _, t -> t }
    fun toVect0r(): Vect0r<T> = object : Pai2<Int, (Int) -> T> {
        override val first by al::size
        override val second = { x: Int ->
            al[(tail + x).rem(maxSize)]
        }
    }

    //todo: lockless dequeue here ?
    override fun offer(e: T): Boolean =
        synchronized(this) {
            when (al.size) {
                maxSize -> al[tail] = e.also { tail = (++tail).rem(maxSize) }
                else -> al.add(e)
            }
        }.let { true }

    override fun poll(): T = TODO("Not yet implemented")
    override fun peek(): T = TODO("Not yet implemented")
    override fun add(k: T) = TODO("Not yet implemented")
}