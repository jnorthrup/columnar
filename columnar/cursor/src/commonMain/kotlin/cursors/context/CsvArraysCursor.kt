package cursors.context

import cursors.Cursor
import cursors.TypeMemento
import cursors.io.IOMemento
import ports.ByteBuffer
import vec.macros.*
import vec.util._a

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
        dataTypes = (Array(colnames.size) { i -> overrides?.let { overrides[colnames[i]] } ?: dt1 [i] })
        res.also {
            csvLoopLambda = rowFetcher
        }
    }
    csvLoopLambda = metaFetcher

    val csvArrays = csvLines1.map { csvLoopLambda(it) }
    val xSize = colnames.size

    val sdt = dataTypes.mapIndexed { ix, dt ->
        //TODO: review whether using FixedWidth here is is a bad thing and we need a new Context Class for this feature.
                (if (IOMemento.IoString == dt) Scalar.Scalar(type = dt,
                    name = colnames[ix]) + RecordBoundary.FixedWidth(recordLen = longest[ix],
                    coords = dummy,
                    endl = { null },
                    pad = { null }) else Scalar.Scalar(dt, colnames[ix])).`⟲`
    }
    return  (csvArrays.size - 1) t2 { iy: Int ->
        val row = csvArrays[iy + 1]
         (xSize) t2  { ix: Int ->
            val function = sdt[ix]
            row[ix] t2 function
        }
    }
}