package cursors

import cursors.ml.onehot_mask
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class OneHotTest {

    @Test
    fun testMinimum() {
        Assertions.assertEquals("asdasdasd", "asdasdasd")
    }

    fun testCategories() {
        val colname = "something"
        val cats: List<*> = listOf(
            0,
            2,
            4,
            8,
            16,
            32,
            64,
            128,
            256
        )

        val dummy: Any? = Any()
        onehot_mask(dummy, cats)
        val colnames = cats.map {
            "${colname}_is_$it"
        }
    }


}