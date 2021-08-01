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
class DoubleCirQlar(
    val maxSize: Int,
    val al: DoubleArray =DoubleArray(maxSize)  ,
) : AbstractQueue<Double>() {
    var tail = 0
     val full get() = maxSize <= tail

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("gonna blow up on mutable ops")
    override fun iterator(): MutableIterator<Double> {
        val v = this.toVect0r()
        return object : Iterator<Double> {
            var x = 0
            override inline fun hasNext(): Boolean {
                return x < v.size
            }

            override inline fun next() = v.second(x++)

        } as MutableIterator<Double>
    }

    fun toList() = toVect0r().mapIndexedToList { _, t -> t }
    fun toVect0r(): Vect0r<Double> = object : Pai2<Int, (Int) -> Double> {
        override val first by al::size
        override val second: (Int) -> Double = { x: Int ->
            al[(tail + x).rem(maxSize)] as Double
        }
    }

    //todo: lockless dequeue here ?
    override fun offer(e: Double): Boolean =
        synchronized(this) {

            al[tail% maxSize ] = e.also { tail = ++tail }
            if(tail==2*maxSize)tail=maxSize
        }.let { true }

    override fun poll(): Double = TODO("Not yet implemented")
    override fun peek(): Double = TODO("Not yet implemented")
    override fun add(k: Double) =offer(k)
    override val size:Int get()= kotlin.math.min(tail, maxSize)
}