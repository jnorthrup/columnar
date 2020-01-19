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
    val readOrdering = RowMajor()
    val fixedWidth = fixedWidthOf(nio, coords)
    val indexable = indexableOf(nio, fixedWidth)
    val columnarArity = Columnar(drivers, names)
    val root = RowMajor().fromFwf(fixedWidth, indexable, nio, columnarArity)

    init {

        "rewrite" {
            val cursor: Cursor = cursorOf(root).resample(0).pivot(0.toArray(), 1.toArray(), intArrayOf(2, 3))

            val resample = cursor.resample(0)
            val columnar = Columnar.of(resample.scalars)


            val createTempFile = File.createTempFile("ninetyDegreesTest", ".fwf")
            val mappedFile = MappedFile(createTempFile.absolutePath, "rw", FileChannel.MapMode.READ_WRITE)
            val drivers1: Array<CellDriver<ByteBuffer, Any?>> =
                Fixed.mapped[columnar.first] as Array<CellDriver<ByteBuffer, Any?>>
            val wnio = NioMMap(mappedFile, drivers1)
//            val writeOrdering = ColumnMajor()

        }
    }
}

