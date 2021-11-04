package cursors.context

import cursors.Cursor
import cursors.TypeMemento
import cursors.context.Scalar.Companion.Scalar
import cursors.io.IOMemento
import ports.ByteBuffer
import vec.macros.*
import vec.util._a
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmStatic

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
sealed class RecordBoundary : CoroutineContext. Element {
    companion object {
        val boundaryKey = object : CoroutineContext.Key<RecordBoundary> {}
    }

    override val key get() = boundaryKey
}


class TokenizedRow(val tokenizer: (String) -> List<String>) : RecordBoundary()

private val dummy = vect0rOf<Pai2<Int, Int>>()

class FixedWidth(
    val recordLen: Int,
    val coords: Vect02<Int, Int>,
    val endl: () -> Byte? = { '\n'.code.toByte() },
    val pad: () -> Byte? = { ' '.code.toByte() },
) : RecordBoundary()

/**
 * same as CsvLinesCursor but does the splits at creationtime.
 * this does no quote escapes, handles no padding, and assumes row 0 is the header names
 */
fun CsvArraysCursor(
    csvLines1: Iterable<String>,
    dt1: Vect0r<TypeMemento> = Vect0r(Int.MAX_VALUE) { ix: Int -> IOMemento.IoString },
    overrides: Map<String, TypeMemento>? = null,
): Cursor {
    lateinit var longest: IntArray
    var colnames = _a[""]
    lateinit var dataTypes: Array<TypeMemento>
    val regex = "\\s*,\\s*".toRegex()

    /**
     cheeky self replacing lambda for kicks, but also because first row is exceptional.
     */
    lateinit var csvLoopLambda:(  String)->List<String>
    val rowFetcher = { s: String ->
        s.split(regex, dataTypes.size).let { res ->
            res.mapIndexed { i, s ->
                when (val ioMemento = dataTypes[i]) {
                    IOMemento.IoString -> s.also { if (s.length > longest[i]) longest[i] = s.length }
                    else -> Tokenized.mapped[ioMemento]!!.read(ByteBuffer.wrap(s.encodeToByteArray())
                        .rewind())
                }
            }
            res
        }
    }
    val metaFetcher = { s: String ->
        val res = s.split(regex).map { "$it" }
        longest = IntArray(res.size) { 0 }
        colnames = res.toTypedArray()
        dataTypes = (Array(colnames.size) { i -> overrides?.let { overrides[colnames[i]] } ?: dt1[i] })
        res.also {
            csvLoopLambda = rowFetcher
        }
    }
    csvLoopLambda = metaFetcher

    val csvArrays = csvLines1.map (csvLoopLambda)
    val xSize = colnames.size

    val sdt = dataTypes.mapIndexed { ix, dt ->
        //TODO: review whether using FixedWidth here is is a bad thing and we need a new Context Class for this feature.
                (if (IOMemento.IoString == dt) Scalar(type = dt,
                    name = colnames[ix]) + FixedWidth(recordLen = longest[ix],
                    coords = dummy,
                    endl = { null },
                    pad = { null }) else Scalar(dt, colnames[ix])).`⟲`
    }
    return  (csvArrays.size - 1) t2 { iy: Int ->
        val row = csvArrays[iy + 1]
         (xSize) t2  { ix: Int ->
            val function = sdt[ix]
            row[ix] t2 function
        }
    }
}