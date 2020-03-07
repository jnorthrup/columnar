package columnar

import columnar.IOMemento.*
import columnar.context.Columnar
import columnar.context.FixedWidth
import columnar.context.NioMMap
import columnar.context.RowMajor
import columnar.context.RowMajor.Companion.fixedWidthOf
import columnar.context.RowMajor.Companion.indexableOf
import org.junit.jupiter.api.Test

class CrossTabTest/* : StringSpec()*/ {
    val coords = intArrayOf(
        0, 10,
        10, 84,
        84, 124,
        124, 164
    ).zipWithNext() //α { (a:Int,b:Int) :Pai2<Int,Int> -> Tw1n (a,b)   }

    val drivers = vect0rOf(
        IoLocalDate as TypeMemento,
        IoString,
        IoFloat,
        IoFloat
    )

    val names = vect0rOf("date", "channel", "delivered", "ret")
    val mf = MappedFile("src/test/resources/caven4.fwf")
    val nio = NioMMap(mf)
    val fixedWidth: FixedWidth
        get() = fixedWidthOf(nio = nio, coords = coords)

    @Suppress("UNCHECKED_CAST")
    val root = RowMajor().fromFwf(
        fixedWidth,
        indexableOf(nio, fixedWidth),
        nio,
        Columnar(drivers.zip(names) as Vect02<TypeMemento, String?>)
    )


    /**
     *
    |  product                     | 2017-10-13:sold| ... | 2017-10-13:returned | ... | total:Sum| total:return
    |  ---                         | ---            | --- | ---                 | --- | ---      | ---
    |  0101761/0101010207/13-14/01 | 88             | ... | 0                   | ... | 88       | 0
    |  0102211/0101010212/13-14/01 | null           | ... | null                | ... | 80       | 0
    |  0500020/0101010106/13-14/05 | null           | ... | null                | ... | 824      | 0
    |  Total Result	               | 88             | ... | 0                   | ... | 992      | 0

     */
    @Test
    fun crossTab() {
        val cursor: Cursor = cursorOf(root)
        println(cursor.narrow().toList())
        val lhs = _a[0]
        val axis = _a[1]
        val dataColumns = _a[2, 3]

        val piv = cursor.pivot(lhs, axis, dataColumns)
        Cursor(piv.first + 1) { iy: Int ->
            RowVec(piv.scalars.size + dataColumns.size) { ix: Int ->
                /*   single column groupby per column*/if (iy < piv.size) {

                if (ix < piv.size)/* return  piv (y,x) */ piv.second(iy)[ix]
                else
                /**
                 * use the magic of radix to find the indexes of the relevant collection
                 */;
                /*
                             * column+one "Total Result" per DataColumn
                             */
            } else {
                if (ix < piv.size)/* return  totals of x */
                else {
                    //return totals of the totals.
                }
                /**
                 * use the magic of radix to find the indexes of the relevant collection
                 */
                //                totals row.
            }
            }

        }
        val toArray = piv.scalars.toArray()
        val map = toArray.map { it.second }
        println(map)
        piv.forEach {
            val left = it.left.toList()
            println("" + left)
        }
    }
}