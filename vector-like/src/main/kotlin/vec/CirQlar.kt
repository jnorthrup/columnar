package vec

import vec.macros.Pai2
import vec.macros.Vect0r
import java.util.*

/**

stripped down  circular

only mutability is offer(T)

has cheap direct toVect0r with live properties
has more expensive toList/iterator by copy/concat
 */
class CirQlar<T>(
    val maxSize: Int,
    val backingStore: MutableList<T> = arrayListOf<T>().also { it.ensureCapacity(maxSize) },
) : AbstractQueue<T>() {
    var tail = 0
    override val size = backingStore.size
    val full get() = maxSize == size
    fun get(x:Int)    = backingStore::get
    @Deprecated("gonna blow up on mutable ops")
    override fun iterator(): MutableIterator<T> =
        this.toList<T>().iterator() as MutableIterator<T> // gonna blow on mutable ops

    fun toList() = when (backingStore.size) {
        maxSize -> backingStore.drop(tail) + backingStore.dropLast(maxSize - tail)
        else -> backingStore
    }


    /**
     * this is a mutable Vect0r that is going to violate reverse,combine,join repeatability.
     */
    fun toVect0r(): Vect0r<T> = object : Pai2<Int, (Int) -> T> {
        override val first by backingStore::size

        override val second = { x: Int ->
            backingStore[(tail + x).rem(maxSize)]
        }
    }

    //todo: lockless dequeue here ?
    override fun offer(e: T): Boolean =
        synchronized(this) {
            when (backingStore.size) {
                maxSize -> backingStore[tail] = e.also { tail = (++tail).rem(maxSize) }
                else -> backingStore.add(e)
            }
        }.let { true }

    override fun poll(): T = TODO("Not yet implemented")
    override fun peek(): T = TODO("Not yet implemented")
    override fun add(k: T) = TODO("Not yet implemented")
}
