package cursors


import cursors.context.*
import cursors.io.*
import cursors.io.IOMemento.*
import cursors.macros.`∑`
import cursors.macros.join
import cursors.macros.α
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.w3c.dom.Document
import org.w3c.dom.NodeList
import vec.macros.*
import vec.util.*
import java.io.File
import java.io.FileInputStream
import kotlin.coroutines.CoroutineContext

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import kotlin.math.min
import kotlin.random.Random
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
                when (retcode) {
                    0, 128 -> {
                        val strings = _a["zstd", "-d", "--memory=268MB", "--rm", "superannuated1909.fwf.zst"]
                        System.err.println("decompression:  $strings")
                        Runtime.getRuntime().exec(strings, null, Paths.get("..", "superannuate").toFile()).waitFor()
                    }
                    else -> throw Error(
                            "git fetch issue" + it + "\nstderr" + String(
                                    it.errorStream.readAllBytes()
                            )
                    )
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
            val varcharSizes = varcharMappings(theCursor, theCoords)
            (theCursor α floatFillNa(0f)).writeISAM(pathname.toString(), 24, varcharSizes)
        }
        System.err.println("transcription took: " + nanos)

        MappedFile(pathname.toString()).use { mf ->
            val binaryCursor = ISAMCursor(pathname, mappedFile = mf)
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
            (theCursor α floatFillNa(0f)).writeISAM(pathname.toString(), 24, varcharSizes)
        }
        System.err.println("transcription took: " + nanos)

        val fileSha256Sum = fileSha256Sum(pathname.toString())

        System.err.println("isam digest: " + fileSha256Sum)

        MappedFile(pathname.toString()).use { mf ->
            val binaryCursor = ISAMCursor(pathname, mappedFile = mf)
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
                    message = stringOf(RowVec(min(it.size, 100), it.second))
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
                    varcharMappings(theCursor, theCoords)
            (theCursor α floatFillNa(0f)).writeISAM(pathname.toString(), 24, varcharSizes)
        }
        System.err.println("transcription took: $nanos")

        MappedFile(pathname.toString()).use { mf ->
            val piv = ISAMCursor(pathname, mappedFile = mf).resample(0).pivot(
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

    fun cities(): Vect0r<String> {
        val xmlDocument: Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(FileInputStream("src/test/resources/List_of_largest_cities.html"))
        val xPath: XPath = XPathFactory.newInstance().newXPath()
        val expression = """//*[@id="mw-content-text"]/div/table[2]/tbody/tr[position() >= 3]/td[1]/a/text()"""
        val nodeList = xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET) as NodeList

        return Vect0r(nodeList.length) { ix: Int ->
            nodeList.item(ix).textContent
        }

    }

    /**
     * index based column substitution by gathering a cluster of the target column and bloom filtering the indexes.
     */
    @Test
    fun testBloomRemap() {
        // a list of cities at least as large as the cluster index (70)
        val cities: Vect0r<String> = cities()

        // SalesArea column is 1.  cluster key is on _a[1]
        val groupClusters: List<IntArray> = curs.groupClusters(_a[1])

        // replace lambda capture overhead from this cursor by using cluster-level bloom filters
        val bloomIndex: List<Pai2<BloomFilter, IntArray>> = bloomAccess(groupClusters)
        val scalarColHdr = Scalar(IoString, "City")
        val function: () -> Scalar = scalarColHdr.`⟲`


        val cityCursor:Cursor= Cursor(curs.size) { rowNum: Int ->
            RowVec(1) { ix: Int ->
                /*seeks++*/
                val cindex = bloomIndex.indexOfFirst {  (b, ia) -> b.contains(rowNum) && (ia.binarySearch(rowNum )> -1)/*.also { if (!it) misses++ } */ }
                cities[cindex] t2 function
            }
        }
        val citified:Cursor  = join(curs[0], cityCursor, curs[2 until curs.scalars.size])

        for (i in 0 until 100) {
            System.err.println(citified.second(Random.nextInt(citified.size)).left.toList())
        }
//        System.err.println("seeks:$seeks misses:$misses %${misses.toFloat()/seeks.toFloat() *100.0}") //11 bits is ~ 1%
    }
}

