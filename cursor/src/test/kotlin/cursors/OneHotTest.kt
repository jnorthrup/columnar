package cursors

import cursors.context.Scalar
import cursors.effects.show
import cursors.io.*
import cursors.ml.DummySpec
import cursors.ml.onehot_mask
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import vec.macros.*
import vec.util._l

class OneHotTest {
    val colname = "something"
    val cats = listOf(
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

    @Test
    fun testMinimum() {
        Assertions.assertEquals("asdasdasd", "asdasdasd")
    }

    @Test
    fun testCategories() {
        val dummy: Any? = Any()
        val (i, any) = onehot_mask(dummy, cats)
        val colnames = cats.map {
            "${colname}_is_$it"
        }
        println(any to colnames)

    }


    @Test
    fun testCategories2() {
        val ctFun = Scalar(IOMemento.IoString, colname).`⟲`
        val cursor: Cursor = Cursor(cats.size) { iy: Int ->
            RowVec(1) {
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
        }
    }

}