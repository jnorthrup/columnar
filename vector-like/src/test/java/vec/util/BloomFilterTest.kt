package vec.util

import kotlin.test.*
import java.lang.management.ManagementFactory
import java.lang.management.ThreadMXBean
import java.util.*
import kotlin.math.exp
import kotlin.math.ln


class TestBloomFilter {
    var elements: Int = 1000000
    var bitsize: Int = 10000000
    var filter: BloomFilter
    var prng: Random
    var bean: ThreadMXBean

    @Test
    fun testCorrectness() {
        println(
            """
    Testing correctness.
    Creating a Set and filling it together with our filter...
    """.trimIndent()
        )
        filter.clear()
        val inside: MutableSet<Int> = HashSet((elements / 0.75).toInt())
        while (inside.size < elements) {
            val v = prng.nextInt()
            inside.add(v)
            filter.add(v)
            assert(filter.contains(v)) { "There should be no false negative" }
        }

        // testing
        var found = 0
        var total = 0
        var rate = 0.0
        while (total < elements) {
            val v = prng.nextInt()
            if (inside.contains(v)) continue
            total++
            found += if (filter.contains(v)) 1 else 0
            rate = found.toFloat() / total.toDouble()
            if (total % 1000 == 0 || total == elements) {
                System.out.format(
                    "\rElements incorrectly found to be inside: %8d/%-8d (%3.2f%%)",
                    found, total, 100 * rate
                )
            }
        }
        println("\n")
        val ln2 = ln(2.0)
        val expectedRate = exp(-ln2 * ln2 * bitsize / elements)
        assert(rate <= expectedRate * 1.10) { "error rate p = e^(-ln2^2*m/n)" }
    }

    @Test

    fun testInsertion() {
        println("Testing insertion speed...")
        filter.clear()
        val start = bean.currentThreadCpuTime
        for (i in 0 until elements) filter.add(prng.nextInt())
        val end = bean.currentThreadCpuTime
        val time = end - start
        System.out.format(
            """
                    Inserted %d elements in %d ns.
                    Insertion speed: %g elements/second
                    
                    
                    """.trimIndent(),
            elements,
            time,
            elements / (time * 1e-9)
        )
    }

    @Test

    fun testQuery() {
        println("Testing query speed...")
        filter.clear()
        for (i in 0 until elements) filter.add(prng.nextInt())
        var xor = true // Make sure our result isn’t optimized out
        val start = bean.currentThreadCpuTime
        for (i in 0 until elements) xor = xor xor filter.contains(prng.nextInt())
        val end = bean.currentThreadCpuTime
        val time = end - start
        System.out.format(
            """
                    Queried %d elements in %d ns.
                    Query speed: %g elements/second
                    
                    
                    """.trimIndent(),
            elements,
            time,
            elements / (time * 1e-9)
        )
    }

    @Test
    @Throws(CloneNotSupportedException::class)
    fun testMerge() {
        print("Testing merge... ")
        filter.clear()
        val filter2 = filter.clone()
        for (i in 0 until elements) {
            var a: Int
            var b: Int
            filter.add(prng.nextInt().also { a = it })
            filter2.add(prng.nextInt().also { b = it })
            val concat = filter.clone()
            concat.merge(filter2)
            assert(concat.contains(a) && concat.contains(b)) { "merged filters don't lose elements" }
        }
        val concat1 = filter.clone()
        concat1.merge(filter2)
        val concat2 = filter2.clone()
        concat2.merge(filter)
        assert(concat1.equals(concat2)) { "a.merge(b) = b.merge(a)" }
        println("Done.\n")
    }

    /*
        companion object {
            
            fun main(args: Array<String>) {
                val test = TestBloomFilter ()
                if (args.size >= 1) test.elements = args[0].toInt()
                if (args.size >= 2) test.bitsize = args[1].toInt()
                test.testCorrectness()
                test.testInsertion()
                test.testQuery()
            }
        }

    */
    init {
        System.out.format(
            """
                    Testing a bloom filter containing n=%d elements in a bit array of m=%d bits (=%.1fMib) 
                    
                    
                    """.trimIndent(),
            elements, bitsize, bitsize.toFloat() / (1024 * 1024 * 8)
        )
        bean = ManagementFactory.getThreadMXBean()
        prng = Random()
        prng.setSeed(0)
        filter = BloomFilter(elements, bitsize)
    }
}
