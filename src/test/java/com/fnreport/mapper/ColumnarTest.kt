@file:Suppress("UNCHECKED_CAST")

package com.fnreport.mapper

import arrow.core.Option
import arrow.core.Some
import io.kotlintest.TestCase
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.*

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
                val (_, data: Pair<Flow<Array<Any?>>, Int>) = r4
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
                p4.let { (col, data: Pair<Flow<Array<Any?>>, Int>) ->
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
                clusters.let { (colnames, data: Pair<Flow<Array<Any?>>, Int>) ->
                    val (rows) = data
                    println("by col 1")
                    println("${colnames.asList()} ")
                    rows.collect { arrayOfAnys: Array<Any?> ->
                        println(arrayOfAnys.contentDeepToString())
                    }
                    rows.count() shouldBe 3
                }
                clusters = r4.group( /*...by=*/0)
                clusters.let { (cnames, data: Pair<Flow<Array<Any?>>, Int>) ->
                    val (rows, _) = data
                    println("by col 0")
                    println("${cnames.map { it.first }}")
                    rows.collect { arrayOfAnys: Array<Any?> ->
                        println(arrayOfAnys.contentDeepToString())
                    }
                    rows.count() shouldBe 3
                }
                clusters = clusters.group(3)
                clusters.let { (names, data) ->
                    val (rows: Flow<Array<Any?>>, _: Int) = data
                    println("composability by previous group to column 3  ")
                    println("${names.map { it.first }} ")
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
            var x = suspend {
                var p4 = (columns reify f4).pivot(/*lhs = */intArrayOf(0),/* axis = */intArrayOf(1),/*fanout...*/ 2, 3)
                also {
                    val (a, b) = p4
                    println(a.map { it.first })
                    val (c, d) = b
                    c.collect { println(it.contentDeepToString()) }
                }
                also {
                    val (compoundColumns, _) = p4
                    println("--- pivot squared ")
                    p4 = p4.pivot(lhs = intArrayOf(), axis = intArrayOf(0), fanOut = *compoundColumns.indices.drop(1).toIntArray())

                    val (a, b) = p4
                    println(a.map { it.first })
                    val (c, d) = b
                    c.collect { println(it.contentDeepToString()) }

                }
            }
            x()
        }
        "group pivot"{
            println("pivotgroup")

            val x = suspend {
                val p4 = (columns reify f4).pivot(/*lhs = */intArrayOf(0),/* axis = */intArrayOf(1),/*fanout...*/ 2, 3)
                        .group(0)
                val (a, b) = p4
                val (c, _) = b
                System.err.println(a.contentDeepToString())
                c.collect { println(it.contentDeepToString()) }
            }

            x()
        }
        "group pivot fillna"{
            println("pivotgroupfillna")

            val x = suspend {

                var c4 = columns[0, 1] + columns[2, 3]{ any: Any? -> any ?: 0f }
                val pivot = (c4 reify f4)
                        .pivot(intArrayOf(0), intArrayOf(1), 2, 3)

                val col = pivot.first.indices.drop(1).toIntArray()
                val pair = pivot[0]
                val pair1 = pivot.get(*col).invoke({ any: Any? -> any ?: 0f })

                var p4 = (pair with pair1).let {
                    var (a, b) = it
                    var (c, _) = b
                    System.err.println(a.contentDeepToString())
                    c.collect { ar ->
                        val message = ar.mapIndexed { ind, v ->
                            (a[ind].second.fold({ v }, { it(v) }))
                        }
                        println(message
                        )
                    }


                }

                val pair2 = pair with pair1
                val ind = pair2.first.indices.drop(1).toIntArray()
                val group = (pair2)
                        .group(0)
                p4 = (group[0] with group.get(*ind).invoke  { it: Any? ->
                            when {
                                it is Array <*>-> it.map{(it as? Float? )?:0f}.sum()
                                it is List <*>-> it.map{(it as? Float? )?:0f}.sum()
                                else -> it
                            }
                        })
                        .let {
                            var (a, b) = it
                            var (c, _) = b
                            System.err.println(a.contentDeepToString())
                            c.collect { ar ->
                                val message = ar.mapIndexed { index, any ->
                                    a[index].second.fold({any},{it(any)})
                                }
                                println(message)
                            }
                        }
            }
            x()
        }
    }
}

private operator fun RowDecoder.invoke(t: xform): RowDecoder = map { (a, b) ->
    val (c, d) = b
    a to (c to { any: Any? -> t(d(any)) })
}.toTypedArray()


operator fun DecodedRows.invoke(t: xform): DecodedRows = this.let { (a, b) ->
    a.map { (c, d) ->
        c to Some(d.fold({ t }, { dprime: xform ->
            { rowval: Any? ->
                t(dprime(rowval))
            }
        })) as Option<xform>
    }.toTypedArray() to b
}

infix suspend fun DecodedRows.with(that: DecodedRows): DecodedRows = let { (a, b) ->
    b.let { (c, d) ->
        val second1 = that.second.second
        assert(second1 == d) { "rows must be same -- ${d} !== $second1" }
        val toList = c.toList()
        val toList1 = that.second.first.toList()
        val x = toList.mapIndexed { index: Int, v: Array<Any?> ->
            val r = v.toList() + toList1[index].toList()
            r.toTypedArray()
        }.asFlow()
        (a + that.first) to (x to d)
    }
}

