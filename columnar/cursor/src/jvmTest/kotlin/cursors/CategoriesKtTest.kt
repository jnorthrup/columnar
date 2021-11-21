package cursors

import cursors.context.CsvArraysCursor
import cursors.effects.show
import cursors.io.IOMemento
import kotlin.test.Test
import vec.ml.DummySpec
import vec.util._l
import kotlin.test.Ignore

internal class CategoriesKtTest {

    val curs = CsvArraysCursor(

        _l["something,notsomething,cbf",
                "john,0,below me",
                "john,2,below me",
                "john,4,below me",
                "john,8,below me",
                "john,16,below me",
                "john,32,below me",
                "john,64,below me",
                "john,128,below me",
                "phillip,256,below me"].map { it.toString() },
        overrides = linkedMapOf("notsomething" to IOMemento.IoInt)
    )

@Ignore
    @Test
    fun testCategories() {
        val categories = curs.categories()
        categories.show()
        curs.categories(DummySpec.Last).show()
        curs.categories(1).show()
    }
@Ignore
    @Test
    fun testCategories1() {
        curs.categories().asBitSet().show()
        curs.categories(DummySpec.Last).asBitSet().show()
        curs.categories(1).asBitSet().show()
    }
}
