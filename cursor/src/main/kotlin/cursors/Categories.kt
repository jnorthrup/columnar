package cursors

import cursors.context.Columnar
import cursors.context.Scalar
import cursors.io.*
import cursors.io.IOMemento.IoInt
import cursors.macros.join
import cursors.macros.α
import cursors.ml.DummySpec

import vec.macros.*
import vec.util.fib
import vec.util.logDebug
import java.util.*

/***
 *
 * this creates a one-hot encoding set of categories for each value in each column.
 *
 * every distinct (column,value) permutation is reified as (as of this comment, expensive) in-place pivot
 *
 * TODO: staple the catx values to the cursor foreheads
 *
 *
 * maybe this is faster if each (column,value) pair was a seperate 1-column cursor.  first fit for now.
 *
 * allocates IntArray per column, row length
 *
 */

fun Cursor.categories(
        /**
        if this is a index value, that value is omitted from columns. by default null is an omitted value.  if this is a DummySpec, the DummySpec specifies the index.
         */
        dummySpec: Any? = null,
): Cursor {
    var c = 1;
    var trigger = 0
    logDebug { "categories on cursor with ${scalars.size} columns and $size rows" }
    val cols = let { parent ->
        val cols = parent.scalars.toList().mapIndexed { i, (a, b) ->
            b t2 mutableMapOf<String, Int>() t3 IntArray(size)
        }
        for (iy in 0 until parent.size) {
            if (iy == trigger) logDebug { "step1 row $iy".also { trigger = fib(++c) } }
            val row = (parent at iy)
            cols.toList().mapIndexed { i, (nama, uniq, journal) ->
                if (iy == trigger)
                    logDebug { "step1a row $i $nama".also { trigger = fib(++c) } }
                val v = row.left[i].toString()
                journal[iy] == uniq.getOrPut(v, uniq::size)
            }
        }
        cols.mapIndexed { i, (nama, index, journal) ->
            if (i == trigger) logDebug { "step1 cleanup  col $i $nama elements: ${journal.size} uniq: ${index.keys.size}".also { trigger = fib(++c) } }

            nama t2 index.keys.toTypedArray() t3 journal
        }
    }
    c = 1
    val toBeJournaled: List<Cursor> = cols.mapIndexed { i, (nama, index, journal) ->
        if (i == trigger) logDebug { "step2 projection col  $i  $nama uniq: ${index.size} len: ${journal.size}".also { trigger = fib(++c) } }

        this.size t2 { iy: Int ->
            index.size t2 { ix: Int ->
                (if (journal[iy] == ix) 1 else 0) t2 { Scalar(IoInt, "$nama=${index[ix].replace("\\W+".toRegex(), "_")}") }
            }
        }
    }
    logDebug { "pre-join using dummyspec $dummySpec" }
    c = 1
    val toBeJoined: List<Cursor> = toBeJournaled.mapIndexed { index, curs: Cursor ->

        val s = curs.scalars.size
        (if (curs.scalars.size == 1) curs else curs[(0 until s) -
                when (dummySpec) {
                    is String -> {
                        curs.colIdx[dummySpec][0]
                    }
                    is Int -> {
                        dummySpec
                    }
                    DummySpec.Last -> {
                        curs.scalars.size - 1
                    }
                    else -> 0
                }]).also {

            logDebug { "pre-join result cursor $index columns: in ${curs.scalars.size}  out ${it.scalars.size}" }
        }
    }
    return join(*toBeJoined.toTypedArray())
}

fun Cursor.asBitSet(): Cursor = run {
//    val sc=scalars.map { (a,b)->   Scalar(IOMemento.IoBoolean, b).`⟲` {  }    }.toList()
    val xsize = scalars.size
    val r = BitSet(size * xsize)
    val tmp = this.α { b: Any? -> b == 1 }
    for (iy in 0 until size) {
        for (ix in 0 until xsize) {
            r[iy * ix] = (tmp at iy).left[ix] as Boolean
        }
    }
    size t2 { iy: Int ->
        xsize t2 { ix: Int ->
            r[iy * ix + ix] t2 { scalars[ix] }/* sc[ix] *///weirdness, if this is scalars[ix] this optimizes much better
        }
    }
}
