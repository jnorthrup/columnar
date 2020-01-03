package id.rejuve.dayjob

import columnar.*
import columnar.IOMemento.*
import columnar.context.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlin.coroutines.CoroutineContext

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


    val drivers = vect0rOf(
        IoString,
        IoString,
        IoLocalDate,
        IoString,
        IoString,
        IoFloat,
        IoFloat,
        IoString
    )
    val names = vect0rOf("SalesNo", "SalesAreaID", "date", "PluNo", "ItemName", "Quantity", "Amount", "TransMode")


    val zip  = names.zip(drivers)
    val columnar = Columnar.of(zip)
    val nioMMap = NioMMap(MappedFile(s), NioMMap.text(columnar.first))
    val fixedWidth: FixedWidth = fixedWidthOf(nioMMap, coords)
    val indexable = indexableOf(nioMMap, fixedWidth)

    val fromFwf = fromFwf(RowMajor(), fixedWidth, indexable, nioMMap, columnar)

    var curs = (cursorOf(fromFwf))
    curs = curs[2, 1, 3, 5]
    val scalars = curs[0].toList() α { (_, b): Pai2<Any?, () -> CoroutineContext> ->
        val context = b()
        runBlocking<Pai2<String, IOMemento>>(context) {
            (coroutineContext[Arity.arityKey] as Scalar).let { (a, b): Scalar ->
                b!! t2 a

            }
        }
    }
    //α ({ it: CoroutineContext -> it[Arity.arityKey] as Scalar } )α { it: Scalar -> it }).toList()
    val pai2 = scalars α Pai2<String, IOMemento>::pair
    System.err.println("" + pai2.toList())
    System.err.println("" + scalars[1].pair)
    System.err.println("" + curs[0][0].pair)
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
