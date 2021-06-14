@file:Suppress("UNCHECKED_CAST")

package cursors

import cursors.context.Columnar
import cursors.context.FixedWidth
import cursors.context.NioMMap
import cursors.context.RowMajor
import cursors.context.RowMajor.Companion.fixedWidthOf
import cursors.io.*
import cursors.io.IOMemento.IoFloat
import cursors.io.IOMemento.IoString
import cursors.io.Vect02_.Companion.left
import cursors.io.Vect02_.Companion.right
import cursors.macros.`∑`
import cursors.macros.join
import cursors.ml.DummySpec
import org.junit.Assert
import org.junit.Test
import shouldBe
import vec.macros.*
import vec.util._a
import vec.util._v


class CursorKtTest {
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

    val names = _v["date", "channel", "delivered", "ret"]
    val mf = MappedFile("src/test/resources/caven4.fwf")
    val nio = NioMMap(mf)
    val fixedWidth: FixedWidth
        get() = fixedWidthOf(nio = nio, coords = coords)

    @Suppress("UNCHECKED_CAST")
    val root = RowMajor().fromFwf(
        fixedWidth,
        RowMajor.indexableOf(nio, fixedWidth),
        nio,
        Columnar(drivers.zip(names) as Vect02<TypeMemento, String?>)
    )


    @Test
    fun div() {
        val pai21 = (0..2800000) / Runtime.getRuntime().availableProcessors()
        System.err.println(pai21.toList().toString())

    }

    @Test
    fun resample() {
        val cursor: Cursor = cursorOf(root)
        val narrow = cursor.narrow()
        cursor.toList()[3][2].first shouldBe 820f
        System.err.println(narrow.toList())
        val toList = cursor.resample(0).narrow().toList()
        toList[3][2] shouldBe 820f
        toList.forEach { System.err.println(it) }
    }

    @Test
    fun oneHot() {
        val cursor: Cursor = cursorOf(root)
        var categories = cursor[0].categories()
        var scalars = categories.scalars as Vect02<TypeMemento, String?>
        System.err.println(scalars.right.toList())
        var toList = categories.narrow().toList()
        toList.forEach { System.err.println(it) }
        categories = cursor[0].categories(DummySpec.Last)
        scalars = categories.scalars as Vect02<TypeMemento, String?>
        System.err.println(scalars.right.toList())
        toList = categories.narrow().toList()
        toList.forEach { System.err.println(it) }
    }

    @Test
    fun `resample+ordered`() {
        val cursor: Cursor = cursorOf(root)
        run {
            System.err.println("unordered\n\n")

            val resample = cursor.resample(0)
            val toList = resample
                /*.ordered(intArrayOf(0), Comparator { o1, o2 -> o1.toString().compareTo(o2.toString()) })*/.narrow()
                .toList()
            resample.toList()[3][2].first shouldBe 820f
            toList.forEach { System.err.println(it) }
        }
        System.err.println("ordered\n\n")
        run {
            val ordered = cursor.resample(0)
                .ordered(intArrayOf(0)/*, Comparator { o1, o2 -> o1.toString().compareTo(o2.toString()) }*/)

            val toList = ordered.narrow()
                .toList()

            toList.forEach { System.err.println(it) }
            ordered.toList()[9][1].first shouldBe "0102211/0101010212/13-14/01"
            ordered.toList()[10][1].first shouldBe "0500020/0101010106/13-14/05"
        }
    }

    @Test
    fun `resample+join`() {
        val cursor: Cursor = cursorOf(root)


        val resample = cursor.resample(0)
        val join = join(_v[resample[0, 1], resample[2, 3]])
        for (i in 0 until resample.first) {
            (resample at (i)).left.toList() shouldBe (join at (i)).left.toList()
            println(
                (resample at (i)).left.toList() to (join at (i)).left.toList()
            )
        }


    }

    @Test
    fun whichKey() {
        val fanOut_size = 2
        val lhs_size = 2
        fun whichKey(ix: Int) = (ix - lhs_size) / fanOut_size
        whichKey(702) shouldBe 350
        Assert.assertEquals(349, whichKey(700))
    }

    @Test
    fun whichValue() {

        val fanOut_size = 2
        val lhs_size = 2

        fun whichValue(ix: Int) = (ix - lhs_size) % fanOut_size
        whichValue(3) shouldBe 1
        whichValue(33) shouldBe 1

        whichValue(3) shouldBe 1
        whichValue(4) shouldBe 0
        whichValue(0) shouldBe 0
    }

    @Test
    fun pivot() {
        val cursor: Cursor = cursorOf(root)
        println(cursor.narrow().toList())
        val piv = cursor.pivot(intArrayOf(0), intArrayOf(1), intArrayOf(2, 3))
        val toArray = piv.scalars.toArray()
        val map = toArray.map { it.second }
        println(map)
        piv.forEach {
            val left = it.left.toList()
            println("" + left)

        }
    }

    @Test
    fun group() {

        val cursor: Cursor = cursorOf(root)
        println(cursor.narrow().toList())
        val piv = cursor.group((0))
        cursor.forEach { it ->
            println(it.map { pai2 ->
                "${
                    pai2.component1().let {
                        (it as? Vect0r<*>)?.toList() ?: it
                    }
                }"
            }.toList())
        }
        piv.forEach { it ->
            println(it.map { pai2 ->
                "${
                    pai2.component1().let {
                        (it as? Vect0r<*>)?.toList() ?: it
                    }
                }"
            }.toList())
        }
    }

    @Test
    fun `pivot+group`() {
        System.err.println("pivot+group ")
        val cursor: Cursor = cursorOf(root)
        println("from:\n" + cursor.narrow().toList())
        val piv = cursor.pivot(intArrayOf(0), intArrayOf(1), intArrayOf(2, 3)).group((0))
        println()
        piv.forEach { it ->
            println(it.map { vec ->
                "${
                    vec.component1().let {
                        (it as? Vect0r<*>)?.toList() ?: it
                    }
                }"
            }.toList())
        }
    }

    @Test
    fun `pivot+group+reduce`() {
        System.err.println("pivot+group+reduce")
        val cursor: Cursor = cursorOf(root)
        println(cursor.narrow().toList())
        val piv = cursor.pivot(
            intArrayOf(0),
            intArrayOf(1),
            intArrayOf(2, 3)
        ).group((0)).`∑`(sumReducer[IOMemento.IoFloat]!!)

        piv.forEach { it ->
            println(it.map { vec ->
                "${
                    vec.component1().let {
                        (it as? Vect0r<*>)?.toList() ?: it
                    }
                }"
            }.toList())
        }
    }

    @Test
    fun `resample+pivot+group+reduce+join`() {
        println("resample+group+reduce+join")
        val cursor: Cursor = cursorOf(root)
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
        val join: Cursor = join(_v[grp[0, 1], pai2.`∑`(floatSum)])
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
