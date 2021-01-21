package org.jsuffixarrays


/**
 * An algorithm that can produce a *suffix array* for a sequence of integer symbols.
 *
 * @see .buildSuffixArray
 */
interface ISuffixArrayBuilder {
    /**
     * Computes suffix array for sequence of symbols (integers). The processed sequence is
     * a subsequence of `input` determined by `start` and
     * `length` parameters.
     *
     *
     * Concrete implementations may have additional requirements and constraints
     * concerning the input. For example, it is quite common that extra cells are required
     * after `start + length` to store special marker symbols. Also, some
     * algorithms may require non-negative symbols in the input. For such constrained
     * algorithms, use various decorators and adapters available in this package.
     *
     * @param input A sequence of input symbols, int-coded.
     * @param start The starting index (inclusive) in `input`.
     * @param length Number of symbols to process.
     * @return An array of indices such that the suffix of `input` at index
     * `result[i]` is lexicographically larger or equal to any other
     * suffix that precede it. Note that the output array may be larger than
     * `input.length`, in which case only the first
     * `input.length` elements are of relevance.
     *
     *
     * The returned array contains suffix indexes starting from 0 (so
     * `start` needs to be added manually to access a given suffix in
     * `input`).
     */
    fun buildSuffixArray(input: IntArray, start: Int, length: Int): IntArray
}