package cursors.io

import cursors.Cursor
import cursors.TypeMemento
import cursors.at
import cursors.context.*
import vec.macros.*
import vec.util.span
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.coroutines.CoroutineContext


@Suppress("USELESS_CAST")
fun ISAMCursor(
        binpath: Path,
        mappedFile: MappedFile,
        metapath: Path = Paths.get(binpath.toString() + ".meta")
) = mappedFile.run {
    val lines = Files.readAllLines(metapath)
    lines.removeIf { it.startsWith("# ") || it.isNullOrBlank() }
    val rcoords = lines[0].split("\\s+".toRegex()).α(String::toInt).zipWithNext() as Vect02<Int, Int>
    val rnames = lines[1].split("\\s+".toRegex()).toVect0r()
    val typeVec = lines[2].split("\\s+".toRegex()).α(IOMemento::valueOf)
    val recordlen = rcoords.last().second
    val drivers = NioMMap.binary(typeVec)
    val nio = NioMMap(this, drivers)
    val fixedWidth = FixedWidth(recordlen, rcoords, { null }, { null })
    val indexable: Addressable =
            RowMajor.indexableOf(nio, fixedWidth)
    cursorOf(
            RowMajor().fromFwf(
                    fixedWidth,
                    indexable as Indexable,
                    nio,
                    Columnar(typeVec.zip(rnames) as Vect02<TypeMemento, String?>)
            )
    )
}

/**
 * this writes a cursor values to a single Network-endian ByteBuffer translation and writes an additional filename+".meta"
 * file containing commented meta description strings
 *
 * this is a tempfile format until further notice, changes and fixes should be aggressively developed, there is no
 * RFC other than this comment to go by
 *
 */
fun Cursor.writeISAM(
        pathname: String,
        defaultVarcharSize: Int = 128,
        varcharSizes: Map<
                /**
                column*/
                Int,
                /**length*/
                Int>? = null
)= writeISAM2(pathname, defaultVarcharSize, varcharSizes)
/* {
    val mementos = scalars α Scalar::first

    val vec = scalars `→` { scalars: Vect0r<Scalar> ->
        Columnar.of(
                scalars
        ) t2 mementos
    }
    *//** create context columns *//*
    val (wcolumnar: Arity, ioMemos: Vect0r<TypeMemento>) = vec
    val sizes = varcharSizes ?: let { curs ->
        sequence {
            (curs at 0).right.toList().mapIndexed { index, it ->

                //our blackboard CoroutineCOntext  metadata function.
                val cc = it.invoke()
                (cc[Arity.arityKey] as? Scalar)?.let { (a) ->
                    if (a == IOMemento.IoString)
                        (cc[RecordBoundary.boundaryKey] as? FixedWidth)?.let { fw ->
                            yield((index to fw.recordLen))
                        }

                }
            }
        }.toMap()
    }

    val wcoords: Vect02<Int, Int> = networkCoords(ioMemos, defaultVarcharSize, sizes)
    val wrecordlen: Int = wcoords.right.last()
    MappedFile(
            pathname,
            "rw",
            FileChannel.MapMode.READ_WRITE
    ).use { mappedFile: MappedFile ->
        mappedFile.randomAccessFile.setLength(wrecordlen.toLong() * size)


        *//**
         * preallocate the mmap file
         *//*

        val drivers1 = Fixed.mapped[ioMemos]
        val wfixedWidth: RecordBoundary =
                FixedWidth(wrecordlen, wcoords, { null }, { null })

        writeISAMMeta(pathname, wcoords)
        *//**
         * nio object
         *//*
        val wnio: Medium = NioMMap(
                mappedFile,
                drivers1 as Array<CellDriver<ByteBuffer, Any?>>
        )
        wnio.recordLen = wrecordlen.`⟲`
        val windex: Addressable =
                RowMajor.indexableOf(
                        wnio as NioMMap,
                        wfixedWidth as FixedWidth
                )


        val wtable: TableRoot =  (
                windex +
                        wcolumnar +
                        wfixedWidth +
                        wnio +
                        RowMajor()
                ).let { coroutineContext: CoroutineContext ->
                    val wniocursor: NioCursor = wnio.values(coroutineContext)
                    val arity: Columnar = coroutineContext[Arity.arityKey] as Columnar
                    System.err.println("columnar memento: " + arity.left.toList())
                    wniocursor t2 coroutineContext
                }

//        val scalars: Vect0r<Scalar> = scalars
        val xsize = width
        val ysize = size

        for (y in 0 until ysize) {
            val rowVals = (this at y).left
            for (x in 0 until xsize) {
                val tripl3: Tripl3<() -> Any?, (Any?) -> Unit, Tripl3<CellDriver<ByteBuffer, *>, TypeMemento, Int>> =
                        wtable.first.get(x, y)
                val writefN: (Any?) -> Unit = tripl3.second
                val any: Any? = rowVals[x]
//                System.err.println("wfn: ($y,$x)=$any")
                writefN(any)
            }
        }
    }
}
*/
fun Cursor.writeISAMMeta(
        pathname: String,
//    wrecordlen: Int,
        wcoords: Vect02<Int, Int>
) {
    Files.newOutputStream(
            Paths.get(pathname + ".meta")
    ).bufferedWriter().use {

        fileWriter ->

        val s = this.scalars as Vect02<TypeMemento, String?>

        val coords = wcoords.toList()
                .map { listOf(it.first, it.second) }.flatten().joinToString(" ")
        val nama = s.right
                .map { s1: String? -> s1!!.replace(' ', '_') }.toList().joinToString(" ")
        val mentos = s.left
                .mapIndexed<TypeMemento, Any> { ix, it ->
                    if (it is IOMemento)
                        it.name else {
                        val pai2 = wcoords[ix]
                        pai2.span
                    }
                }.toList()

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