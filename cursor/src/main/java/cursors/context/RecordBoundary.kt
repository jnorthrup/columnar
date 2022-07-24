package cursors.context

import cursors.Cursor
import cursors.TypeMemento
import cursors.context.Scalar.Companion.Scalar
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
        val boundaryKey: CoroutineContext.Key<RecordBoundary> = object : CoroutineContext.Key<RecordBoundary> {}
    }

    override val key: CoroutineContext.Key<RecordBoundary> get() = boundaryKey
}


class TokenizedRow(val tokenizer: (String) -> List<String>) : RecordBoundary() {
    companion object {
        /**
         * same as CsvLinesCursor but does the splits at creationtime.
         * this does no quote escapes, handles no padding, and assumes row 0 is the header names.
         *
         * This Cursor scalar meta contains a compound CoroutineContext holding the max-length of Strings found in each
         * column. FixedWidth sealed class
         */
        @JvmStatic
        @JvmOverloads
        fun CsvArraysCursor(
                csvLines1: Iterable<String>,
                dt1: Vect0r<IOMemento> = Vect0r(Int.MAX_VALUE) { IOMemento.IoString },
                overrides: Map<String, TypeMemento>? = null,
                delimRegexFragment: Regex = ("\\s*,\\s*").toRegex(),
        ): Cursor {
            lateinit var longest: IntArray
            var colnames: Array<String>
            lateinit var dt: Array<TypeMemento>

            val iter = csvLines1.iterator()

            run {
                val s = iter.next()
                val res = s.split(delimRegexFragment).map { "$it" }.toTypedArray()
                longest = IntArray(res.size)
                colnames = res
                dt = Array(colnames.size) { i ->
                    overrides?.let { it[colnames[i]] } ?: dt1[i]
                }
            }

            val csvArrays = mutableListOf<Vect0r<*>>()
            if (iter.hasNext())
                do {
                    val s = iter.next()
                    csvArrays += s.split(delimRegexFragment, dt.size).map { it }.toTypedArray().mapIndexed { i, s ->
                        when (val ioMemento = dt[i]) {
                            IOMemento.IoString -> {
                                longest[i] = max(longest[i], s.length)
                                s
                            }
                            else -> Tokenized.mapped[ioMemento]!!.read(ByteBuffer.wrap(s.toByteArray()).rewind())
                        }
                    }.toVect0r()
                } while (iter.hasNext())

            val sdt = dt.mapIndexed { ix, dt ->
                (if (IOMemento.IoString == dt) {
                    Scalar(type = dt, name = colnames[ix]) + FixedWidth(
                            recordLen = longest[ix],
                            coords = dummy,
                            endl = { null },
                            pad = { null }
                    ) //TODO: review whether using FixedWidth here is is a bad thing and we need a new Context Class for this feature.

                } else Scalar(dt, colnames[ix])).`⟲`
            }.toVect0r()

            return csvArrays α { it zip sdt }
        }
    }
}

private val dummy = vect0rOf<Pai2<Int, Int>>()

class FixedWidth(
        val recordLen: Int,
        val coords: Vect02<Int, Int>,
        val endl: () -> Byte? = { '\n'.code.toByte() },
        val pad: () -> Byte? = { ' '.code.toByte() },
) : RecordBoundary()
