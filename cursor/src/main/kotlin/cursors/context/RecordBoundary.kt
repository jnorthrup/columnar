package cursors.context

import cursors.Cursor
import cursors.io.IOMemento
import vec.macros.*
import java.nio.ByteBuffer
import kotlin.coroutines.CoroutineContext
import kotlin.math.max

/**
 * context-level support class for Fixed or Tokenized Record boundary conditions.
 *
 *
 * This does not extend guarantees to cell-level definitions--
 * FWF cells are parsed strings, thus tokenized within fixed records, and may carry lineendings
 * csv records with fixed length fields is rare but has its place in aged messaging formats.
 * even fixed-record fixed-cell (e.g. purer ISAM) formats have to use external size variables for varchar
 *
 * it is assumed that record-level granularity and above are efficiently held in context details to perform outer loops
 * once the context variables are activated and conjoined per thier roles.
 *
 */
sealed class RecordBoundary : CoroutineContext.Element {
    companion object {
        val boundaryKey = object : CoroutineContext.Key<RecordBoundary> {}
    }

    override val key get() = boundaryKey
}


class TokenizedRow(val tokenizer: (String) -> List<String>) : RecordBoundary() {
    companion object {

        /**
         * same as CsvLinesCursor but does the splits at creationtime.
         * this does no quote escapes, handles no padding, and assumes row 0 is the header names
         */
        fun CsvArraysCursor(csvLines1: Iterable<String>, dt: Vect0r<IOMemento> = Pai2(Int.MAX_VALUE) { ix: Int -> IOMemento.IoString }): Cursor {
            lateinit var longest: IntArray
            lateinit var colnames: List<String>

            val csvArrays = csvLines1.mapIndexed { index, s ->

                val res = s.split(",").map { java.lang.String(it) as kotlin.String }

                if (index == 0) {
                    longest = IntArray(res.size) { 0 }
                    colnames = res
                    res
                } else
                    res.mapIndexed { i, s ->
                        val ioMemento = dt[i]
                        if (ioMemento == IOMemento.IoString) {
                            longest[i] = max(longest[i], s.length)
                            s
                        } else {
                            Tokenized.mapped[ioMemento]!!.read(ByteBuffer.wrap(s.toByteArray()).rewind())
                        }
                    }
            }

            val meta = colnames.toVect0r().zip(dt)
            val xSize = colnames.size
            return Cursor(csvArrays.size - 1) { iy: Int ->
                val row = csvArrays[iy + 1]
                Pai2(xSize) { ix: Int ->
                    row[ix] t2 {
                        if (IOMemento.IoString == dt[ix]) {
                            Scalar(type = dt[ix], name = colnames[ix] ) + FixedWidth(
                                    recordLen = longest[ix],
                                    coords = dummy,
                                    endl = { null },
                                    pad = { null },
                            ) //TODO: review whether using FixedWidth here is is a bad thing and we need a new Context Class for this feature.

                        } else Scalar(dt[ix], colnames[ix])
                    }
                }
            }
        }
    }
}

private val dummy = vect0rOf<Pai2<Int, Int>>()

class FixedWidth(
        val recordLen: Int,
        val coords: Vect02<Int, Int>,
        val endl: () -> Byte? = '\n'::toByte,
        val pad: () -> Byte? = ' '::toByte,
) : RecordBoundary()
