package id.rejuve.dayjob

import com.fnreport.mapper.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlin.system.measureTimeMillis

@ExperimentalCoroutinesApi
@UseExperimental(InternalCoroutinesApi::class)

suspend fun main() {
    val d1names = listOf("SalesNo", "SalesAreaID", "date", "PluNo", "ItemName", "Quantity", "Amount", "TransMode")
    val cw = listOf((0 to 11), (11 to 15), (15 to 25), (25 to 40), (40 to 60), (60 to 82), (82 to 103), (103 to 108))
    val s = "/vol/aux/rejuve/rejuvesinceapril2019_500000" +
            ".fwf"
    val fixedRecordLengthFile = FixedRecordLengthFile(s)
    val other: List<ByteBufferNormalizer> = cw.zip(listOf(stringMapper, stringMapper, dateMapper, stringMapper, stringMapper, floatMapper, floatMapper, stringMapper))
    val thing = d1names.zip(other)
    val decoder = (thing).toTypedArray()
    val rejuve = decoder reify fixedRecordLengthFile
/*    rejuve[7]
    }   */
    val forecast = rejuve[2, 1, 3, 5]

    forecast.let { (a, b) ->
        b.let { (c, d) ->
            val take = c.take(10)
            take.collect {
                System.err.println(it.contentDeepToString())
            }
        }
    }
    val keyAxis = intArrayOf(1, 2)
    suspend {
        lateinit var dist: List<Array<Any?>>
        var t = measureTimeMillis {

            dist = forecast.distinct(*keyAxis)
        }

        System.err.println("$t ms for  rows with distinct: ${dist.size}")

        dist.slice(0..20).forEach { println(it.contentDeepToString()) }
    }()
    System.err.println()

    val pivot = forecast.pivot(intArrayOf(0), keyAxis, 3).group(0)
    var t = measureTimeMillis {
        val left = pivot[0]
        val right = pivot[ (1 until pivot.first.size).toList().toIntArray() ].invoke {
            (it.let { deepTrim(it) as Array<Any?> }.map { (it as? Float) ?: 0f }.sum())
        }
        show(left with right)
    }
    System.err.println("$t ms  ")
}
