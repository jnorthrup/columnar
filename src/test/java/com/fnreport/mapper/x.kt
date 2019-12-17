package id.rejuve.dayjob

import arrow.core.none
import columnar.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.list
import kotlinx.serialization.serializer
import java.io.FileWriter
import java.time.LocalDate
import kotlin.reflect.KClass
import kotlin.system.measureTimeMillis

@ImplicitReflectionSerializer
@ExperimentalCoroutinesApi

suspend fun main() {
    val suffix = "_RD"
    val s = "/vol/aux/rejuve/rejuvesinceapril2019" +
            suffix +
            ".fwf"
    val fixedRecordLengthFile = FixedRecordLengthFile(s)
    val decoder: RowNormalizer =
        listOf("SalesNo", "SalesAreaID", "date", "PluNo", "ItemName", "Quantity", "Amount", "TransMode").zip(
            listOf(
                (0 to 11),
                (11 to 15),
                (15 to 25),
                (25 to 40),
                (40 to 60),
                (60 to 82),
                (82 to 103),
                (103 to 108)
            ).zip(
                listOf<KClass<*>>(
                    String::class,
                    String::class,
                    LocalDate::class,
                    String::class,
                    String::class,
                    Float::class,
                    Float::class,
                    String::class
                )
            )
        ).map { it by none<xform>() }.toTypedArray()
    var rejuve = decoder reify fixedRecordLengthFile
    System.err.println("rows ppre-resampling: " + rejuve.second.second)
    rejuve = rejuve[2, 1, 3, 5].resample(0)
    System.err.println("rows post-resampling: " + rejuve.second.second)
    rejuve.head(10)
    val keyAxis = intArrayOf(1, 2)
    suspend {
        lateinit var dist: List<Array<Any?>>
        val t = measureTimeMillis {
            dist = rejuve.distinct(*keyAxis)
        }

        System.err.println("$t ms for  rows with distinct: ${dist.size}")

        dist.slice(0..20).forEach { println(it.contentDeepToString()) }
    }()
    System.err.println()

    rejuve = rejuve.pivot(intArrayOf(0), keyAxis, 3)
    val tmpPrefix = "/tmp/rjuv2" + suffix
    val tmpName = tmpPrefix + ".fwf"
    val elements:RowBinEncoder = rejuve.toFwf2(tmpName)
    val meta: List<RowBinMeta> =  RowBinMeta.RowBinMetaList(elements)
    val out = Json(JsonConfiguration.Default).toJson(RowBinMeta::class.serializer().list, meta.map {  (it) }  )
    FileWriter(tmpPrefix+".json").use{
        it.write(out.toString())
    }
//    println(out)
}

private suspend fun KeyRow.head(n: Int) {
    let { (_, b) ->
        b.let { (c, _) ->
            c.take(n).collect { it ->
                System.err.println(it.contentDeepToString())
            }
        }
    }
}
