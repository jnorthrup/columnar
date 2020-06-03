package cursors.context

import cursors.Cursor
import cursors.io.IOMemento
import vec.macros.*
import java.nio.ByteBuffer
import kotlin.coroutines.CoroutineContext

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
         * this does no quote escapes, handles no padding, and assumes row 0 is the header names
         */
        fun CsvLinesCursor(csvLines: Vect0r<String>, dt: Vect0r<IOMemento> = Pai2(Int.MAX_VALUE) { ix: Int -> IOMemento.IoString }): Cursor {
            val colnames = csvLines[0].split(",").toVect0r()
            val meta = colnames.zip(dt)
            val xSize = colnames.size


            return Cursor(csvLines.size - 1) { iy: Int ->
                val row = csvLines[iy + 1].split(",")
                Pai2(xSize) { ix: Int ->
                    meta[ix].let { (n, t) ->
                        val read = Tokenized.mapped[t]!!.read
                        val csvCell = row[ix].toByteArray()
                        val wrap = ByteBuffer.wrap(csvCell).rewind()
                        read(wrap) t2 { Scalar(t, n) }
                    }
                }
            }
        }

        /**
         * same as CsvLinesCursor but does the splits at creationtime.
         * this does no quote escapes, handles no padding, and assumes row 0 is the header names
         */
        fun CsvArraysCursor(csvLines1: Vect0r<String>, dt: Vect0r<IOMemento> = Pai2(Int.MAX_VALUE) { ix: Int -> IOMemento.IoString }): Cursor =
                csvLines1.let { l ->
                    val csvArrays = l.map { s: String -> s.split(",").toTypedArray() }


                    val colnames = csvArrays[0]
                    val meta = colnames.zip(dt.toArray())
                    val xSize = colnames.size


                    Cursor(csvArrays.size - 1) { iy: Int ->
                        val row = csvArrays[iy + 1]
                        Pai2(xSize) { ix: Int ->
                            meta[ix].let { (n, t) ->
                                val read = Tokenized.mapped[t]!!.read
                                val csvCell = row[ix].toByteArray()
                                val wrap = ByteBuffer.wrap(csvCell).rewind()
                                read(wrap) t2 { Scalar(t, n) }
                            }
                        }
                    }
                }
    }
}

class FixedWidth(
        val recordLen: Int,
        val coords: Vect02<Int, Int>,
        val endl: () -> Byte? = '\n'::toByte,
        val pad: () -> Byte? = ' '::toByte,
) : RecordBoundary()
