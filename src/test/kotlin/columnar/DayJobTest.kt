package columnar


import columnar.IOMemento.*
import columnar.context.Columnar
import columnar.context.FixedWidth
import columnar.context.NioMMap
import columnar.context.RowMajor
import columnar.context.RowMajor.Companion.fixedWidthOf
import columnar.context.RowMajor.Companion.indexableOf
import java.io.File
import java.time.Duration
import kotlin.coroutines.CoroutineContext
import kotlin.system.measureNanoTime

class DayJobTest/* : StringSpec()*/ {

    //        val suffix = "_100"//"_RD"  105340
    //    val suffix = "_1000"//"_RD"  3392440
//        val suffix = "_10000"     //"_RD"  139618738
        val suffix = "_100000"     //"_RD"
//      val suffix = "_1000000"     //"_RD"
//    val suffix = "_500000"     //"_RD"
    //    val suffix = "_300000"     //"_RD"
    //    val suffix = "_400000"     //"_RD"

    //    val suffix = "_1000"//"_RD"
//        val suffix = "_RD"
    //    val suffix = "_1000"//"_RD"
    //    val suffix = "_1000"//"_RD"
    //    val suffix = "_1000"//"_RD"

//        val suffix = ""//"_RD"
    val s = "/vol/aux/rejuve/rejuvesinceapril2019" + suffix + ".fwf"
    val coords = intArrayOf(
        0, 11,
        11, 15,
        15, 25,
        25, 40,
        40, 60,
        60, 82,
        82, 103,
        103, 108
    ).zipWithNext() ///.map<Pai2<Int, Int>, Tw1nt, Vect0r<Pai2<Int, Int>>> { (a,b): Pai2<Int, Int> -> Tw1n (a,b)  /*not fail*/ }/*.map { ints: IntArray -> Tw1nt(ints)  /*not fail*/ } */ /*.map(::Tw1nt) fail */ /* α ::Tw1nt fail*/

    val drivers = vect0rOf(
        IoString as TypeMemento,
        IoString,
        IoLocalDate,
        IoString,
        IoString,
        IoFloat,
        IoFloat,
        IoString
    )
    val names = vect0rOf(
        "SalesNo",    //        0
        "SalesAreaID",    //    1
        "date",    //           2
        "PluNo",    //          3
        "ItemName",    //       4
        "Quantity",    //       5
        "Amount",    //         6
        "TransMode"    //       7
    )


    val zip = drivers.zip(names)
    val columnar = Columnar(zip as Vect02<TypeMemento, String?>)
    val nioMMap = NioMMap(MappedFile(s), NioMMap.text(columnar.left))
    val fixedWidth: FixedWidth = fixedWidthOf(nioMMap, coords)
    val indexable = indexableOf(nioMMap, fixedWidth)
    val curs = cursorOf(RowMajor().fromFwf(fixedWidth, indexable, nioMMap, columnar)).also {

        System.err.println("record count=" + it.first)
    }


    var lastmessage: String? = null

    inline fun measureNanoTimeStr(block: () -> Unit): String {
        return Duration.ofNanos(measureNanoTime(block)).toString()
    }

    @org.junit.jupiter.api.Test
    fun `reorder+rewrite+pivot+pgroup+reduce`() {
        lateinit var message: String
        val pathname = File.createTempFile("dayjob", ".bin").toPath()
        val nanos = measureNanoTimeStr {
            System.err.println("using filename: " + pathname.toString())
            val theCursor = curs[ 2, 1, 3, 5 ].ordered(intArrayOf(0, 1, 2))
            val theCoords = coords[2, 1, 3, 5 ]
            val varcharSizes = varcharMappings(theCursor, theCoords)
            (theCursor α floatFillNa(0f)).writeBinary(pathname.toString(), 24, varcharSizes)
        }
        System.err.println("transcription took: " + nanos)

        MappedFile(pathname.toString()).use { mf ->
            val binaryCursor = binaryCursor(pathname, mappedFile = mf)
            val filtered = binaryCursor.resample(0).pivot(
                intArrayOf(0),
                intArrayOf(1, 2),
                intArrayOf(3)
            ).group(intArrayOf(0), floatSum)

            lateinit var second: RowVec
            println(
                "row 2 seektime: " +
                        measureNanoTimeStr {
                            second = filtered.second(2)
                        } + "@ " + second.first + " columns"
            )
            println("row 2 took " + measureNanoTimeStr {
                second.let {
                    println("row 2 is:")
                    message = stringOf(it)
                }
            } )
            println(message)
        }
    }

    @org.junit.jupiter.api.Test
    fun `rewrite+pivot+pgroup+reduce`() {
        lateinit var message: String
        val pathname = File.createTempFile("dayjob", ".bin").toPath()
        val nanos = measureNanoTimeStr {

            System.err.println("using filename: " + pathname.toString())
            val reorder = intArrayOf(2, 1, 3, 5)
            val theCursor = curs[reorder]
            val theCoords = coords[reorder]
            val varcharSizes = varcharMappings(theCursor, theCoords)
            (theCursor α floatFillNa(0f)).writeBinary(pathname.toString(), 24, varcharSizes)
        }
        System.err.println("transcription took: " + nanos)

        MappedFile(pathname.toString()).use { mf ->
            val binaryCursor = binaryCursor(pathname, mappedFile = mf)
            val filtered = binaryCursor.resample(0).pivot(
                intArrayOf(0),
                intArrayOf(1, 2),
                intArrayOf(3)
            ).group(intArrayOf(0), floatSum)

            lateinit var second: RowVec
            println(
                "row 2 seektime: " +
                        measureNanoTimeStr {
                            second = filtered.second(2)
                        } + "@ " + second.first + " columns"
            )
            println("row 2 took " + measureNanoTimeStr {
                second.let {
                    println("row 2 is:")
                    message = stringOf(it)
                }
            } )
            println(message)
        }
    }

    fun varcharMappings(
        theCursor: Pai2<Int, (Int) -> Vect0r<Pai2<Any?, () -> CoroutineContext>>>,
        theCoords: Vect0r<Pai2<Int, Int>>
    ) = (theCursor.scalars as Vect02<TypeMemento, String?>).left.toList().mapIndexed { index, typeMemento ->
        index t2 typeMemento
    }.filter { (_, b) -> b == IoString }.map { (a, _) ->
        a to theCoords[a].size
    }.toMap()

    @org.junit.jupiter.api.Test
    fun `pivot+group+reduce`() {
        val piv: Cursor = curs[2, 1, 3, 5].resample(0).pivot(
            intArrayOf(0),
            intArrayOf(1, 2),
            intArrayOf(3)
        ).group(0)
        val filtered = join(piv[0], (piv[1 until piv.scalars.first] /*α floatFillNa(0f)*/).`∑`(floatSum))

        lateinit var second: RowVec
        println(
            "row 2 seektime: " +
                    measureNanoTimeStr {
                        second = filtered.second(2)
                    } + "@ " + second.first + " columns"
        )
        lateinit var message: String
        println("row 2 took " + measureNanoTimeStr {
            second.let {
                println("row 2 is:")
                message = stringOf(it)
            }
        } )
        println(message)
    }

    @org.junit.jupiter.api.Test
    fun `rorw+pivot+group+reduce`() {
        lateinit var message: String
        val pathname = File.createTempFile("dayjob", ".bin").toPath()
        val nanos = measureNanoTimeStr {
            System.err.println("using filename: " + pathname.toString())
            val arrangement = intArrayOf(2, 1, 3, 5)
            val theCursor = curs.ordered(intArrayOf(2, 1, 3))   [arrangement]
            val theCoords = coords[arrangement]
            val varcharSizes = varcharMappings(theCursor, theCoords)
            (theCursor α floatFillNa(0f)).writeBinary(pathname.toString(), 24, varcharSizes)
        }
        System.err.println("transcription took: " + nanos)

        MappedFile(pathname.toString()).use { mf ->
            val piv = binaryCursor(pathname, mappedFile = mf).resample(0).pivot(
                intArrayOf(0),
                intArrayOf(1, 2),
                intArrayOf(3)
            ).group((0))
            val filtered = join(piv[0], (piv[1 until piv.scalars.first] ).`∑`(floatSum))

            lateinit var second: RowVec
            println(
                "row 2 seektime: " +
                        measureNanoTimeStr {
                            second = filtered.second(2)
                        } + "@ " + second.first + " columns"
            )
            println("row 2 took " + measureNanoTimeStr {
                second.let {
                    println("row 2 is:")
                    message = stringOf(it)
                }
            } )
            println(message)
        }
    }
}

