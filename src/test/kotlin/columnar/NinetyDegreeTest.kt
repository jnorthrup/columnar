package id.rejuve.columnar

import columnar.*
import columnar.IOMemento.*
import columnar.context.*
import columnar.context.RowMajor.Companion.fixedWidthOf
import columnar.context.RowMajor.Companion.indexableOf
import io.kotlintest.specs.StringSpec
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.FileChannel


class NinetyDegreeTest : StringSpec() {
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
    val names = vect0rOf("date", "channel", "delivered", "ret")
    val mf = MappedFile("src/test/resources/caven20.fwf")
    val nio = NioMMap(mf)

    val fixedWidth = fixedWidthOf(nio, coords)
    val indexable = indexableOf(nio, fixedWidth)
    val columnarArity = Columnar(drivers, names)
    val root = RowMajor().fromFwf(fixedWidth, indexable, nio, columnarArity)

    init {

        /**
         * wrtie a fixed length networkendian binary fwf of a cursor.
         */
        "rewriteBinFwfRowMajor" {

            //we resample and pivot a source cursor
            val cursor: Cursor = cursorOf(root).resample(0).pivot(0.toArray(), 1.toArray(), intArrayOf(2, 3))

            /** create context columns
             *
             */
            val (   columnar,ioMemos) = cursor.scalars `→` {it->   Columnar.of(it) t2 (it α Scalar::first) }

            /**
             * open destination file
             */
            val createTempFile = File.createTempFile("ninetyDegreesTest1", ".fwf")
            val mappedFile = MappedFile(createTempFile.absolutePath, "rw", FileChannel.MapMode.READ_WRITE)

            // fabricate binary sized coords

            val (wcoords ,wrecordlen) =
                ioMemos.toList().mapIndexed { i, ioMemento -> ioMemento.networkSize ?: coords[i].size }.let { wsizes ->
                    wsizes.mapIndexed { index: Int, size: Int ->
                        val start = if (index == 0) 0 else wsizes[index - 1]
                        start t2 start + size

                    } t2 wsizes.sum()
                }



            val drivers1: Array<CellDriver<ByteBuffer, Any?>> =
                Fixed.mapped[ioMemos] as Array<CellDriver<ByteBuffer, Any?>>

            val wnio = NioMMap(mappedFile, drivers1)

//
//            val wtr:TableRoot = runBlocking(
//                RowMajor() +
//                        fixedWidth +
//                        indexable +
//                        wnio +
//                        columnarArity
//            ) { Pai2(nio.values(), this.coroutineContext) }


        }
    }
}

