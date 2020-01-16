package columnar

import columnar.IOMemento.*
import columnar.context.Columnar
import columnar.context.NioMMap
import columnar.context.RowMajor
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

class CursorKtTest : StringSpec() {
    val coords = vZipWithNext(
        intArrayOf(
            0, 10,
            10, 84,
            84, 124,
            124, 164
        )
    )
    val drivers = vect0rOf(
        IoLocalDate,
        IoString,
        IoFloat,
        IoFloat
    )

    init {
        val names = vect0rOf("date", "channel", "delivered", "ret")
        val mf = MappedFile("src/test/resources/caven4.fwf")
        val nio = NioMMap(mf)
        val fixedWidth = fixedWidthOf(nio, coords)
        val root = fromFwf(RowMajor(), fixedWidth, indexableOf(nio, fixedWidth), nio, Columnar(drivers, names))
        "div"{
            val pai21 = (0..2800000) / Runtime.getRuntime().availableProcessors()
            System.err.println(pai21.toList().toString())

        }
        "sum" {
            val cursor: Cursor = cursorOf(root)
            println(cursor.narrow().toList())
            val piv = cursor.group(/*sortedSetOf*/(0))

        }
        "resample" {
            val cursor: Cursor = cursorOf(root)
            val narrow = cursor.narrow()
            cursor.toList()[3][2].first shouldBe 820f
            System.err.println(narrow.toList())
            val toList = cursor.resample(0).narrow().toList()
            toList[3][2] shouldBe 820f
            toList.forEach { System.err.println(it) }
        }
        "resample+join" {
            val cursor: Cursor = cursorOf(root)


            val resample = cursor.resample(0)
            val join = join(resample[0, 1], resample[2, 3])
            for (i in 0 until resample.size) {
                resample.second(i).left.toList() shouldBe join.second(i).left.toList()
                println(
                    resample.second(i).left.toList() to join.second(i).left.toList()

                )
            }


        }

        "whichKey"{
            val fanOut_size = 2
            val lhs_size = 2


            fun whichKey(ix: Int) = (ix - lhs_size) / fanOut_size
            whichKey(702) shouldBe 350
            whichKey(700) shouldBe 349
        }
        "whichValue" {

            val fanOut_size = 2
            val lhs_size = 2

            fun whichValue(ix: Int) = (ix - lhs_size) % fanOut_size
            whichValue(3) shouldBe 1
            whichValue(33) shouldBe 1

            whichValue(3) shouldBe 1
            whichValue(4) shouldBe 0
            whichValue(0) shouldBe 0
        }
        "pivot" {
            val cursor: Cursor = cursorOf(root)
            println(cursor.narrow().toList())
            val piv = cursor.pivot(intArrayOf(0), intArrayOf(1), intArrayOf(2, 3))
            val toArray = piv.scalars.toArray()
            val map = toArray.map { it.second }
            println(map)
            piv.forEach { it: RowVec ->
                val left = it.left.toList()
                println("" + left)

            }
        }
        "group" {

            val cursor: Cursor = cursorOf(root)
            println(cursor.narrow().toList())
            val piv = cursor.group(/*sortedSetOf*/(0))/*.cursor*/
            cursor.forEach {
                println(it.map {
                    "${it.component1().let {
                        (it as? Vect0r<*>)?.toList() ?: it
                    }}"
                }.toList())
            }
            piv.forEach {
                println(it.map {
                    "${it.component1().let {
                        (it as? Vect0r<*>)?.toList() ?: it
                    }}"
                }.toList())
            }

        }
        "pivot+group" {
            System.err.println("pivot+group ")
            val cursor: Cursor = cursorOf(root)
            println("from:\n" + cursor.narrow().toList())
            val piv = cursor.pivot(intArrayOf(0), intArrayOf(1), intArrayOf(2, 3)).group(/*sortedSetOf*/(0))/*.cursor*/
            println()
            piv.forEach {
                println(it.map {
                    "${it.component1().let {
                        (it as? Vect0r<*>)?.toList() ?: it
                    }}"
                }.toList())
            }
        }
        "pivot+group+reduce" {
            System.err.println("pivot+group+reduce")
            val cursor: Cursor = cursorOf(root)
            println(cursor.narrow().toList())
            val piv = cursor.pivot(
                intArrayOf(0),
                intArrayOf(1),
                intArrayOf(2, 3)
            ).group(/*sortedSetOf*/(0))(sumReducer[IoFloat]!!)

            piv.forEach {
                println(it.map {
                    "${it.component1().let {
                        (it as? Vect0r<*>)?.toList() ?: it
                    }}"
                }.toList())
            }

        }
        "resample+pivot+group+reduce+join" {
            println("resample+group+reduce+join")
            val cursor: Cursor = cursorOf(root)
            val resample = cursor.resample(0)
            resample.forEach {
                println(it.map {
                    "${it.component1().let {
                        (it as? Vect0r<*>)?.toList() ?: it
                    }}"
                }.toList())
            }
            println("---")
            val grp = resample.group(1)
            grp.forEach {
                println(it.map {
                    "${it.component1().let {
                        (it as? Vect0r<*>)?.toList() ?: it
                    }}"
                }.toList())
            }

            val any: RowVec = grp.second(3)
            val anyx = grp.second(3).left.toList().toString()
//            fun stringOf(it: RowVec) = it.left.toList().map { (it as? Vect0r<*>)?.toList() ?: it }.toString()
            val value = stringOf(any)
            //value shouldBe  (null) the damn thing works.
            println("---")
            val join = join(grp[0, 1], grp[2, 3](floatSum))
            join.forEach {
                println(it.map {
                    "${it.component1().let {
                        (it as? Vect0r<*>)?.toList() ?: it
                    }}"
                }.toList())
            }

        }

    }
}
