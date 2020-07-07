package cursors.io

import cursors.Cursor
import cursors.TypeMemento
import cursors.at
import cursors.context.*
import vec.macros.*
import vec.util.logDebug
import vec.util.span
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption


@Suppress("USELESS_CAST")
fun ISAMCursor(
        binpath: Path,
        fc: FileChannel,
        metapath: Path = Paths.get(binpath.toString() + ".meta"),
): Cursor = fc.let { fileChannel ->
    val lines = Files.readAllLines(metapath)
    lines.removeIf { it.startsWith("# ") || it.isNullOrBlank() }
    val rcoords = lines[0].split("\\s+".toRegex()).α(String::toInt).zipWithNext() as Vect02<Int, Int>
    val rnames = lines[1].split("\\s+".toRegex()).toVect0r()
    val typeVec = lines[2].split("\\s+".toRegex()).α(IOMemento::valueOf)
    val recordlen = rcoords.last().second
    val drivers: Array<CellDriver<ByteBuffer, Any?>> = NioMMap.binary(typeVec)

    Cursor((fc.size() / recordlen).toInt()) { iy: Int ->
        /**
         * this is a departure from the original windowed
         * ByteBuffer Nio harnasss
         */

        val row = ByteBuffer.allocate(recordlen)
        val read = fc.read(row.clear(), iy.toLong() * recordlen.toLong())

        RowVec(drivers.size) { ix: Int ->
            rcoords.get(ix).let { (b, e) ->
                val slice = row.limit(e).position(b).slice()
                val read1 = drivers[ix].read(slice)
                read1 t2 { Scalar(typeVec[ix], rnames[ix]) }
            }
        }
    }


    /*  val nio = NioMMap(this, drivers)
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
      )*/
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
                Int>? = null,
) {
    val mementos = scalars α Scalar::first
    val vec = scalars `→` { scalars: Vect0r<Scalar> ->
        Columnar.of(scalars) t2 mementos
    }
    /** create context columns */
    val (_: Arity, ioMemos ) = vec
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
    val wcoords: Vect02<Int, Int> = networkCoords(ioMemos.toArray(), defaultVarcharSize, sizes)
    val reclen = wcoords.right.last()
    writeISAMMeta(pathname, wcoords)
    val rowBuf = ByteBuffer.allocateDirect(reclen + 1)
    val bounceyBuff = ByteBuffer.allocate(reclen + 1)
    val drivers: List<CellDriver<ByteBuffer, *>> = colIdx.left.map(Fixed.mapped::get).toList().filterNotNull()
    try {
        FileChannel.open(Paths.get(pathname), /*ExtendedOpenOption.DIRECT, */StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
    } catch (x: Exception) {
        logDebug { "falling back to non-direct file open. " + x.localizedMessage }
        FileChannel.open(Paths.get(pathname), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
    }.use { fchannel ->

        val xsize = width
        val ysize = size
        for (y in 0 until ysize) {
            val row = this at y
            rowBuf.clear()
            for (x in 0 until xsize) {
                val cellData = row.left[x]
                (drivers[x].write as (ByteBuffer, Any?) -> Unit)(bounceyBuff.clear().limit(wcoords[x].span), cellData)
                rowBuf.put(bounceyBuff.flip())
            }
            fchannel.write(rowBuf.flip())
        }
    }
}

fun Cursor.writeISAMMeta(
        pathname: String,
        //wrecordlen: Int,
        wcoords: Vect02<Int, Int>,
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
        val mentos = s.left.toArray().mapIndexed<TypeMemento, Any> { ix, it ->
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

