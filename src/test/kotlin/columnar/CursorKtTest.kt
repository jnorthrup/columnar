package columnar

import columnar.IOMemento.*
import columnar.context.Columnar
import columnar.context.NioMMap
import columnar.context.RowMajor
import io.kotlintest.specs.StringSpec
import kotlin.coroutines.CoroutineContext

class CursorKtTest : StringSpec() {
    val coords = vZipWithNext(
        intArrayOf(
            0, 10,
            10, 84,
            84, 124,
            124, 164
        )
    )
    val drivers = vect0rOf(
        IoLocalDate,
        IoString,
        IoFloat,
        IoFloat
    )

    init {
        val names = vect0rOf("date", "channel", "delivered", "ret")
        val mf = MappedFile("src/test/resources/caven4.fwf")
        val nio = NioMMap(mf)
        val fixedWidth = fixedWidthOf(nio, coords)
        val root = fromFwf(RowMajor(), fixedWidth, indexableOf(nio, fixedWidth), nio, Columnar(drivers, names))
        "resample" {
            val cursor: Cursor = cursorOf(root)
            System.err.println(cursor.narrow().toList())
            val toList = cursor.resample(0).narrow().toList()
            toList.forEach { System.err.println(it) }
        }
        "pivot" {
            val cursor: Cursor = cursorOf(root)
            System.err.println(cursor.narrow().toList())
//            cursor.pivot(intArrayOf(0), intArrayOf(1), intArrayOf(2, 3))

        }
    }
}
//
//fun Cursor.pivot(lhs: IntArray, axis: IntArray, fanOut: IntArray) = let { cursr ->
//
//    val scalars = this.scalars
//    val targetScalars = this[fanOut].scalars
//    val passthru = this[lhs].scalars
//    val keyMeta = this[axis].scalars
//    val knames = keyMeta.α { (_, name): Pai2<IOMemento, String?> -> name }
//    val keys = (this[axis] α { pai2: Vect02<Any?, () -> CoroutineContext> -> pai2.left })
//        .toList()
//        .distinct()
//    val hashToIndex = keys.mapIndexed { xIndex, any -> any.hashCode() to xIndex }.toMap()
//
//
//    val pivotSize = keys.size * fanOut.size
//    val xsize = passthru.size + pivotSize
//    val second1 = cursr.second
//
//    lateinit var rv:RowVec
//
//
//
//    val function: (Int) -> Pai2<() -> Int, (Int) -> Pai2<() -> Int, (Int) -> Pai2<Any?, () -> CoroutineContext>>> = { ix: Int ->
//        cursr[ix]
//    }
//    function
//
//
//}
//
//
///*
//    val keys = keyIndex.mapIndexed { index, list -> list to index }.toMap()
//*/
///*
//
//    val map1 = keys.map { (kee: List<Any?>, ix: Int): Map.Entry<List<Any?>, Int> ->
//        knames.zip(kee.toVect0r()).α { (n, v): Pai2<String?, Any?> -> "[$n:$v]" }.toList().joinToString(
//            ",", "(", ")"
//        ) t2 ix t3 kee
//    }.toVect0r()
//    val pivotSize = keys.size * fanOut.size
//    val xsize = passthru.size + pivotSize
//    cursr[passthru]
//*/
//
//
////    val map   =
////        map1.map{ (prefix,ix,kee)->
////            targetScalars α { (driver: IOMemento, b: String?): Scalar ->
////                driver t2 "$prefix:$b" t3 kee t4 ix
////            }
////        } .map {(a,q )->
////
////            {ix:Int->q(ix).let{(c: IOMemento,d: String,e: List<Any?>,f: Int)->
////
////                cursr.let { (a,b)->
////
////
////                }
////            }
////
////        }
////
////
////
////}
////
