package columnar

import org.junit.jupiter.api.Test
import shouldBe

class OneHotTest {

    @Test
    fun testMinimum() {
        "asdasdasd" shouldBe "asdasdasd"
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