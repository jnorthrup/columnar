import org.jsuffixarrays.*
import kotlin.random.Random
import kotlin.test.*

/**
 * A set of shared tests and preconditions that all implementations of
 * [ISuffixArrayBuilder] should meet.
 */
abstract class JvmSuffixArrayBuilderTestBase {
    lateinit var  builder: ISuffixArrayBuilder

    var smallAlphabet: MinMax = MinMax(-5, 5)
    var largeAlphabet: MinMax = MinMax(-500, 500)

    /**
     * Subclasses must override and return a valid instance of [ISuffixArrayBuilder]
     * .
     */
    protected abstract val instance: ISuffixArrayBuilder

    @BeforeTest
    fun prepareBuilder() {
        builder = instance
    }

    /**
     * Check the suffixes of `banana`.
     */
    @Test
    fun checkBanana() {
        assertSuffixes("banana", "a", "ana", "anana", "banana", "na", "nana")
    }

    /**
     * Check the suffixes of `mississippi`.
     */
    @Test
    fun checkMississippi() {
        assertSuffixes("mississippi", "i", "ippi", "issippi", "ississippi",
                "mississippi", "pi", "ppi", "sippi", "sissippi", "ssippi", "ssissippi")
    }

    /**
     * Create a suffix array for the same input sequence, but placed at different offsets
     * in the input array. The result should be identical (not counting the offset of
     * course).
     *
     *
     * Checks the LCP array created for the given input as well.
     */
    @Test
    open fun sameResultWithArraySlice() {
        val builder = instance

        val sliceSize = 500
        val totalSize = 1000
        val extraSpace = SuffixArrays.MAX_EXTRA_TRAILING_SPACE

        val rnd = Random(0x11223344)
        val alphabet = MinMax(1, 50)

        val slice = generateRandom(rnd, sliceSize, alphabet)
        val total = generateRandom(rnd, totalSize + extraSpace, alphabet)

        var prevSuffixArray: IntArray? = null
        var prevLCP: IntArray? = null
        for (i in 0 until totalSize - slice.size) {
            val input = total.copyOf()
            arraycopy(slice, 0, input, i, slice.size)

            val clone = input.clone()
            val sa = builder.buildSuffixArray(input, i, slice.size)
            val lcp = SuffixArrays.computeLCP(input, i, slice.size, sa)

            assertEquals(clone.toList(), input.toList())
            if (prevSuffixArray != null) {

                assertEquals(prevSuffixArray.toList(), sa.toList())
            }
            prevSuffixArray = sa

            // Compare LCPs
            if (prevLCP != null) {
                 assertEquals(prevLCP.toList(), lcp.toList())
            }
            prevLCP = lcp
        }
    }

    /**
     * Create suffix arrays for a few random sequences of integers, verify invariants
     * (every suffix array is a permutation of indices, every suffix in the suffix array
     * is lexicographically greater or equal than all its predecessors).
     */
    @Test
    fun invariantsOnRandomSmallAlphabet() {
        val builder = instance

        // Use constant seed so that we can repeat tests.
        val rnd = Random(0x11223344)
        val inputSize = 1000
        val repeats = 500

        runRandom(builder, rnd, inputSize, repeats, smallAlphabet)
    }

    /**
     * @see .invariantsOnRandomSmallAlphabet
     */
    @Test
    open fun invariantsOnRandomLargeAlphabet() {
        val builder = instance

        // Use constant seed so that we can repeat tests.
        val rnd = Random(0x11223344)
        val inputSize = 1000
        val repeats = 500

        runRandom(builder, rnd, inputSize, repeats, largeAlphabet)
    }

    /*
     * Run invariant checks on randomly generated data.
     */
    private fun runRandom(builder: ISuffixArrayBuilder, rnd: Random,
                          inputSize: Int, repeats: Int, alphabet: MinMax) {
        val extraSpace = SuffixArrays.MAX_EXTRA_TRAILING_SPACE
        for (i in 0 until repeats) {
            val input = generateRandom(rnd, inputSize + extraSpace, alphabet)
            val copy = input.clone()

            val start = 0
            val SA = builder.buildSuffixArray(input, start, inputSize)

            assertEquals( input.toList(),  copy.toList())
            assertPermutation(SA, inputSize)
            assertSorted(SA, input, inputSize)
        }
    }

    /*
     * Verify that two suffixes are less or equal.
     */
    private fun sleq(s1: IntArray, s1i: Int, s2: IntArray, s2i: Int, n: Int): Boolean {
        var s1i = s1i
        var s2i = s2i
        do {
            if (s1[s1i] < s2[s2i]) return true
            if (s1[s1i] > s2[s2i]) return false
            s2i++
            s1i++

            if (s1i == n) return true
        } while (true)
    }

    /*
     * Make sure suffixes in a suffix array are sorted.
     */
    private fun assertSorted(SA: IntArray, s: IntArray, n: Int) {
        for (i in 0 until n - 1) {
            if (!sleq(s, SA[i], s, SA[i + 1], n)) {
                 fail("Suffix " + SA[i] + ">" + SA[i + 1] + ":\n" + SA[i] + ">"
                        + s.slice(SA[i]until  n) + "\n" + SA[i + 1]
                        + ">" + s.slice (SA[i + 1].. n) + "\n" + "a>"
                        + s.asList( ))
            }
        }
    }

    /*
     * Assert a suffix array is a permutation of indices.
     */
    private fun assertPermutation(SA: IntArray, length: Int) {
        val seen = BooleanArray(length)
        for (i in 0 until length) {
             assertFalse(seen[SA[i]])
            seen[SA[i]] = true
        }
        for (i in 0 until length) {
             assertTrue(seen[i])
        }
    }

    /*
     * Assert a suffix array built for a given input contains exactly the given set of
     * suffixes, in that order.
     */
    private fun assertSuffixes(input: CharSequence, vararg expectedSuffixes: CharSequence) {
        val suffixes = SuffixArrays.create(input, instance)
         assertEquals(expectedSuffixes.asList( ), SuffixArrays.toString(input,
                suffixes))
    }

    companion object {

        /*
     * Generate random data.
     */
        fun generateRandom(rnd: Random, size: Int, alphabet: MinMax): IntArray {
            val input = IntArray(size)
            fillRandom(rnd, input, size, alphabet)
            return input
        }

        /*
     * Fill an array with random symbols from the given alphabet/
     */
        fun fillRandom(rnd: Random, input: IntArray, size: Int, alphabet: MinMax) {
            for (j in input.indices) {
                input[j] = rnd.nextInt(alphabet.range() + 1) + alphabet.min
            }
        }
    }
}

private fun IntArray.clone(): IntArray =copyOf()
/**
 * Tests for [DivSufSort].
 */
class JvmDivSufSortTest : SuffixArrayBuilderTestBase() {
    private val alphabetSize = 256

    /*
     *
     */
    override val instance: ISuffixArrayBuilder
        get() = DivSufSort(alphabetSize)

    @BeforeTest
    fun setupForConstraints() {
        smallAlphabet=(MinMax(1, 10))
        largeAlphabet=(MinMax(1, alphabetSize - 1))
    }
}
