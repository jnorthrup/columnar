@file:Suppress("UNCHECKED_CAST")

package cursors

import cursors.context.Scalar
import cursors.io.*
import cursors.io.IOMemento.IoFloat
import cursors.io.IOMemento.IoString
import cursors.macros.`∑`
import cursors.macros.join
import org.junit.jupiter.api.Test
import vec.macros.*
import vec.util._a
import vec.util._v
import java.time.LocalDate

data class row(val date: LocalDate, val channel: String, val delivered: Float, val ret: Float)

class HeapCursorTest {
    val coords = _a[
            0, 10,
            10, 84,
            84, 124,
            124, 164
    ].zipWithNext()

    val drivers = _v[
            IOMemento.IoLocalDate,
            IoString,
            IoFloat,
            IoFloat
    ]


    val rows = _v[

            row(LocalDate.of(2017, 10, 13), "0101761/0101010207/13-14/01", 88.0f, 0.0f),
            row(LocalDate.of(2017, 10, 22), "0102211/0101010212/13-14/01", 80.0f, 0.0f),
            row(LocalDate.of(2017, 10, 24), "0500020/0101010106/13-14/05", 4.0f, 0.0f),
            row(LocalDate.of(2017, 10, 22), "0500020/0101010106/13-14/05", 820.0f, 0.0f)
    ]

    val names = _v["date", "channel", "delivered", "ret"]


    val heapCursor: Cursor = Cursor(rows.size) { iy: Int ->
        rows[iy].run {
            RowVec(names.size) { ix: Int ->
                when (ix) {
                    0 -> component1()
                    1 -> component2()
                    2 -> component3()
                    else -> component4()
                } as Any? t2 { Scalar(drivers[ix], names[ix]) }

            }
        }
    }

    @Test
    fun `resample+pivot+group+reduce+join`() {
        println("resample+group+reduce+join")
        val cursor: Cursor = heapCursor
        val resample = cursor.resample(0)
        resample.forEach { it ->
            println(it.map { vec ->
                "${
                    vec.component1().let {
                        (it as? Vect0r<*>)?.toList() ?: it
                    }
                }"
            }.toList())
        }
        println("---")
        val grp = resample.group((1))
        grp.forEach { it ->
            println(it.map { vec ->
                "${
                    vec.component1().let {
                        (it as? Vect0r<*>)?.toList() ?: it
                    }
                }"
            }.toList())
        }
        println("---")
        val pai2 = grp[2, 3]
        val join: Cursor = join(grp[0, 1], pai2.`∑`(floatSum))
        join.forEach { it ->
            println(it.map { vec ->
                "${
                    vec.component1().let {
                        (it as? Vect0r<*>)?.toList() ?: it
                    }
                }"
            }.toList())
        }
    }

}