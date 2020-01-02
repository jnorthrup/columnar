package id.rejuve.dayjob

import columnar.*
import columnar.IOMemento.*
import columnar.context.Columnar
import columnar.context.FixedWidth
import columnar.context.NioMMap
import columnar.context.RowMajor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.ImplicitReflectionSerializer

@ImplicitReflectionSerializer
@ExperimentalCoroutinesApi

suspend fun main() {
    val suffix = "_RD"
    val s = "/vol/aux/rejuve/rejuvesinceapril2019" +
            suffix +
            ".fwf"
    val coords = vZipWithNext(
        intArrayOf(
            0, 11,
            11, 15,
            15, 25,
            25, 40,
            40, 60,
            60, 82,
            82, 103,
            103, 108
        )
    )


    val drivers = arrayOf(
        IoString,
        IoString,
        IoLocalDate,
        IoString,
        IoString,
        IoFloat,
        IoFloat,
        IoString
    )
    val names = listOf("SalesNo", "SalesAreaID", "date", "PluNo", "ItemName", "Quantity", "Amount", "TransMode")


    val columnar = Columnar.of(*drivers)
    val nioMMap = NioMMap(MappedFile(s), NioMMap.text(columnar.type))
    val fixedWidth: FixedWidth = fixedWidthOf(nioMMap, coords)
    val indexable = indexableOf(nioMMap, fixedWidth)

    val fromFwf = fromFwf(RowMajor(), fixedWidth, indexable, nioMMap, columnar)
    val cursorOf = (cursorOf(fromFwf)[0..10])
    System.err.println(cursorOf.reify())

}


//    System.err.println("rows ppre-resampling: " + rejuve.second.second)
//    rejuve = rejuve[2, 1, 3, 5].resample(0)
//    System.err.println("rows post-resampling: " + rejuve.second.second)
//    rejuve.head(10)
//    val keyAxis = intArrayOf(1, 2)
//    suspend {
//        lateinit var dist: List<Array<Any?>>
//        val t = measureTimeMillis {
//            dist = rejuve.distinct(*keyAxis)
//        }
//
//        System.err.println("$t ms for  rows with distinct: ${dist.size}")
//
//        dist.slice(0..20).forEach { println(it.contentDeepToString()) }
//    }()
//    System.err.println()
//
//    rejuve = rejuve.pivot(intArrayOf(0), keyAxis, 3)
//    val tmpPrefix = "/tmp/rjuv2" + suffix
//    val tmpName = tmpPrefix + ".fwf"
//    val elements: RowBinEncoder = rejuve.toFwf2(tmpName)
//    val meta: List<RowBinMeta> = RowBinMeta.RowBinMetaList(elements)
//    val out = Json(JsonConfiguration.Default).toJson(RowBinMeta::class.serializer().list, meta.map { (it) })
//    FileWriter(tmpPrefix + ".json").use {
//        it.write(out.toString())
//    }
////    println(out)
//}
//
//private suspend fun KeyRow.head(n: Int) {
//    let { (_, b) ->
//        b.let { (c, _) ->
//            c.take(n).collect { it ->
//                System.err.println(it.contentDeepToString())
//            }
//        }
//    }
//}
