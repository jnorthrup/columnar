package columnar

//import io\.kotlintest\.specs\.StringSpec\n
import columnar.IOMemento.*
import columnar.context.*
import columnar.context.RowMajor.Companion.fixedWidthOf
import columnar.context.RowMajor.Companion.indexableOf
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.FileChannel


class NinetyDegreeTest/* : StringSpec()*/ {
    val coords = vZipWithNext(
        intArrayOf(
            0, 10,
            10, 84,
            84, 124,
            124, 164
        )
    ) α { ints: IntArray -> Tw1nt(ints)  /*not fail*/ }/*.map { ints: IntArray -> Tw1nt(ints)  /*not fail*/ } */ /*.map(::Tw1nt) fail */ /* α ::Tw1nt fail*/
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
                        IoLocalDate,
                        IoString,
                        IoFloat,
                        IoFloat
                    ), vect0rOf("date", "channel", "delivered", "ret")
                )
            )
        ).resample(0).pivot(0.toArray(), 1.toArray(), intArrayOf(2, 3))

        /** create context columns
         *
         */
        val (wcolumnar: Arity, ioMemos: Vect0r<IOMemento>) = piv.scalars `→` { scalars: Vect0r<Scalar> ->
            Columnar.of(
                scalars
            ) t2 (scalars α Scalar::first)
        }
        val defaultVarcharSize = 64

        val sizes = ioMemos α { ioMemento: IOMemento ->
            ioMemento.networkSize ?: defaultVarcharSize
        }
        //todo: make IntArray Tw1nt Matrix
        var wrecordlen = 0

        val wcoords = Array(sizes.size) {
            val size = sizes[it]
            Tw1n(wrecordlen, (wrecordlen + size).also { wrecordlen = it })
        }
        System.err.println("wcoords:" + wcoords.toList().map { (a, b) -> a to b })
        /**
         * open destination file
         */
        val createTempFile = File.createTempFile("ninetyDegreesTest1", ".fwf")
        System.err.println("tmpfile is " + createTempFile.toPath())
        /**
         * a stutter for now, resize the fil;e, close, and then come back to it again
         */
        MappedFile(createTempFile.absolutePath, "rw", FileChannel.MapMode.READ_WRITE).use { mappedFile ->
            mappedFile.randomAccessFile.setLength(wrecordlen.toLong() * piv.size)
        }
        val mappedFile = MappedFile(createTempFile.absolutePath, "rw", FileChannel.MapMode.READ_WRITE)

        /**
         * preallocate the mmap file
         */

        val drivers1: Array<CellDriver<ByteBuffer, Any?>> =
            Fixed.mapped[ioMemos] as Array<CellDriver<ByteBuffer, Any?>>
        val wfixedWidth: RecordBoundary = FixedWidth(
            wrecordlen, wcoords α { tw1nt: Tw1nt -> tw1nt.ia }, null.`⟲`, null.`⟲`
        )

        /**
         * nio object
         */
        val wnio: Medium = NioMMap(mappedFile, drivers1)
        wnio.recordLen = wrecordlen.`⟲`
        val windex: Addressable = indexableOf(wnio as NioMMap, wfixedWidth as FixedWidth)


        //val wnioCursor:             NioCursor

        val wtable: TableRoot = runBlocking(
            windex +
                    wcolumnar +
                    wfixedWidth +
                    wnio +
                    RowMajor()
        ) {
            val wniocursor = wnio.values()
            val coroutineContext1 = this.coroutineContext
            val arity = coroutineContext1[Arity.arityKey] as Columnar
            val first = System.err.println("columnar memento: " + arity.first.toList())
            wniocursor t2 coroutineContext1
        }

        val scalars = piv.scalars
        val xsize = scalars.size
        val ysize = piv.size

        for (y in 0 until ysize) {
            val rowVals = piv.second(y).left
            for (x in 0 until xsize) {
                val tripl3 = wtable.first[x, y]
                val writefN = tripl3.second
                val any = rowVals[x]
                System.err.println("wfn: ($y,$x)")
                writefN(any)
            }
        }
    }
}
