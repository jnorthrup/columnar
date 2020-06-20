package cursors

import cursors.context.TokenizedRow
import cursors.effects.head
import org.junit.jupiter.api.Test
import shouldBe
import vec.macros.size
import vec.util._l

internal class NegateColumnTest {
    @Test
    fun testNegate() {
        val csvLines1 =
                TokenizedRow.CsvArraysCursor(
                        _l[
                                "One,Two,Three",
                                "1,2,3",
                                "1,2,3",
                                "1,2,3",
                                "1,2,3",
                                "1,2,3",
                                "1,2,3",
                                "1,2,3",
                                "1,2,3",
                        ])
        csvLines1.head()
        csvLines1[-"One"].size shouldBe 8
        csvLines1[-"One", -"Three"].size shouldBe 8
        csvLines1[1].size shouldBe 8
    }


}
