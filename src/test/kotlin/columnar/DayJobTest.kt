package columnar


import columnar.IOMemento.*
import columnar.context.*
import columnar.context.RowMajor.Companion.fixedWidthOf
import columnar.context.RowMajor.Companion.indexableOf
import java.io.File
import kotlin.coroutines.CoroutineContext
import kotlin.system.measureTimeMillis

class DayJobTest/* : StringSpec()*/ {

    //        val suffix = "_100"//"_RD"  105340
    //    val suffix = "_1000"//"_RD"  3392440
    //    val suffix = "_10000"     //"_RD"  139618738
//        val suffix = "_100000"     //"_RD"
//      val suffix = "_1000000"     //"_RD"
    val suffix = "_500000"     //"_RD"
    //    val suffix = "_300000"     //"_RD"
    //    val suffix = "_400000"     //"_RD"

    //    val suffix = "_1000"//"_RD"
    //    val suffix = "_RD"
    //    val suffix = "_1000"//"_RD"
    //    val suffix = "_1000"//"_RD"
    //    val suffix = "_1000"//"_RD"
//        val suffix = ""//"_RD"
    private val s = //"/vol/aux/rejuve/rejuvesinceapril2019" + suffix + ".fwf"
            "/home/me/Descargas/rejuvesinceapril2019_100000.fwf"
    private val coords = intArrayOf(
        0, 11,
        11, 15,
        15, 25,
        25, 40,
        40, 60,
        60, 82,
        82, 103,
        103, 108
    ).zipWithNext() ///.map<Pai2<Int, Int>, Tw1nt, Vect0r<Pai2<Int, Int>>> { (a,b): Pai2<Int, Int> -> Tw1n (a,b)  /*not fail*/ }/*.map { ints: IntArray -> Tw1nt(ints)  /*not fail*/ } */ /*.map(::Tw1nt) fail */ /* α ::Tw1nt fail*/

    private val drivers = vect0rOf(
        IoString as TypeMemento,
        IoString,
        IoLocalDate,
        IoString,
        IoString,
        IoFloat,
        IoFloat,
        IoString
    )
    private val names = vect0rOf(
        "SalesNo",    //        0
        "SalesAreaID",    //    1
        "date",    //           2
        "PluNo",    //          3
        "ItemName",    //       4
        "Quantity",    //       5
        "Amount",    //         6
        "TransMode"    //       7
    )

    private val zip = drivers.zip(names)
    private val columnar = Columnar(zip   as Vect02<TypeMemento, String?>)
    private val nioMMap = NioMMap(MappedFile(s), NioMMap.text(columnar.left))
    private val fixedWidth: FixedWidth = fixedWidthOf(nioMMap, coords)
    private val indexable = indexableOf(nioMMap, fixedWidth)
    private val curs = cursorOf(RowMajor().fromFwf(fixedWidth, indexable, nioMMap, columnar)).also {
        System.err.println("record count=" + it.first)
    }


    var lastmessage: String? = null

    @org.junit.jupiter.api.Test
    fun `rewrite+pivot+pgroup+reduce`() {
        lateinit var message: String
        val pathname = File.createTempFile("dayjob", ".bin").toPath()
        val millis = measureTimeMillis {

            System.err.println("using filename: " + pathname.toString())
            val reorder = intArrayOf(2, 1, 3, 5)
            val theCursor = curs[reorder]
            val theCoords=coords[reorder]
            val varcharSizes = varcharMappings(theCursor, theCoords)
            (theCursor α floatFillNa(0f)).writeBinary(pathname.toString(),24, varcharSizes)
        }

        System.err.println("transcription took: " + millis)
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
                        measureTimeMillis {
                            second = filtered.second(2)
                        } + " ms @ " + second.first + " columns"
            )
            println("row 2 took " + measureTimeMillis {
                second.let {
                    println("row 2 is:")
                    message = stringOf(it)
                }
            } + "ms")
            println(message)
        }
    }

    private fun varcharMappings(
        theCursor: Pai2<Int, (Int) -> Vect0r<Pai2<Any?, () -> CoroutineContext>>>,
        theCoords: Vect0r<Pai2<Int, Int>>
    )  = (theCursor.scalars as Vect02<TypeMemento, String?>).left.toList().mapIndexed { index, typeMemento ->
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
        ).group((0))
        val filtered = join(piv[0], (piv[1 until piv.scalars.first] /*α floatFillNa(0f)*/).`∑`(floatSum))

        lateinit var second: RowVec
        println(
            "row 2 seektime: " +
                    measureTimeMillis {
                        second = filtered.second(2)
                    } + " ms @ " + second.first + " columns"


        )
        lateinit var message: String
        println("row 2 took " + measureTimeMillis {
            second.let {
                println("row 2 is:")
                message = stringOf(it)
            }
        } + "ms")
        println(message)


    }
}

