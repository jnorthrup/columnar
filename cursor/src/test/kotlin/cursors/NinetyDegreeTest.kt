package cursors


import cursors.context.*
import cursors.context.RowMajor.Companion.fixedWidthOf
import cursors.context.RowMajor.Companion.indexableOf
import cursors.io.*
import cursors.io.IOMemento.*
import cursors.io.Vect02_.left
import cursors.io.Vect02_.right
import cursors.macros.join
import cursors.macros.α
import org.junit.Test
import shouldBe
import vec.macros.*
import vec.util.toArray
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

class NinetyDegreeTest {


    /**
     * wrtie a fixed length networkendian binary fwf of a cursor.
     */
    @Test
    fun rewriteBinFwfRowMajor() {
        val coords = intArrayOf(
            0, 10,
            10, 84,
            84, 124,
            124, 164
        ).zipWithNext()
        val mf = MappedFile("src/test/resources/caven20.fwf")
        val nio = NioMMap(mf)
        val fixedWidth = fixedWidthOf(nio, coords)
        val indexable: Addressable = indexableOf(nio, fixedWidth)
        //we resample and pivot a source cursor
        val piv: Cursor = cursorOf(
            RowMajor().fromFwf(
                fixedWidth, indexable as Indexable, nio, Columnar(
                    vect0rOf(
                        IoLocalDate as TypeMemento,
                        IoString,
                        IoFloat,
                        IoFloat
                    ).zip(
                        vect0rOf<String?>("date", "channel", "delivered", "ret")
                    )
                )
            )
        ).resample(0).pivot(0.toArray(), 1.toArray(), intArrayOf(2, 3)) α floatFillNa(0f)
        val defaultVarcharSize = 64

        /**
         * open destination file
         */
        val createTempFile = File.createTempFile("ninetyDegreesTest1", ".bin")
        System.err.println("tmpfile is " + createTempFile.toPath())
        piv.writeISAM(createTempFile.absolutePath, defaultVarcharSize)
    }

    /**
     * wrtie a fixed length networkendian binary fwf of a cursor.
     */
    @Test
    fun shardCursor() {
        val coords = intArrayOf(
            0, 10,
            10, 84,
            84, 124,
            124, 164
        ).zipWithNext()
        val mf = MappedFile("src/test/resources/caven20.fwf")
        val nio = NioMMap(mf)
        val fixedWidth = fixedWidthOf(nio, coords)
        val indexable: Addressable = indexableOf(nio, fixedWidth)
        //we resample and pivot a source cursor
        val piv: Cursor = cursorOf(
            RowMajor().fromFwf(
                fixedWidth, indexable as Indexable, nio, Columnar(
                    vect0rOf(
                        IoLocalDate as TypeMemento,
                        IoString,
                        IoFloat,
                        IoFloat
                    ).zip(
                        vect0rOf<String?>("date", "channel", "delivered", "ret")
                    )
                )
            )
        ).resample(0).pivot(0.toArray(), 1.toArray(), intArrayOf(2, 3)) α floatFillNa(0f)

        val defaultVarcharSize = 64

        val fnList = piv.scalars.mapIndexedToList { i, scalar ->


            System.err.println("writing " + scalar.name)
            /**
             * open destination file
             */
            val createTempFile = File.createTempFile(
                "shardedPiv-" + scalar.name.replace(
                    "\\W+".toRegex(),
                    "_"
                ),
                ".bin"
            )
            System.err.println("tmpfile is " + createTempFile.toPath())
            val pathname = createTempFile.absolutePath
            piv[i].writeISAM(pathname)
            pathname
        }
        System.err.println(fnList)

        val handleMapThing: Vect02<FileChannel, Cursor> = fnList.α {
            val fc = FileChannel.open(Paths.get(it), StandardOpenOption.READ)
            fc t2 ISAMCursor(Paths.get(it), fc)
        }

        try {
            val c: Vect0r<Cursor> = handleMapThing.right
            val piv2 = join(c)


            for (i in 0 until piv.size) for (j in 0 until piv.scalars.size)
                (piv at i)[j].first shouldBe (piv2 at (i))[j].first

        } finally {
            try {
                handleMapThing.left.forEach { it.close() }
            } catch (_: Throwable) {
            }
        }

    }

    @Test
    fun readBinary() {
        val s1 = "ninetyDegreesTest19334425141920886859.bin"
        val s2 = s1 + ".meta"
        val binpath = Paths.get(
            "src",
            "test",
            "resources", s1
        )

        FileChannel.open(binpath, StandardOpenOption.READ).use { mf ->
            val cursr = ISAMCursor(binpath, mf)

            System.err.println((cursr at (0)).left.toList())
            System.err.println((cursr at (1)).left.toList())
            System.err.println((cursr at (2)).left.toList())
            System.err.println((cursr at (3)).left.toList())
        }
    }

}

