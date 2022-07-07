package cursors

import cursors.context.TokenizedRow
import cursors.effects.head
import cursors.io.colIdx
import org.junit.Test
import vec.util._l

internal class ByColNameKtTest {

    @Test
    fun testTheNegation() {
        val curs = TokenizedRow.CsvArraysCursor(
            _l[
                    "c1,c2,c3,c4,c5,c6",
                    "1,2,3,4,5,6"
            ]
        )

        curs["c2", "c3"].head()
        curs[-"c2", -"c3"].head()

        val ints = curs.colIdx["c2", "c3"]
        val ints1 = curs.colIdx[-"c2", -"c3"]
        println(ints.toList())
        println(ints1.toList())
        curs[ints + ints1].head()
    }

}