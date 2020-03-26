package columnar


import columnar.context.*
import columnar.io.*
import columnar.io.IOMemento.*
import columnar.macros.*
import columnar.util._a
import columnar.util._l
import columnar.util.size
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import kotlin.coroutines.CoroutineContext
import kotlin.system.measureNanoTime

class DayJobTest {
    var curs1: Cursor
    var curs: Cursor
    var indexable: Indexable
    var fixedWidth: FixedWidth
    var nioMMap: NioMMap
    var columnar: Columnar
    var zip: Pai2<Int, (Int) -> Pai2<TypeMemento, String>>
    var names: Pai2<Int, (Int) -> String>
    var drivers: Pai2<Int, (Int) -> TypeMemento>
    var coords: Pai2<Int, (Int) -> Pai2<Int, Int>>
    var rowFwfFname: Path

    val testRecordCount = 100_000

    init {
        this.rowFwfFname = Paths.get("..", "superannuate", "superannuated1909.fwf")
        this.coords = intArrayOf(
            0, 11,
            11, 15,
            15, 25,
            25, 40,
            40, 60,
            60, 82,
            82, 103,
            103, 108
        ).zipWithNext() ///.map<Pai2<Int, Int>, Tw1nt, Vect0r<Pai2<Int, Int>>> { (a,b): Pai2<Int, Int> -> Tw1n (a,b)  /*not fail*/ }/*.map { ints: IntArray -> Tw1nt(ints)  /*not fail*/ } */ /*.map(::Tw1nt) fail */ /* α ::Tw1nt fail*/

        this.drivers = vect0rOf(
            IoString as TypeMemento,
            IoString,
            IoLocalDate,
            IoString,
            IoString,
            IoFloat,
            IoFloat,
            IoString
        )
        this.names = vect0rOf(
            "SalesNo",    //        0
            "SalesAreaID",    //    1
            "date",    //           2
            "PluNo",    //          3
            "ItemName",    //       4
            "Quantity",    //       5
            "Amount",    //         6
            "TransMode"    //       7
        )

        val fetchDayjobData = _l[
                "git clone --depth=1 git@github.com:jnorthrup/superannuate",
                "zstd -d --memory=268MB --rm superannuated1909.fwf.zst"

        ]

        if (!Files.exists(rowFwfFname)) {
            val s = "git@github.com:jnorthrup/superannuate"
            System.err.println("fetching $s")
            val parentDir = Paths.get("..").toFile()
            Runtime.getRuntime().exec(_a["git", "clone", "--depth=1", s], null, parentDir).also {


                val retcode = it.waitFor()
                if (retcode != 0) throw Error(
                    "git fetch issue" + it + "\nstderr" + String(
                        it.errorStream.readAllBytes()
                    )
                )
                else {
                    val strings = _a["zstd", "-d", "--memory=268MB", "--rm", "superannuated1909.fwf.zst"]
                    System.err.println("decompression:  $strings")
                    Runtime.getRuntime().exec(strings, null, Paths.get("..", "superannuate").toFile()).waitFor()
                }
            }
        }

        this.zip = drivers.zip(names)
        this.columnar = Columnar(zip as Vect02<TypeMemento, String?>)
        this.nioMMap = NioMMap(MappedFile(rowFwfFname.toString()), NioMMap.text(columnar.left))
        this.fixedWidth = RowMajor.fixedWidthOf(nioMMap, coords)
        this.indexable = RowMajor.indexableOf(nioMMap, fixedWidth)
        this.curs1 = cursorOf(
            RowMajor().fromFwf(
                fixedWidth,
                indexable,
                nioMMap,
                columnar
            )
        ).also {
            System.err.println("curs1 record count=" + it.first)
        }
        this.curs = Cursor(minOf(curs1.size, testRecordCount), { y: Int -> curs1 at (y) }).also {
            System.err.println("curs record count=" + it.first)
        }

        var lastmessage: String? = null
    }

    inline fun measureNanoTimeStr(block: () -> Unit): String = Duration.ofNanos(measureNanoTime(block)).toString()

    @org.junit.jupiter.api.Test
    fun `reorder+rewrite+pivot+pgroup+reduce`() {
        lateinit var message: String
        val pathname = File.createTempFile("dayjob", ".bin").toPath()
        val nanos = measureNanoTimeStr {
            System.err.println("using filename: " + pathname.toString())
            val theCursor = curs[2, 1, 3, 5].ordered(intArrayOf(0, 1, 2))
            val theCoords = coords[2, 1, 3, 5]
            val varcharSizes = varcharMappings(theCursor as Cursor, theCoords)
            ((theCursor α floatFillNa(0f)) as Cursor).writeBinary(pathname.toString(), 24, varcharSizes)
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
                            second = filtered at (2)
                        } + "@ " + second.first + " columns"
            )
            println("row 2 took " + measureNanoTimeStr {
                second.let {
                    println("row 2 is:")
                    message = stringOf(it)
                }
            })
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
                            second = filtered at (2)
                        } + "@ " + second.first + " columns"
            )
            println("row 2 took " + measureNanoTimeStr {
                second.let {
                    println("row 2 is:")
                    message = stringOf(it)
                }
            })
            println(message)
        }
    }

    fun varcharMappings(
        theCursor: Cursor,
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
        println("row 2 seektime: " +
                measureNanoTimeStr {
                    second = filtered at (2)
                } + "@ " + second.first + " columns"
        )
        lateinit var message: String
        println("row 2 took " + measureNanoTimeStr {
            second.let {
                println("row 2 is:")
                message = stringOf(it)
            }
        })
        println(message)
    }

    @org.junit.jupiter.api.Test
    fun `rorw+pivot+group+reduce`() {
        lateinit var message: String
        val pathname = File.createTempFile("dayjob", ".bin").toPath()
        val nanos = measureNanoTimeStr {
            System.err.println("using filename: $pathname")
            val arrangement = intArrayOf(2, 1, 3, 5)
            val theCursor = curs.ordered(intArrayOf(2, 1, 3))[arrangement]
            val theCoords = coords[arrangement]
            val varcharSizes =
                varcharMappings(theCursor as Pai2<Int, (Int) -> Vect0r<Pai2<Any?, () -> CoroutineContext>>>, theCoords)
            ((theCursor α floatFillNa(0f)) as Cursor).writeBinary(pathname.toString(), 24, varcharSizes)
        }
        System.err.println("transcription took: $nanos")

        MappedFile(pathname.toString()).use { mf ->
            val piv = binaryCursor(pathname, mappedFile = mf).resample(0).pivot(
                intArrayOf(0),
                intArrayOf(1, 2),
                intArrayOf(3)
            ).group((0))
            val filtered = join(piv[0], (piv[1 until piv.scalars.first]).`∑`(floatSum))

            lateinit var second: RowVec
            println(
                "row 2 seektime: " +
                        measureNanoTimeStr {
                            second = filtered at (2)
                        } + "@ " + second.first + " columns"
            )

            println("row 2 took " + measureNanoTimeStr {
                second.let {
                    println("row 2 is:")
                    message = stringOf(it)
                }
            })

            println(message)
        }
    }
}

