package columnar

import columnar.IOMemento.*
import columnar.context.Columnar
import columnar.context.FixedWidth
import columnar.context.NioMMap
import columnar.context.RowMajor
import columnar.context.RowMajor.Companion.fixedWidthOf
import columnar.context.RowMajor.Companion.indexableOf
import org.junit.jupiter.api.Test
import shouldBe

class CursorKtTest/* : StringSpec()*/ {
    private val coords = intArrayOf(
        0, 10,
        10, 84,
        84, 124,
        124, 164
    ).zipWithNext() //α { (a:Int,b:Int) :Pai2<Int,Int> -> Tw1n (a,b)   }

    private val drivers = vect0rOf(
        IoLocalDate as TypeMemento,
        IoString,
        IoFloat,
        IoFloat
    )
    private val names = vect0rOf("date", "channel", "delivered", "ret")
    private val mf = MappedFile("src/test/resources/caven4.fwf")
    private val nio = NioMMap(mf)
    private val fixedWidth: FixedWidth
        get() = fixedWidthOf(nio = nio, coords = coords)
    @Suppress("UNCHECKED_CAST")
    val root = RowMajor().fromFwf(fixedWidth, indexableOf(nio, fixedWidth), nio, Columnar(drivers.zip( names) as Vect02<TypeMemento, String?> ))


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
    fun `resample+join`() {
        val cursor: Cursor = cursorOf(root)


        val resample = cursor.resample(0)
        val join = join(resample[0, 1], resample[2, 3])
        for (i in 0 until resample.first) {
            resample.second(i).left.toList() shouldBe join.second(i).left.toList()
            println(
                resample.second(i).left.toList() to join.second(i).left.toList()
            )
        }


    }

    @Test
    fun whichKey() {
        val fanOut_size = 2
        val lhs_size = 2
        fun whichKey(ix: Int) = (ix - lhs_size) / fanOut_size
        whichKey(702) shouldBe 350
        whichKey(700) shouldBe 349
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
            println(it.map { it ->
                "${it.component1().let {
                    (it as? Vect0r<*>)?.toList() ?: it
                }}"
            }.toList())
        }
        piv.forEach { it ->
            println(it.map { it ->
                "${it.component1().let {
                    (it as? Vect0r<*>)?.toList() ?: it
                }}"
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
            println(it.map { it ->
                "${it.component1().let {
                    (it as? Vect0r<*>)?.toList() ?: it
                }}"
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
        ).group((0)).`∑`(sumReducer[IoFloat]!!)

        piv.forEach { it ->
            println(it.map { it ->
                "${it.component1().let {
                    (it as? Vect0r<*>)?.toList() ?: it
                }}"
            }.toList())
        }
    }

    @Test
    fun `resample+pivot+group+reduce+join`() {
        println("resample+group+reduce+join")
        val cursor: Cursor = cursorOf(root)
        val resample = cursor.resample(0)
        resample.forEach { it ->
            println(it.map { it ->
                "${it.component1().let {
                    (it as? Vect0r<*>)?.toList() ?: it
                }}"
            }.toList())
        }
        println("---")
        val grp = resample.group((1))
        grp.forEach { it ->
            println(it.map { it ->
                "${it.component1().let {
                    (it as? Vect0r<*>)?.toList() ?: it
                }}"
            }.toList())
        }
        println("---")
        val pai2 = grp[2, 3]
        val join: Cursor = join(grp[0, 1], pai2.`∑`(floatSum))
        join.forEach { it ->
            println(it.map { it ->
                "${it.component1().let {
                    (it as? Vect0r<*>)?.toList() ?: it
                }}"
            }.toList())}}}