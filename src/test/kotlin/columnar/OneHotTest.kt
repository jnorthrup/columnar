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

    /**
     * Cursors creates one series of values, then optionally omits a column from cats
     */
    fun onehot_mask(dummy: Any?, cats: List<*>) = when {
            dummy is dummy_col ->
                when (dummy) {
                    dummy_col.First -> 0 t2 cats.first()
                    dummy_col.Last -> cats.lastIndex t2 cats.last()
                    dummy_col.None -> TODO()
                }

            dummy != null -> cats.indexOf(dummy) t2 dummy
        else -> null
    }
}

