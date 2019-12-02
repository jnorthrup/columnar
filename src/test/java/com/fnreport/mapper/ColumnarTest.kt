package com.fnreport.mapper

import io.kotlintest.TestCase
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.util.*

@ExperimentalCoroutinesApi
@UseExperimental(InternalCoroutinesApi::class)
class ColumnarTest : StringSpec() {
    val columns: RowDecoder = listOf("date", "channel", "delivered", "ret").zip(
            listOf((0 to 10), (10 to 84), (84 to 124), (124 to 164))
                    .zip(
                            listOf(dateMapper,
                                    stringMapper,
                                    floatMapper,
                                    floatMapper))).toTypedArray()
    val f20 = FixedRecordLengthFile("src/test/resources/caven20.fwf")
    val f4 = FixedRecordLengthFile("src/test/resources/caven4.fwf")

    val c20: Table1 = columns.from(f20)
    val c4: Table1 = columns from f4


    override fun beforeTest(testCase: TestCase) {
    }

    init {
        "dateCol"{
            val any = c20(1)[0].first()
            any.toString() shouldBe "2017-10-22"
            println(any)
        }
        "size" {
            f4.size shouldBe 4

        }
        "remap"{
            val c41: List<Any?> = c4(1).map { it.first() }
            println(c41)
            c41.last() shouldBe 0.0

            val c4remap: Table1 /* suspend (Int) -> Array<Flow<Any?>>*/ = c4[0, 0, 0, 1, 3, 2, 1, 1, 1]
            val firstrow: Array<Flow<Any?>> = c4remap(0)
            val reorderedRow: List<Any?> = firstrow.map { flow ->
                flow.first()
            }
            println(reorderedRow)
            reorderedRow.last() shouldBe reorderedRow[3]


        }
        "reify"{

            val r4: DecodedRows = columns reify f4
            val x = suspend {
                println("reify")
                val (_: Array<String>, data: Pair<Flow<Array<Any?>>, Int>) = r4
                val (rows, _) = data
                rows.collect<Array<Any?>> {
                    println(it.asList())
                }
                rows.count() shouldBe 4
            }
            x()
        }
        "pivot" {
            println("pivot")
            val x = suspend {
                val r4: DecodedRows = columns reify f4
                val p4: DecodedRows = (r4).pivot(/*lhs=*/intArrayOf(0), /*axis =*/ intArrayOf(1), /*...fanout=*/2, 3)
                p4.let { (col: Array<String>, data: Pair<Flow<Array<Any?>>, Int>) ->
                    println(col.map { it })
                    data.let { (rows: Flow<Array<Any?>>) ->
                        rows.collect { arr: Array<Any?> ->
                            println(arr.toList())
                        }
                        rows.first().last() shouldBe null
                        rows.toList().last().dropLast(1).last() shouldBe 820.0

                    }
                }
            }
            x()
        }

        "group" {

            println("group")
            val x = suspend {
                val r4: DecodedRows = columns reify f4
                var clusters: DecodedRows = r4.group(1)
                clusters.let { (colnames: Array<String>, data: Pair<Flow<Array<Any?>>, Int>) ->
                    val (rows) = data
                    println("by col 1")
                    println("${colnames.asList()} ")
                    rows.collect { arrayOfAnys: Array<Any?> ->
                        println(arrayOfAnys.contentDeepToString())
                    }
                    rows.count() shouldBe 3
                }
                clusters = r4.group( /*...by=*/0)
                clusters.let { (cnames: Array<String>, data: Pair<Flow<Array<Any?>>, Int>) ->
                    val (rows, _) = data
                    println("by col 0")
                    println("${cnames.asList()} ")
                    rows.collect { arrayOfAnys: Array<Any?> ->
                        println(arrayOfAnys.contentDeepToString())
                    }
                    rows.count() shouldBe 3
                }
                clusters = clusters.group(3)
                clusters.let { (names, data) ->
                    val (rows: Flow<Array<Any?>>, _: Int) = data
                    println("composability by previous group to column 3  ")
                    println("${names.asList()} ")
                    rows.collect { arrayOfAnys: Array<Any?> ->
                        println(arrayOfAnys.contentDeepToString())

                    }
                    rows.count() shouldBe 2
                }
            }
            x()
        }
        "composability" {
            println("pivotpivot")
            val x = suspend {
                var p4 = (columns reify f4).pivot(/*lhs = */intArrayOf(0),/* axis = */intArrayOf(1),/*fanout...*/ 2, 3)
                println(p4.first.contentDeepToString())
                p4.second.first.collect { println(it.contentDeepToString()) }

                val (compoundColumns: Array<String>, _) = p4
                println("--- pivot squared ")
                p4 = p4.pivot(lhs = intArrayOf(), axis = intArrayOf(0), fanOut = *compoundColumns.indices.drop(1).toIntArray())
                println(p4.first.contentDeepToString())
                p4.second.first.collect { println(it.contentDeepToString()) }
            }
            x()
        }
    }
}