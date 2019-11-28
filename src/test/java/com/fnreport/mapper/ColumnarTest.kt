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
            listOf((0 to 10), (10 to 84), (84 to 124), (124 to 164))
                    .zip(
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
//            val f20 = FixedRecordLengthFile("src/test/resources/caven20.fwf")
//            val f4 = FixedRecordLengthFile("src/test/resources/caven4.fwf")
//            val c20 = columns from f20
//            val c4 = columns from f4
            System.err.println("group")
            val x=suspend{
                val r4 = columns reify f4
                var clusters = r4.group(1)

                clusters.let{(a,b)->val(c, d) = b
                    System.err.println("${a.asList()} ")
                    c.collect {
                        System.err.println(it.contentDeepToString())
                    }
                }
                  clusters = r4.group(0)

                clusters.let{(a,b)->val(c, d) = b
                    System.err.println("${a.asList()} ")
                    c.collect {
                        System.err.println(it.contentDeepToString())
                    }
                }
                clusters = clusters.group( 3)
                clusters.let{(a,b)->val(c, d) = b
                    System.err.println("${a.asList()} ")
                    c.collect {
                        System.err.println(it.contentDeepToString())
                    }
                }

            }
            x()
        }
    }
}