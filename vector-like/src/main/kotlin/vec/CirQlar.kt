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
    val al: MutableList<T> = arrayListOf<T>().also { it.ensureCapacity(maxSize) },
) : AbstractQueue<T>() {
    var tail = 0
    override val size = al.size

    @Deprecated("gonna blow up on mutable ops")
    override fun iterator(): MutableIterator<T> =
        this.toList<T>().iterator() as MutableIterator<T> // gonna blow on mutable ops

    fun toList() = when (al.size) {
        maxSize -> al.drop(tail) + al.dropLast(maxSize - tail)
        else -> al
    }


    /**
     * this is a mutable Vect0r that is going to violate reverse,combine,join repeatability.
     */
    fun toVect0r(): Vect0r<T> = object : Pai2<Int, (Int) -> T> {
            override val first by al::size

            override val second= {x:Int->
                al[(tail+x).rem(maxSize)]
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