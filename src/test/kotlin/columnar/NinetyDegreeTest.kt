package columnar

import columnar.*
import columnar.IOMemento.*
import columnar.context.*
import columnar.context.RowMajor.Companion.fixedWidthOf
import columnar.context.RowMajor.Companion.indexableOf
//import io\.kotlintest\.specs\.StringSpec\n
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
    ) α ::Tw1nt
    val drivers = vect0rOf(
        IoLocalDate,
        IoString,
        IoFloat,
        IoFloat
    )
    val names = vect0rOf("date", "channel", "delivered", "ret")
    val mf = MappedFile("src/test/resources/caven20.fwf")
    val nio = NioMMap(mf)

    val fixedWidth = fixedWidthOf(nio, coords)
    val indexable: Addressable = indexableOf(nio, fixedWidth)
    val columnarArity = Columnar(drivers, names)
    val root = RowMajor().fromFwf(fixedWidth, indexable as Indexable, nio, columnarArity)


    /**
     * wrtie a fixed length networkendian binary fwf of a cursor.
     */
    fun `rewriteBinFwfRowMajor`() {

        //we resample and pivot a source cursor
        val cursor: Cursor = cursorOf(root).resample(0).pivot(0.toArray(), 1.toArray(), intArrayOf(2, 3))

        /** create context columns
         *
         */
        val (wcolumnar: Arity, ioMemos: Vect0r<IOMemento>) = cursor.scalars `→` { scalars: Vect0r<Scalar> ->
            Columnar.of(
                scalars
            ) t2 (scalars α Scalar::first)
        }


        /** fabricate  binary sized coords */
        val (wcoords: Vect0r<IntArray>, wrecordlen: Int) =
            ioMemos.toList().mapIndexed { i, ioMemento ->
                val pai2 = coords[i]
                ioMemento.networkSize ?: pai2.size
            }.let { wsizes ->
                val x: MutableList<Tw1n<Int>> = mutableListOf()
                wsizes.fold(Tw1n(0, 0)) { (s, e), wsz: Int ->
                    (e t2 (e + wsz)).also { x.add(it) }
                }

                wsizes.mapIndexed { index: Int, size: Int ->
                    val start = if (index == 0) 0 else wsizes[index - 1]
                    intArrayOf(start, start + size)
                }.toVect0r() t2 wsizes.also { System.err.println("sizes: ${it.toList()}") }.sum()


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
            mappedFile.randomAccessFile.setLength(wrecordlen.toLong() * cursor.size)
        }
        var mappedFile = MappedFile(createTempFile.absolutePath, "rw", FileChannel.MapMode.READ_WRITE)

        /**
         * preallocate the mmap file
         */

        val drivers1: Array<CellDriver<ByteBuffer, Any?>> =
            Fixed.mapped[ioMemos] as Array<CellDriver<ByteBuffer, Any?>>
        val wfixedWidth: RecordBoundary = FixedWidth(
            wrecordlen, wcoords, null.`⟲`, null.`⟲`
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
            wniocursor t2 coroutineContext1
        }

        val xsize = cursor.scalars.size
        val ysize = cursor.size

        for (y in 0 until ysize) {
            val rowVals = cursor.second(y).left
            for (x in 0 until xsize) {
                val wfn = wtable.first[y, x].second
                wfn(rowVals[x])
            }
        }
    }
}
