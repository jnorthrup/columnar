package columnar

import columnar.IOMemento.*
import columnar.context.*
import columnar.context.Arity.Companion.arityKey
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
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun crossTab() {
        val cursor: Cursor = cursorOf(root)
        println("src:" + cursor.narrow().toList())
        val lhs = _a[0]
        val axis = _a[1]

        val rowRed: (Any?, Any?) -> Any? = floatSum
        val colRed: (Any?, Any?) -> Any? = floatSum
        val lhSize = lhs.size
        val axSize = axis.size
        val fanOut = intArrayOf(2, 3)
        val piv = cursor.pivot(lhs, axis, fanOut)


        /**
         * when the pivot above synthesizes columns it generates one block per key and one column per fanout index.
         */
        val blocks: Vect02<Cursor, Cursor> = fanOut.indices.toVect0r() α { fanoutIndex: Int ->
            val rawBlock0 = piv[(lhSize + fanoutIndex) until piv.scalars.size step fanOut.size]

            val bottomRow = bottomReducer(rawBlock0, colRed)
            val rawBlock:Cursor = combine(vect0rOf( rawBlock0, bottomRow))
            val rhs:Cursor =  columnar.rowReducer(rawBlock,rowRed)
            rawBlock t2 rhs
        }
        val v = blocks.left as  Vect0r<Cursor>
        val v1 = blocks.right as Vect0r<Cursor>
        val res = when {
            fanOut.size > 1 -> { // special case, we need a grand total on the totals.

                val v2 = join(v1)


                join(join(v), join (v2, rowReducer(v2,rowRed) ))
            }
            else -> join(v[0], v1[0])
        }



        System.err.println("")
    }

}

/**
 * simplest possible column reducer function with potential IO consequences for large data
 */
fun bottomReducer(rawBlock0:Cursor,reducer:Reducer):Cursor = Cursor(1) { _: Int ->
    RowVec(rawBlock0.scalars.size) { ix: Int ->
        sequence  {
            for (iy in 0 until rawBlock0.size)
                yield(rawBlock0.second(iy)[ix].first )
        }.reduce(reducer)  t2  rawBlock0.second(0)[ix].second //todo: fix the scalar inside this.
    }
}

/**too simple, needs more
 *
 */
fun rowReducer(rawBlock:Cursor,  rowReducer: Reducer):Cursor = Cursor(rawBlock.size) { iy ->
                RowVec(1) {
                    val toList = rawBlock.second(iy).left.toList()
                    toList.reduce(rowReducer ) t2 {
                        val function = rawBlock.second(0).right[0]
                        val oldCtxt = function()
                        val pivotInfo = oldCtxt[PivotInfo.Companion.pivotInfoKey]!!
                        pivotInfo.parentContext()[arityKey] as Scalar
                    }
                }
            }