package ports

import kotlin.jvm.Transient
import kotlin.math.max
import kotlin.math.min

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//
open class BitSet {
    private lateinit var words: LongArray

    @Transient
    private var wordsInUse = 0

    @Transient
    private var sizeIsSticky = false
    private fun checkInvariants() {
        assert(wordsInUse == 0 || words[wordsInUse - 1] != 0L)
        assert(wordsInUse >= 0 && wordsInUse <= words.size)
        assert(wordsInUse == words.size || words[wordsInUse] == 0L)
    }

    private fun recalculateWordsInUse() {
        var i: Int
        i = wordsInUse - 1
        while (i >= 0 && words[i] == 0L) {
            --i
        }
        wordsInUse = i + 1
    }

    constructor() {
        initWords(64)
        sizeIsSticky = false
    }

    constructor(nbits: Int) {
        require(nbits >= 0)

        initWords(nbits)
        sizeIsSticky = true

    }

    private fun initWords(nbits: Int) {
        words = LongArray(wordIndex(nbits - 1) + 1)
    }

    private constructor(words: LongArray) {
        this.words = words
        wordsInUse = words.size
        checkInvariants()
    }

    fun toByteArray(): ByteArray {
        val n = wordsInUse
        return if (n == 0) {
            ByteArray(0)
        } else {
            var len = 8 * (n - 1)
            run {
                var x = this.words[n - 1]
                while (x != 0L) {
                    ++len
                    x = x ushr 8
                }
            }
            val bytes = ByteArray(len)
            val wrap: ByteBuffer = ByteBuffer.wrap(bytes)
            val bb: ByteBuffer = wrap.order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until n - 1) {
                bb.putLong(words[i])
            }
            var x = words[n - 1]
            while (x != 0L) {
                bb.put((x and 255L).toInt().toByte())
                x = x ushr 8
            }
            bytes
        }
    }

    fun toLongArray(): LongArray {
        return words.copyOf(wordsInUse)
    }


    private fun ensureCapacity(wordsRequired: Int) {
        if (words.size < wordsRequired) {
            val request: Int = max(2 * words.size, wordsRequired)
            words = words.copyOf(request)
            sizeIsSticky = false
        }
    }

    private fun expandTo(wordIndex: Int) {
        val wordsRequired = wordIndex + 1
        if (wordsInUse < wordsRequired) {
            ensureCapacity(wordsRequired)
            wordsInUse = wordsRequired
        }
    }

    fun flip(bitIndex: Int) {
        if (bitIndex < 0) {
            throw IndexOutOfBoundsException("bitIndex < 0: $bitIndex")
        } else {
            val wordIndex: Int = wordIndex(bitIndex)
            expandTo(wordIndex)
            val var10000 = words
            var10000[wordIndex] = var10000[wordIndex] xor (1L shl bitIndex)
            recalculateWordsInUse()
            checkInvariants()
        }
    }

    fun flip(fromIndex: Int, toIndex: Int) {
        checkRange(fromIndex, toIndex)
        if (fromIndex != toIndex) {
            val startWordIndex: Int = wordIndex(fromIndex)
            val endWordIndex: Int = wordIndex(toIndex - 1)
            expandTo(endWordIndex)
            val firstWordMask = -1L shl fromIndex
            val lastWordMask = -1L ushr -toIndex
            var var10000: LongArray
            if (startWordIndex == endWordIndex) {
                var10000 = words
                var10000[startWordIndex] = var10000[startWordIndex] xor (firstWordMask and lastWordMask)
            } else {
                var10000 = words
                var10000[startWordIndex] = var10000[startWordIndex] xor firstWordMask
                for (i in startWordIndex + 1 until endWordIndex) {
                    var10000 = words
                    var10000[i] = var10000[i].inv()
                }
                var10000 = words
                var10000[endWordIndex] = var10000[endWordIndex] xor lastWordMask
            }
            recalculateWordsInUse()
            checkInvariants()
        }
    }

    fun set(bitIndex: Int) {
        if (bitIndex < 0) {
            throw IndexOutOfBoundsException("bitIndex < 0: $bitIndex")
        } else {
            val wordIndex: Int = wordIndex(bitIndex)
            expandTo(wordIndex)
            val var10000 = words
            var10000[wordIndex] = var10000[wordIndex] or (1L shl bitIndex)
            checkInvariants()
        }
    }

    operator fun set(bitIndex: Int, value: Boolean) {
        if (value) {
            this.set(bitIndex)
        } else {
            this.clear(bitIndex)
        }
    }

    operator fun set(fromIndex: Int, toIndex: Int) {
        checkRange(fromIndex, toIndex)
        if (fromIndex != toIndex) {
            val startWordIndex: Int = wordIndex(fromIndex)
            val endWordIndex: Int = wordIndex(toIndex - 1)
            expandTo(endWordIndex)
            val firstWordMask = -1L shl fromIndex
            val lastWordMask = -1L ushr -toIndex
            var var10000: LongArray
            if (startWordIndex == endWordIndex) {
                var10000 = words
                var10000[startWordIndex] = var10000[startWordIndex] or (firstWordMask and lastWordMask)
            } else {
                var10000 = words
                var10000[startWordIndex] = var10000[startWordIndex] or firstWordMask
                for (i in startWordIndex + 1 until endWordIndex) {
                    words[i] = -1L
                }
                var10000 = words
                var10000[endWordIndex] = var10000[endWordIndex] or lastWordMask
            }
            checkInvariants()
        }
    }

    operator fun set(fromIndex: Int, toIndex: Int, value: Boolean) {
        if (value) {
            this[fromIndex] = toIndex
        } else {
            this.clear(fromIndex, toIndex)
        }
    }

    fun clear(bitIndex: Int) {
        if (bitIndex < 0) {
            throw IndexOutOfBoundsException("bitIndex < 0: $bitIndex")
        } else {
            val wordIndex: Int = wordIndex(bitIndex)
            if (wordIndex < wordsInUse) {
                val var10000 = words
                var10000[wordIndex] = var10000[wordIndex] and (1L shl bitIndex).inv()
                recalculateWordsInUse()
                checkInvariants()
            }
        }
    }

    fun clear(fromIndex: Int, toIndex: Int) {
        var toIndex1 = toIndex
        checkRange(fromIndex, toIndex1)
        if (fromIndex != toIndex1) {
            val startWordIndex: Int = wordIndex(fromIndex)
            if (startWordIndex < wordsInUse) {
                var endWordIndex: Int = wordIndex(toIndex1 - 1)
                if (endWordIndex >= wordsInUse) {
                    toIndex1 = length()
                    endWordIndex = wordsInUse - 1
                }
                val firstWordMask = -1L shl fromIndex
                val lastWordMask = -1L ushr -toIndex1
                var var10000: LongArray
                if (startWordIndex == endWordIndex) {
                    var10000 = words
                    var10000[startWordIndex] = var10000[startWordIndex] and (firstWordMask and lastWordMask).inv()
                } else {
                    var10000 = words
                    var10000[startWordIndex] = var10000[startWordIndex] and firstWordMask.inv()
                    for (i in startWordIndex + 1 until endWordIndex) {
                        words[i] = 0L
                    }
                    var10000 = words
                    var10000[endWordIndex] = var10000[endWordIndex] and lastWordMask.inv()
                }
                recalculateWordsInUse()
                checkInvariants()
            }
        }
    }

    fun clear() {
        while (wordsInUse > 0) {
            words[--wordsInUse] = 0L
        }
    }

    operator fun get(bitIndex: Int): Boolean {
        return if (bitIndex < 0) {
            throw IndexOutOfBoundsException("bitIndex < 0: $bitIndex")
        } else {
            checkInvariants()
            val wordIndex: Int = wordIndex(bitIndex)
            wordIndex < wordsInUse && words[wordIndex] and 1L shl bitIndex != 0L
        }
    }

    operator fun get(fromIndex: Int, toIndex: Int): BitSet {
        var index = toIndex
        checkRange(fromIndex, index)
        checkInvariants()
        val len = length()
        return if (len > fromIndex && fromIndex != index) {
            if (index > len) {
                index = len
            }
            val result: BitSet = BitSet(index - fromIndex)
            val targetWords: Int = wordIndex(index - fromIndex - 1) + 1
            var sourceIndex: Int = wordIndex(fromIndex)
            val wordAligned = fromIndex and 63 == 0
            var i = 0
            while (i < targetWords - 1) {
                result.words[i] =
                    if (wordAligned) words[sourceIndex] else words[sourceIndex] ushr fromIndex or words[sourceIndex + 1] shl -fromIndex
                ++i
                ++sourceIndex
            }
            val lastWordMask = -1L ushr -index
            result.words[targetWords - 1] =
                if (index - 1 and 63 < fromIndex and 63) words[sourceIndex] ushr fromIndex or (words[sourceIndex + 1] and lastWordMask) shl -fromIndex else words[sourceIndex] and lastWordMask ushr fromIndex
            result.wordsInUse = targetWords
            result.recalculateWordsInUse()
            result.checkInvariants()
            result
        } else {
            BitSet(0)
        }
    }

    fun nextSetBit(fromIndex: Int): Int {
        return if (fromIndex < 0) {
            throw IndexOutOfBoundsException("fromIndex < 0: $fromIndex")
        } else {
            checkInvariants()
            var u: Int = wordIndex(fromIndex)
            if (u >= wordsInUse) {
                -1
            } else {
                var word: Long
                word = words[u] and -1L shl fromIndex
                while (word == 0L) {
                    ++u
                    if (u == wordsInUse) {
                        return -1
                    }
                    word = words[u]
                }
                u * 64 + numberOfTrailingZeros(word)
            }
        }
    }

    fun nextClearBit(fromIndex: Int): Int {
        return if (fromIndex < 0) {
            throw IndexOutOfBoundsException("fromIndex < 0: $fromIndex")
        } else {
            checkInvariants()
            var u: Int = wordIndex(fromIndex)
            if (u >= wordsInUse) {
                fromIndex
            } else {
                var word: Long
                word = words[u].inv() and -1L shl fromIndex
                while (word == 0L) {
                    ++u
                    if (u == wordsInUse) {
                        return wordsInUse * 64
                    }
                    word = words[u].inv()
                }
                u * 64 + numberOfTrailingZeros(word)
            }
        }
    }

    fun previousSetBit(fromIndex: Int): Int {
        return if (fromIndex < 0) {
            if (fromIndex == -1) {
                -1
            } else {
                throw IndexOutOfBoundsException("fromIndex < -1: $fromIndex")
            }
        } else {
            checkInvariants()
            var u: Int = wordIndex(fromIndex)
            if (u >= wordsInUse) {
                length() - 1
            } else {
                var word: Long
                word = words[u] and -1L ushr -(fromIndex + 1)
                while (word == 0L) {
                    if (u-- == 0) {
                        return -1
                    }
                    word = words[u]
                }
                (u + 1) * 64 - 1 - numberOfLeadingZeros(word)
            }
        }
    }

    fun previousClearBit(fromIndex: Int): Int {
        return if (fromIndex < 0) {
            if (fromIndex == -1) {
                -1
            } else {
                throw IndexOutOfBoundsException("fromIndex < -1: $fromIndex")
            }
        } else {
            checkInvariants()
            var u: Int = wordIndex(fromIndex)
            if (u >= wordsInUse) {
                fromIndex
            } else {
                var word: Long
                word = words[u].inv() and -1L ushr -(fromIndex + 1)
                while (word == 0L) {
                    if (u-- == 0) {
                        return -1
                    }
                    word = words[u].inv()
                }
                (u + 1) * 64 - 1 - numberOfLeadingZeros(word)
            }
        }
    }

    fun length(): Int {
        return if (wordsInUse == 0) 0 else 64 * (wordsInUse - 1) + (64 - numberOfLeadingZeros(words[wordsInUse - 1]))
    }

    val isEmpty: Boolean
        get() = wordsInUse == 0

    fun intersects(set: BitSet): Boolean {
        for (i in min(wordsInUse, set.wordsInUse) - 1 downTo 0) {
            if (words[i] and set.words[i] != 0L) {
                return true
            }
        }
        return false
    }

    open fun bitCount(i: Int): Int {
        var i1 = i
        i1 -= i1 ushr 1 and 1431655765
        i1 = (i1 and 858993459) + (i1 ushr 2 and 858993459)
        i1 = i1 + (i1 ushr 4) and 252645135
        i1 += i1 ushr 8
        i1 += i1 ushr 16
        return i1 and 63
    }

    open fun bitCount(i: Long): Int {
        var i1 = i
        i1 -= i1 ushr 1 and 6148914691236517205L
        i1 = (i1 and 3689348814741910323L) + (i1 ushr 2 and 3689348814741910323L)
        i1 = i1 + (i1 ushr 4) and 1085102592571150095L
        i1 += i1 ushr 8
        i1 += i1 ushr 16
        i1 += i1 ushr 32
        return i1.toInt() and 127
    }

    fun cardinality(): Int {
        var sum = 0
        for (i in 0 until wordsInUse) {
            sum += bitCount(words[i])
        }
        return sum
    }

    fun and(set: BitSet) {
        if (this !== set) {
            while (wordsInUse > set.wordsInUse) {
                words[--wordsInUse] = 0L
            }
            for (i in 0 until wordsInUse) {
                val var10000 = words
                var10000[i] = var10000[i] and set.words[i]
            }
            recalculateWordsInUse()
            checkInvariants()
        }
    }

    fun or(set: BitSet) {
        if (this !== set) {
            val wordsInCommon: Int = min(wordsInUse, set.wordsInUse)
            if (wordsInUse < set.wordsInUse) {
                ensureCapacity(set.wordsInUse)
                wordsInUse = set.wordsInUse
            }
            for (i in 0 until wordsInCommon) {
                val var10000 = words
                var10000[i] = var10000[i] or set.words[i]
            }
            if (wordsInCommon < set.wordsInUse) {
                set.words.copyInto( words, wordsInCommon, wordsInCommon, wordsInUse - wordsInCommon)
            }
            checkInvariants()
        }
    }


    fun xor(set: BitSet) {
        val wordsInCommon: Int = min(wordsInUse, set.wordsInUse)
        if (wordsInUse < set.wordsInUse) {
            ensureCapacity(set.wordsInUse)
            wordsInUse = set.wordsInUse
        }
        for (i in 0 until wordsInCommon) {
            val var10000 = words
            var10000[i] = var10000[i] xor set.words[i]
        }
        if (wordsInCommon < set.wordsInUse) {
            ///arraycopy(set.words, wordsInCommon, words, wordsInCommon, set.wordsInUse - wordsInCommon)//

            set.words.copyInto(words,wordsInCommon,wordsInCommon,set.wordsInUse - wordsInCommon)
        }
        recalculateWordsInUse()
        checkInvariants()
    }

    fun andNot(set: BitSet) {
        for (i in min(wordsInUse, set.wordsInUse) - 1 downTo 0) {
            val var10000 = words
            var10000[i] = var10000[i] and set.words[i].inv()
        }
        recalculateWordsInUse()
        checkInvariants()
    }

    override fun hashCode(): Int {
        var h = 1234L
        var i = wordsInUse
        while (true) {
            --i
            if (i < 0) {
                return (h shr 32 xor h).toInt()
            }
            h = h xor words[i] * (i + 1).toLong()
        }
    }

    fun size(): Int {
        return words.size * 64
    }

    override fun equals(other: Any?): Boolean {
        return if (other !is BitSet) {
            false
        } else if (this === other) {
            true
        } else {
            val set: BitSet = other
            checkInvariants()
            set.checkInvariants()
            if (wordsInUse != set.wordsInUse) {
                false
            } else {
                for (i in 0 until wordsInUse) {
                    if (words[i] != set.words[i]) {
                        return false
                    }
                }
                true
            }
        }
    }


    private fun trimToSize() {
        if (wordsInUse != words.size) {
            words = LongArray(wordsInUse) { words[it] }
            checkInvariants()
        }
    }

    override fun toString(): String {
        checkInvariants()
        val numBits = if (wordsInUse > 128) cardinality() else wordsInUse * 64
        val b = StringBuilder(6 * numBits + 2)
        b.append('{')
        var i = this.nextSetBit(0)
        if (i != -1) {
            b.append(i)
            while (true) {
                ++i
                if (i < 0 || this.nextSetBit(i).also { i = it } < 0) {
                    break
                }
                val endOfRun = nextClearBit(i)
                while (true) {
                    b.append(", ").append(i)
                    ++i
                    if (i == endOfRun) {
                        break
                    }
                }
            }
        }
        b.append('}')
        return b.toString()
    }

    private fun nextSetBit(fromIndex: Int, toWordIndex: Int): Int {
        var u: Int = wordIndex(fromIndex)
        return if (u > toWordIndex) {
            -1
        } else {
            var word: Long
            word = words[u] and -1L shl fromIndex
            while (word == 0L) {
                ++u
                if (u > toWordIndex) {
                    return -1
                }
                word = words[u]
            }
            u * 64 + numberOfTrailingZeros(word)
        }
    }

    companion object {
        private const val ADDRESS_BITS_PER_WORD = 6
        private const val BITS_PER_WORD = 64
        private const val BIT_INDEX_MASK = 63
        private const val WORD_MASK = -1L
        private fun wordIndex(bitIndex: Int): Int {
            return bitIndex shr 6
        }

        fun valueOf(longs: LongArray): BitSet {
            var n: Int
            n = longs.size
            while (n > 0 && longs[n - 1] == 0L) {
                --n
            }
            return BitSet(longs.copyOf(n))
        }


        private fun checkRange(fromIndex: Int, toIndex: Int) {
            if (fromIndex < 0) {
                throw IndexOutOfBoundsException("fromIndex < 0: $fromIndex")
            } else if (toIndex < 0) {
                throw IndexOutOfBoundsException("toIndex < 0: $toIndex")
            } else if (fromIndex > toIndex) {
                throw IndexOutOfBoundsException("fromIndex: $fromIndex > toIndex: $toIndex")
            }
        }
    }
}

fun numberOfTrailingZeros(i: Long): Int {
    var i1 = i
    if (0L == i1) {
        return 32
    } else {
        var n = 31
        var y = i1 shl 16
        if (y != 0L) {
            n -= 16
            i1 = y
        }
        y = i1 shl 8
        if (y != 0L) {
            n -= 8
            i1 = y
        }
        y = i1 shl 4
        if (y != 0L) {
            n -= 4
            i1 = y
        }
        y = i1 shl 2
        if (y != 0L) {
            n -= 2
            i1 = y
        }
        return (n - (i1 shl 1 ushr 31)).toInt()
    }
}

fun numberOfLeadingZeros(i: Long): Int {
    val x = (i ushr 32).toInt()
    return if (x == 0) 32 + numberOfLeadingZeros(i.toInt()) else numberOfLeadingZeros(x)
}

fun numberOfLeadingZeros(i: Int): Int {
    var i1 = i
    return if (i1 <= 0) {
        if (i1 == 0) 32 else 0
    } else {
        var n = 31
        if (i1 >= 65536) {
            n -= 16
            i1 = i1 ushr 16
        }
        if (i1 >= 256) {
            n -= 8
            i1 = i1 ushr 8
        }
        if (i1 >= 16) {
            n -= 4
            i1 = i1 ushr 4
        }
        if (i1 >= 4) {
            n -= 2
            i1 = i1 ushr 2
        }
        n - (i1 ushr 1)
    }
}