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
class IntCirQlar(
    val maxSize: Int,
    val al: IntArray =IntArray(maxSize),
) : AbstractQueue<Int>() {
    var tail = 0
     val full get() = maxSize <= tail

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("gonna blow up on mutable ops")
    override fun iterator(): MutableIterator<Int> {
        val v = this.toVect0r()
        return object : Iterator<Int> {
            var x = 0
            override inline fun hasNext(): Boolean {
                return x < v.size
            }

            override inline fun next() = v.second(x++)

        } as MutableIterator<Int>
    }

    fun toList() = toVect0r().mapIndexedToList { _, t -> t }
    fun toVect0r(): Vect0r<Int> = object : Pai2<Int, (Int) -> Int> {
        override val first by al::size
        override val second: (Int) -> Int = { x: Int ->
            al[(tail + x).rem(maxSize)] as Int
        }
    }

    //todo: lockless dequeue here ?
    override fun offer(e:Int): Boolean =
        synchronized(this) {
            al[tail% maxSize ] = e.also { tail = ++tail }
            if(tail==2*maxSize)tail=maxSize
        }.let { true }

    override fun poll() = TODO("Not yet implemented")
    override fun peek() = TODO("Not yet implemented")
    override fun add(k: Int) =offer(k)
    override val size: Int get()= kotlin.math.min(tail, maxSize)
}