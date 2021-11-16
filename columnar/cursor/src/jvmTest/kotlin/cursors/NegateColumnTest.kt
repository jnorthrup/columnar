package cursors

import cursors.context.CsvArraysCursor
import cursors.effects.head
import kotlin.test.Test
import vec.macros.size
import vec.util._l
import kotlin.test.assertEquals

internal class NegateColumnTest {
    @Test
    fun testNegate() {
        val csvLines1 =
            CsvArraysCursor(
                _l[
                        "One,Two,Three",
                        "1,2,3",
                        "1,2,3",
                        "1,2,3",
                        "1,2,3",
                        "1,2,3",
                        "1,2,3",
                        "1,2,3",
                        "1,2,3"
                ]
            )
        csvLines1.head()
        csvLines1[-"One"].size shouldBe 8
        csvLines1[-"One", -"Three"].size shouldBe 8
        csvLines1[1].size shouldBe 8
    }


}

  infix fun Any.shouldBe(i: Any?) = assertEquals(this, i)