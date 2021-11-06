package cursors

import kotlinx.coroutines.flow.combine
import kotlin.test.*
import vec.macros.*
import vec.util._a
import vec.util._l


infix fun Any?.shouldBe(that: Any?) {
 assertEquals(that, this)
}

class VectorLikeKtTest {
    @Test
    fun testCombineVec() {
        val c: Vect0r<Int> = combine(
            (0..2).toVect0r(),
            (3..5).toVect0r(),
            (6..9).toVect0r()
        )
        val toList = c.toList()
        assertEquals("[0, 1, 2, 3, 4, 5, 6, 7, 8, 9]", toList.toString())
        System.err.println(toList)
    }

    @Test
    fun testDiv() {
        val intRange = 0..11
        fun IntRange.split(nSubRanges: Int) = run {
            val subSize = (last - first + (1 - first)) / nSubRanges
            sequence {
                for (i in this@split step subSize) yield(i..minOf(last, i + subSize - 1))
            }
        }
        System.err.println(intRange.toList())
        System.err.println(intRange.last)
        val toList = intRange.split(3).toList()
        val toList1 = (intRange / 3).toList()
        System.err.println(toList to toList1)
        assertEquals(toList1, toList)
    }
}
