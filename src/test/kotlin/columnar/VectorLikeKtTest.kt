package columnar

import io.kotlintest.specs.StringSpec

class VectorLikeKtTest : StringSpec() {

    init {
        "combinevec" {
            val c: Vect0r<Int> = combine(
                listOf(0, 1, 2).toVect0r(),
                listOf(3, 4, 5).toVect0r(),
                listOf(6, 7, 8, 9).toVect0r()
            )
            val toList = c.toList()
            System.err.println(toList)
        }
    }

}