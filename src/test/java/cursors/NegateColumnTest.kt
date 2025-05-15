package cursors

import cursors.context.TokenizedRow
import cursors.effects.head
import vec.macros.reverse
import kotlin.test.*
import vec.macros.size
import vec.util._l

internal class NegateColumnTest {
    @Test
    fun testNegate() {
        val csvLines11 = _l[
                "One,Two,Three",
                "7,8,9",
                "1,2,3",
                "1,2,3",
                "1,2,3",
                "1,2,3",
                "1,2,3",
                "1,2,3",
                "7,6,5"
        ]
        val csvLines1 =
            TokenizedRow.CsvArraysCursor(csvLines11)
        csvLines1.head()
        csvLines1.reverse.head()
        csvLines1[-"One"].size shouldBe 8
        csvLines1[-"One", -"Three"].size shouldBe 8
        csvLines1[1].size shouldBe 8
    }


}
