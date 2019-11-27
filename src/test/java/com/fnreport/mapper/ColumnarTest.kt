package com.fnreport.mapper

import io.kotlintest.TestCase
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.util.*

fun Array<*>.equalsArray(other: Array<*>) = Arrays.equals(this, other)
fun Array<*>.deepEqualsArray(other: Array<*>) = Arrays.deepEquals(this, other)

@UseExperimental(InternalCoroutinesApi::class)
class ColumnarTest : StringSpec() {
    val columns: List<Pair<String, Pair<Pair<Int, Int>, (Any?) -> Any?>>> = listOf("date", "channel", "delivered", "ret").zip(
            listOf((0 to 10), (10 to 84), (84 to 124), (124 to 164)).zip(
                    listOf(dateMapper,
                            stringMapper,
                            floatMapper,
                            floatMapper)))
    val f20 = FixedRecordLengthFile("src/test/resources/caven20.fwf")
    val f4 = FixedRecordLengthFile("src/test/resources/caven4.fwf")

    val c20 = columns from f20
    val c4 = columns from f4
    val c4remap = c4[0, 0, 0, 1, 3, 2, 1, 1, 1]


    override fun beforeTest(testCase: TestCase) {
    }

    init {
        "dateCol"{
            val any = c20(1)[0].first()
            any.toString().shouldBe("2017-10-22")
            System.err.println(any)
        }
        "size" {
            f4.size.shouldBe(4)

        }
        "remap"{
            val c41 = c4(1).map { it.first() }
            System.err.println(c41)
            val map = c4remap(1).map { it.first() }
            System.err.println(map)
        }
        "reify"{
            val suspendFunction1 = columns from f4
            val stage = f4.take(f4.size)
            val r4 = columns reify f4
            val x = suspend {
                System.err.println("reify")
                val (a, b) = r4
                val (c, d) = b
                c.collect {
                    System.err.println(it.asList())

                }
                c.collect {
                    System.err.println(it.asList())
                }
            }
            x()

        }

        "pivot" {
            System.err.println("pivot")
            val x = suspend {

                val reify = columns reify f4
                val p4 = reify.pivot(intArrayOf(0), intArrayOf(1), 2, 3)
                p4.let { (col, data) ->
                    System.err.println(col.map { it })
                    data.let { (rows, size) ->
                        rows.collect { arr ->
                            System.err.println(arr.toList())
                        }
                    }
                }
            }
            x()
        }

        "group" {

            val f20 = FixedRecordLengthFile("src/test/resources/caven20.fwf")
            val f4 = FixedRecordLengthFile("src/test/resources/caven4.fwf")

            val c20 = columns from f20
            val c4 = columns from f4
            System.err.println("group")
            val x = suspend {

                val r4 = columns reify f4
                val clusters = r4.clusters(0)

                System.err.println("$clusters")
                val index = r4.index(0)
                System.err.println("${index.asList()}")
//
//                val clusters1 = r4.clusters(1)
//                System.err.println("$clusters1")


            }
            x()
        }

    }
}

suspend fun Table2.index(vararg by: Int) = clusters(*by).let { clust ->
    this.let { (colnames, data) ->
        data.let { (rows, size) ->

            /**
             * use elevator to stuff flows into lists.
             */
            val elevator = IntArray(size)

            clust.values.mapIndexed { index, list ->

                rows.transform {


                }
            }
        }
    }
}


/**
 * cost of one full tablscan
 */
suspend fun Table2.clusters(vararg by: Int): Map<Int, List<Int>> =
        this.get(*by).let { (keynames, keyData) ->
            keyData.let { (keyRows, keyMaxY) ->
                mutableMapOf<Int, MutableList<Int>>().also { clusters ->
                    keyRows.collectIndexed { index, value ->
                        val hashCode = value.contentDeepHashCode()
                        when {
                            clusters.containsKey(hashCode) -> clusters[hashCode]!! += (index)
                            else -> clusters[hashCode] = mutableListOf(index)
                        }
                    }
                }
            }
        }

                

                