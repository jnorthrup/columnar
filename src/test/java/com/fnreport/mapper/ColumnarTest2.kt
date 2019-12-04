package com.fnreport.mapper

import io.kotlintest.TestCase
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.*

@ExperimentalCoroutinesApi
@UseExperimental(InternalCoroutinesApi::class)
class ColumnarTest2 : StringSpec() {
    val columns: RowDecoder = listOf("date", "channel", "delivered", "ret").zip(
            listOf((0 to 10), (10 to 84), (84 to 124), (124 to 164))
                    .zip(    listOf(dateMapper,
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
        "resample"{

            System.err.println("time")
            val p4 = columns reify f4

            val indexcol = 0

            val expanded = resample(p4, indexcol)
            println("reaw resample")
            expanded.second.first.collect {
                println(it.contentDeepToString())

            }

            println("resampled groupby").also {
                var group = expanded.group(0)
                show(groupSumFloat(group[0] with group[1]{ any ->
                    (any as? Array<*>?)?.filterNotNull() ?: (any as? Collection<*>?)?.filterNotNull()
                } with group[2, 3], 0, 1))

            }

            println("resampled groupby pivot ").also {
                var group = expanded.group(0)
                show(groupSumFloat(group[0] with group[1]{ any ->
                    (any as? Array<*>?)?.filterNotNull() ?: (any as? Collection<*>?)?.filterNotNull()
                } with group[2, 3], 0, 1).pivot(intArrayOf(0), intArrayOf(1), 2, 3))
            }
        }

    }


}