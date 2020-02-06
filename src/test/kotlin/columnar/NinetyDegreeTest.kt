package columnar

//import io\.kotlintest\.specs\.StringSpec\n
import columnar.IOMemento.*
import columnar.context.*
import columnar.context.RowMajor.Companion.fixedWidthOf
import columnar.context.RowMajor.Companion.indexableOf
import java.io.File


class NinetyDegreeTest/* : StringSpec()*/ {
    val coords = vZipWithNext(
        intArrayOf(
            0, 10,
            10, 84,
            84, 124,
            124, 164
        )
    ) α { ints: IntArray -> Tw1nt(ints)  }
    val mf = MappedFile("src/test/resources/caven20.fwf")
    val nio = NioMMap(mf)

    val fixedWidth = fixedWidthOf(nio, coords)
    val indexable: Addressable = indexableOf(nio, fixedWidth)


    /**
     * wrtie a fixed length networkendian binary fwf of a cursor.
     */
    @org.junit.jupiter.api.Test
    fun rewriteBinFwfRowMajor() {
        //we resample and pivot a source cursor
        val piv: Cursor = cursorOf(
            RowMajor().fromFwf(
                fixedWidth, indexable as Indexable, nio, Columnar(
                     vect0rOf(
                        IoLocalDate as TypeMemento,
                        IoString,
                        IoFloat,
                        IoFloat
                    ),
                    vect0rOf<String>("date", "channel", "delivered", "ret")
                )
            )
        ).resample(0).pivot(0.toArray(), 1.toArray(), intArrayOf(2, 3)) α floatFillNa(0f)
        val defaultVarcharSize = 64

        /**
         * open destination file
         */
        val createTempFile = File.createTempFile("ninetyDegreesTest1", ".bin")
        System.err.println("tmpfile is " + createTempFile.toPath())
        piv.writeBinary(createTempFile.absolutePath, defaultVarcharSize)



    }

}

