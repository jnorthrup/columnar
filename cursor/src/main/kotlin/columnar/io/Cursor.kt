package columnar.io

import columnar.*
import columnar.context.*
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Paths

/**
 * this writes a cursor values to a single Network-endian ByteBuffer translation and writes an additional filename+".meta"
 * file containing commented meta description strings
 *
 * this is a tempfile format until further notice, changes and fixes should be aggressively developed, there is no
 * RFC other than this comment to go by
 *
 */
fun Cursor.writeBinary(
    pathname: String,
    defaultVarcharSize: Int = 128,
    varcharSizes: Map<
            /**
            column*/
            Int,
            /**length*/
            Int>? = null
) {
    val mementos = scalars α  Scalar::first

    val vec = scalars `→` { scalars: Vect0r<Scalar> ->
        Columnar.of(
            scalars
        ) t2 mementos
    }
    /** create context columns */
    val (wcolumnar: Arity,   ioMemos: Vect0r<TypeMemento>) = vec
    val wcoords = networkCoords(ioMemos, defaultVarcharSize, varcharSizes)
    val wrecordlen: Int = wcoords.right.last()
    MappedFile(
        pathname,
        "rw",
        FileChannel.MapMode.READ_WRITE
    ).use { mappedFile ->
        mappedFile.randomAccessFile.setLength(wrecordlen.toLong() * size)


        /**
         * preallocate the mmap file
         */

        val drivers1: Array<CellDriver<ByteBuffer, Any?>> =
            Fixed.mapped[ioMemos] as Array<CellDriver<ByteBuffer, Any?>>
        val wfixedWidth: RecordBoundary =
            FixedWidth(wrecordlen, wcoords, { null }, { null })

        writeMeta(pathname, wcoords)
        /**
         * nio object
         */
        val wnio: Medium =
            NioMMap(mappedFile, drivers1)
        wnio.recordLen = wrecordlen.`⟲`
        val windex: Addressable =
            RowMajor.indexableOf(
                wnio as NioMMap,
                wfixedWidth as FixedWidth
            )


        val wtable: TableRoot = /*runBlocking*/(
                windex +
                        wcolumnar +
                        wfixedWidth +
                        wnio +
                        RowMajor()
                ).let { coroutineContext ->
            val wniocursor: NioCursor = wnio.values(coroutineContext)
            val arity = coroutineContext[Arity.arityKey] as Columnar
            System.err.println("columnar memento: " + arity.left.toList())
            wniocursor t2 coroutineContext
        }

        val scalars = scalars
        val xsize = scalars.size
        val ysize = size

        for (y in 0 until ysize) {
            val rowVals = this.second(y).left
            for (x in 0 until xsize) {
                val tripl3 = wtable.first[x, y]
                val writefN = tripl3.second
                val any = rowVals[x]
//                System.err.println("wfn: ($y,$x)=$any")
                writefN(any)
            }
        }
    }
}

fun Cursor.writeMeta(
    pathname: String,
//    wrecordlen: Int,
    wcoords: Vect02<Int, Int>
) {
    Files.newOutputStream(
        Paths.get(pathname + ".meta")
    ).bufferedWriter().use {

            fileWriter ->

        val s: Vect02<TypeMemento, String?> = scalars as Vect02<TypeMemento, String?>

        val coords = wcoords.toList()
            .map { listOf(it.first, it.second) }.flatten().joinToString(" ")
        val nama = s.right
            .map { s1: String? -> s1!!.replace(' ', '_') }.toList().joinToString(" ")
        val mentos = s.left
            .mapIndexed<TypeMemento, Any> { ix, it -> if (it is IOMemento) it.name else wcoords[ix].size }.toList()
            .joinToString(" ")
        listOf(
            "# format:  coords WS .. EOL names WS .. EOL TypeMememento WS ..",
            "# last coord is the recordlen",
            coords,
            nama,
            mentos
        ).forEach { line ->
            fileWriter.write(line)
            fileWriter.newLine()
        }

    }
}