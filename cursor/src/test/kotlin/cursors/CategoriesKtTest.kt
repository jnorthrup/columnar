package cursors

import cursors.context.TokenizedRow
import cursors.effects.head
import cursors.effects.show
import cursors.ml.DummySpec
import org.junit.jupiter.api.Test
import vec.util._l

internal class CategoriesKtTest {

    val curs = TokenizedRow.CsvArraysCursor(

            _l["something,notsomething",
                    "john,0",
                    "john,2",
                    "john,4",
                    "john,8",
                    "john,16",
                    "john,32",
                    "john,64",
                    "john,128",
                    "phillip,256"].map { it.toString() }
    )


    @Test
    fun testCategories() {
        curs.categories().show()
        curs.categories(DummySpec.Last).show()
        curs.categories(1).show()
    }
}


/*    @Test
    fun testCategories2() {
        val ctFun = Scalar(IOMemento.IoString, colname).`⟲`
        val cursor: Cursor = Cursor(cats.size) { iy: Int ->
            Pai2(1) {
                cats[iy] t2 ctFun
            }
        }
        println("original data: ")
        cursor.show()
        println("--- ")

        _l[null, DummySpec.First, DummySpec.Last].forEach { dummyPolicy ->
            println("DummyPolicy $dummyPolicy")
            cursor.categories(dummyPolicy).show()

            println("--- ")
            cursor.categories2(dummyPolicy).show()

        }
    }
}*/
