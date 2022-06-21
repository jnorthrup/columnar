@file:Suppress("OVERRIDE_BY_INLINE")

package cursors.io

import cursors.Cursor
import cursors.TypeMemento
import cursors.at
import cursors.context.*
import cursors.context.Scalar
import vec.macros.*
import vec.macros.Vect02_.left
import vec.macros.Vect02_.right
import vec.util.FibonacciReporter
import vec.util.debug
import vec.util.logDebug
import vec.util.span
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.*


@Suppress("USELESS_CAST")
class ISAMCursor(
        binpath: Path,
        val fc: FileChannel,
        metapath: Path = Paths.get("$binpath.meta".debug { logDebug { "using default Isam meta: $it" } }),
) : Cursor {
    override inline val first get() = size
    override val second: (Int) -> RowVec
    val width get() = drivers.size
    val drivers by lazy { NioMMap.binary(scalars.map(Scalar::first) as Vect0r<IOMemento>) }
    val recordlen: Int by lazy { rcoords.last.second }
    val rcoords: Vect02<Int, Int>
    val scalars: Vect0r<Scalar>
    val size get() = (fc.size() / recordlen).toInt()

    init {
        val lines = Files.readAllLines(metapath).apply { removeIf { it.startsWith("# ") || it.isNullOrBlank() } }
        rcoords = (lines[0].split("\\s+".toRegex()) α (String::toInt)).zipWithNext()
        val typeVec = run {
            val s = lines[2]
            val split = s.split("\\s+".toRegex())
            val res = split.α(IOMemento::valueOf)

            res
        }
        val rnames = lines[1].split("\\s+".toRegex()).toVect0r()
        scalars = (typeVec).zip(rnames, Scalar.Companion::invoke) /*as Vect0r<Scalar>*/

        fc.let { fileChannel ->

            second = { iy: Int ->
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
                        read1 t2 { this@ISAMCursor.scalars[ix] }
                    }
                }
            }
        }
    }
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
    var logger: FibonacciReporter? = null;


    val mementos = scalars.map(Scalar::first).toArray()
    val vec = Columnar.of(scalars) t2 mementos
    /** create context columns */
    val (_: Arity, ioMemos) = vec
    val sizes = varcharSizes ?: let { curs ->

        linkedMapOf<Int, Int>().apply {
            (curs at 0).right.`➤`.withIndex().onEach { (index, it) ->

                //our blackboard CoroutineCOntext  metadata function.
                val cc = it.invoke()
                (cc[Arity.arityKey] as? Scalar)?.let { (a) ->
                    if (a == IOMemento.IoString)
                        (cc[RecordBoundary.boundaryKey] as? FixedWidth)?.let { fw: FixedWidth ->
                            this.entries.add(AbstractMap.SimpleEntry(index, fw.recordLen))
                        }
                }
            }
        }
    }
    val wcoords: Vect02<Int, Int> = networkCoords(ioMemos, defaultVarcharSize, sizes)
    val reclen = wcoords.right.last
    writeISAMMeta(pathname, wcoords)
    val rowBuf = ByteBuffer.allocateDirect(reclen + 1)
    val bounceyBuff = ByteBuffer.allocate(reclen + 1)
    val drivers: List<CellDriver<ByteBuffer, *>> = colIdx.left.map(Fixed.mapped::get).toList().filterNotNull()
    try {
        FileChannel.open(
                Paths.get(pathname),
                // ExtendedOpenOption.DIRECT,  Current location of the bytebuffer (0) is not a multiple of the block size (4096)
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE
        )
    } catch (x: Exception) {
        logDebug { "falling back to non-direct file open. " + x.localizedMessage }
        FileChannel.open(
                Paths.get(pathname),
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE
        )
    }.use { fchannel ->

        val xsize = width
        val ysize = size
        debug {
            logger = FibonacciReporter(ysize)
        }
        for (y in 0 until ysize) {
            val row = this at y
            rowBuf.clear()
            for (x in 0 until xsize) {
                val cellData = row.left[x]
                (drivers[x].write as (ByteBuffer, Any?) -> Unit)(bounceyBuff.clear().limit(wcoords[x].span), cellData)
                rowBuf.put(bounceyBuff.flip())
            }
            fchannel.write(rowBuf.flip())
            logger?.let {it.report(y)?.let { System.err.println(it) }}
        }
    }
}

fun Cursor.writeISAMMeta(
        pathname: String,
        //wrecordlen: Int,
        wcoords: Vect02<Int, Int>,
) {
    Files.newOutputStream(
            Paths.get("$pathname.meta")
    ).bufferedWriter().use {

        fileWriter ->

        val s = this.scalars as Vect02<TypeMemento, String?>

        val coords = wcoords.toList()
                .map { listOf(it.first, it.second) }.flatten().joinToString(" ")
        val nama = s.right.map { s1: String? -> s1!!.replace(' ', '_') }.toList().joinToString(" ")
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
