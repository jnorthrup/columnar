package columnar

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

class VectorLikeKtTest : StringSpec() {

    init {
        "combinevec" {
            val c: Vect0r<Int> = combine(
                (0..2).toVect0r(),
                (3..5).toVect0r(),
                (6..9).toVect0r()
            )
            val toList = c.toList()
            toList.toString() shouldBe "[0, 1, 2, 3, 4, 5, 6, 7, 8, 9]"
            System.err.println(toList)
        }
        "div"{
            val intRange = 0..11
            fun IntRange.split(nSubRanges: Int) = run {
                val subSize = (last - first +(1- first)) / nSubRanges
                sequence {
                    for (i in this@split step subSize) {
                        yield(i..minOf(last, i + subSize - 1))
                    }
                }
            }
            System.err.println(intRange.toList())
            System.err.println(intRange.last)
            val toList = intRange.split(3).toList()
            val toList1 = (intRange / 3).toList()
            System.err.println(toList to toList1)
            toList shouldBe toList1
        }
        "subranges"{

        }
    }

}