/**
 *
 * credit to lovasoa https://github.com/lovasoa/bloomfilter/blob/master/src/main/java/BloomFilter.java
package com.github.lovasoa.bloomfilter
 */

package vec.util

import java.lang.Math.*
import java.util.*
import kotlin.math.log
import kotlin.math.log2

/**
 *  bloom filter.
 * @param n Expected number of elements
 * @param m Desired size of the container in bits
 */

public class BloomFilter(n: Int, m: Int =n*11  /* good for 31 bit ints*/  ): Cloneable {
    private var k // Number of hash functions
            : Int = round(LN2 * m / n).toInt().let { if (it <= 0) 1 else it }
    private val hashes: BitSet = BitSet(m).also { logDebug {  "bloomfilter for $n using $m bits"} }
    private val prng: RandomInRange = RandomInRange(m, k)


    /**
     * Add an element to the container
     */
    fun add(o: Any) {
        prng.init(o)
        for (r in prng) hashes.set(r.value)
    }

    /**
     * If the element is in the container, returns true.
     * If the element is not in the container, returns true with a probability ≈ e^(-ln(2)² * m/n), otherwise false.
     * So, when m is large enough, the return value can be interpreted as:
     * - true  : the element is probably in the container
     * - false : the element is definitely not in the container
     */
    operator fun contains(o: Any): Boolean {
        prng.init(o)
        for (r in prng) if (!hashes[r.value]) return false
        return true
    }

    /**
     * Removes all of the elements from this filter.
     */
    fun clear() {
        hashes.clear()
    }

    /**
     * Create a copy of the current filter
     */
    @Throws(CloneNotSupportedException::class)
    public override fun clone(): BloomFilter = super.clone() as BloomFilter

    /**
     * Generate a unique hash representing the filter
     */
    override fun hashCode(): Int = hashes.hashCode() xor k

    /**
     * Test if the filters have equal bitsets.
     * WARNING: two filters may contain the same elements, but not be equal
     * (if the filters have different size for example).
     */
    fun equals(other: BloomFilter): Boolean = hashes == other.hashes && k == other.k

    /**
     * Merge another bloom filter into the current one.
     * After this operation, the current bloom filter contains all elements in
     * other.
     */
    fun merge(other: BloomFilter) {
        require(!(other.k != k || other.hashes.size() != hashes.size())) { "Incompatible bloom filters" }
        hashes.or(other.hashes)
    }

    inner class RandomInRange internal constructor(
// Maximum value returned + 1
            private val max: Int, // Number of random elements to generate
            private val count: Int, val seed: Long = Runtime.getRuntime().freeMemory(),
    ) : Iterable<RandomInRange>, MutableIterator<RandomInRange> {
        private val prng: Random = Random(seed).also {
            logDebug { "using randomSeeed $seed" }
        }
        private var i = 0 // Number of elements generated
        var value // The current value
                = 0

        fun init(o: Any) {
            prng.setSeed(o.hashCode().toLong())
        }

        override fun iterator(): Iterator<RandomInRange> {
            i = 0
            return this
        }

        override fun next(): RandomInRange {
            i++
            value = prng.nextInt() % max
            if (value < 0) value = -value
            return this
        }

        override fun hasNext(): Boolean = i < count

        override fun remove() {
            throw UnsupportedOperationException()
        }


    }

    companion object {
        private const val LN2 = 0.6931471805599453 // ln(2)
    }

}