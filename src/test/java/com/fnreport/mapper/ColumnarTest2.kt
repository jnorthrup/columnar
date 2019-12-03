package com.fnreport.mapper

import io.kotlintest.TestCase
import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collect

@ExperimentalCoroutinesApi
@UseExperimental(InternalCoroutinesApi::class)
class ColumnarTest2 : StringSpec() {
    val columns: RowDecoder = listOf("date", "channel", "delivered", "ret").zip(
            listOf((0 to 10), (10 to 84), (84 to 124), (124 to 164))
                    .zip(listOf(dateMapper,
                            stringMapper,
                            floatMapper,
                            floatMapper))).toTypedArray()
    val f4 = FixedRecordLengthFile("src/test/resources/caven4.fwf")//4 rows of data
    val p4 = columns reify f4
    init {"resampled pivot groupby date sum"{

        val resampled = resample(p4, 0)
        val pivot = resampled.pivot(intArrayOf(0), intArrayOf(1), 2, 3).group(0)
        val (pivotColumns) = pivot

        val left = pivot[0]
        val right = pivot.get(*(1 until pivotColumns.size).toList().toIntArray()).invoke {
            (it.let { deepTrim(it) as Array<Any?>}.map{(it as? Float )?:0f} .sum() )
        }
        show(left with right)

    }

   "resample"{

            System.err.println("time")

            val indexcol = 0

            val expanded = resample(p4, indexcol)
            println("reaw resample")
            expanded.second.first.collect {
                println(it.contentDeepToString())
            }

        }
        "resample group"{

            val indexcol = 0
            val expanded = resample(p4, indexcol)
            var group = expanded.group(0)
            show(groupSumFloat(group[0] with group[1]{ any ->
                (any as? Array<*>?)?.filterNotNull() ?: (any as? Collection<*>?)?.filterNotNull()
            } with group[2, 3], 0, 1))

        }
        "resampled pivot"{

            val indexcol = 0

            val expanded = resample(p4, indexcol)

            val pivot = expanded.pivot(intArrayOf(0), intArrayOf(1), 2, 3)
            val (a, b) = pivot
            val (c, d) = b
            show(pivot)
        }
        "resampled pivot groupby date"{

            val indexcol = 0

            val expanded = resample(p4, indexcol)

            val pivot = expanded.pivot(intArrayOf(0), intArrayOf(1), 2, 3).group(0)
            val (a, b) = pivot
            val (c, d) = b
            show(pivot)
        }


    }
}