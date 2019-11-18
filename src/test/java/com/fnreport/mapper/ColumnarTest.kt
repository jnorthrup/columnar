package com.fnreport.mapper

import io.kotlintest.TestCase
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.InternalCoroutinesApi

@UseExperimental(InternalCoroutinesApi::class)

class ColumnarTest : StringSpec() {

    val columns = listOf("date", "channel", "delivered", "ret").zip(
            arrayListOf((0 to 10), (10 to 84), (84 to 124), (124 to 164)).zip(
                    listOf(dateMapper(),
                            stringMapper(),
                            floatMapper(),
                            floatMapper(
                            ))))
    val f20 = FixedRecordLengthFile("src/test/resources/caven20.fwf")
    val f4 = FixedRecordLengthFile("src/test/resources/caven4.fwf")
    val c20 = Columnar(f20, columns)
    val c4 = Columnar(f4, columns)

    override fun beforeTest(testCase: TestCase) {

    }


    init {

        "dateCol"{
            val values20 = decode(1, c20)
            val any = values20[0]
            any.toString().shouldBe("2017-10-22")
            System.err.println(any)

        }
        "size" {
            f4.size.shouldBe(4)
            c4.size.shouldBe(4)
        }
        "values" {
            val values20 = decode(1, c20)
            System.err.println(values20)
            val values4 = decode(1, c4)
            System.err.println(values4)
            values20.shouldBe(values4)
        }

        "pivot" {
            val p4 = c4.pivot(listOf(0), 1, 2, 3)
            System.err.println(p4.columns.map { (s) -> s })
            (0 until p4.size).forEach {

                val values = p4.values(it)
                System.err.println(values)
            }
        }
        "pivotAggregate" {
            val p4 = c4.pivot(/*listOf(0)*/emptyList(), 1, 2, 3)
            p4[0, 1]{ any -> (any as? Iterable<Float>)?.sum() }
            System.err.println("agg:")
            System.err.println(p4.columns.map { (s) -> s })
            (0 until p4.size).forEach {
                val values = p4.values(it)
                System.err.println(values)
            }
        }

        "group" {
            val group1 = c4.group((0))
            val group2 = c4.group((1))
            System.err.println("group1:")
            (0 until group1.size).forEach { System.err.println(group1.values(it)) }
            System.err.println("group2:")
            (0 until group2.size).forEach { System.err.println(group2.values(it)) }
            group1[2].values(0).shouldBe(listOf(88.0f))
            group2.values(2)[2].toString().shouldBe(
            "[4.0, 820.0]"
            )
        }
        "groupAggregate" {
            var group1 = c4.group((0))
            var group2 = c4.group((1))
            System.err.println("group1Agg:")
            group1=group1[0,1]*group1[2,3]{ any -> (any as? Iterable<Float>)?.sum()?:any }
            (0 until group1.size).forEach { System.err.println(group1.values(it)) }
            group2=group2[0,1]*group1[2,3]{ any -> (any as? Iterable<Float>)?.sum()?:any }
            System.err.println("group2Agg:")
            (0 until group2.size).forEach { System.err.println(group2.values(it)) }
        }
        "group+pivot" {
            val by = listOf(0)
            val p4 = c4.pivot(listOf(0), 1, 2, 3)
            val g4 = p4.group((0))
            val res = g4.let { columnar ->
                (0 until columnar.size).map {
                    columnar.values(it)
                }
            }
            System.err.println("pivot+group:")
            val cnames = g4.columns.map { (cname) -> cname }
            res.forEach { row ->

                val tuple = cnames.zip(row)
                System.err.println(tuple)

            }
        }
    }

    private suspend fun spivot(c4: Columnar, arrayOf: Collection<Int>, i: Int, vararg rhs: Int): Columnar = c4.pivot(arrayOf, i, *rhs)

    private suspend fun s4(columnar: Columnar) = columnar.values(0).toList().map { it }

    private suspend fun decode(row: Int, columnar: Columnar): List<Any?> = columnar.values(row)
}