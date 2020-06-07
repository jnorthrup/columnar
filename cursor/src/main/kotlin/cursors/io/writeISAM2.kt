package cursors.io

import cursors.Cursor
import cursors.TypeMemento
import cursors.at
import cursors.context.*
import vec.macros.*
import vec.util.span
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

/**
 * this writes a cursor values to a single Network-endian ByteBuffer translation and writes an additional filename+".meta"
 * file containing commented meta description strings
 *
 * this is a tempfile format until further notice, changes and fixes should be aggressively developed, there is no
 * RFC other than this comment to go by
 *
 */
fun Cursor.writeISAM2(
        pathname: String,
        defaultVarcharSize: Int = 128,
        /**
         * optional map of columnIndex / length
         */
        varcharSizes: Map<  Int,  Int>? = null) {
    val mementos = scalars α Scalar::first
    val vec = scalars `→` { scalars: Vect0r<Scalar> ->
        Columnar.of(      scalars    ) t2 mementos
    }
    /** create context columns */
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

    writeISAMMeta(pathname, wcoords)

    val drivers : List<CellDriver<ByteBuffer,*>> = scala2s.left.map(Fixed.mapped::get).toList().filterNotNull()
    FileChannel.open(Paths.get(pathname), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE, StandardOpenOption.WRITE).use {
        fchannel ->

        val xsize = width
        val ysize = size
        for (y in 0 until ysize) {
            val row=this at y
            for(x in 0 until xsize){
                val s = scala2s.left[x]
                val byteArray = ByteArray(wcoords[x].span)
                val cellData = row.left[x]
                val write = drivers[x].write as (ByteBuffer,Any?)->Unit
                val wrap = ByteBuffer.wrap(byteArray)
                write(wrap , cellData  )
                fchannel.write(wrap.rewind() )

            }
        }
    }

}
