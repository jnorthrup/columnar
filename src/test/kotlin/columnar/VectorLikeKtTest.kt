package columnar

import io.kotlintest.matchers.string.shouldBeEqualIgnoringCase
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

class VectorLikeKtTest : StringSpec() {

    init {
        "combinevec" {
            val c: Vect0r<Int> = combine(
              (0..2).toVect0r(),
              (3..5).toVect0r(),
              (6.. 9).toVect0r()
            )
            val toList = c.toList()
            toList.toString() shouldBe "[0, 1, 2, 3, 4, 5, 6, 7, 8, 9]"
            System.err.println(toList)
        }
    }

}