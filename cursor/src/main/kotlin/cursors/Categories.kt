package cursors

import cursors.context.Scalar.Companion.Scalar
import cursors.io.*
import cursors.io.Vect02_.left
import cursors.io.Vect02_.right
import cursors.macros.join
import cursors.ml.DummySpec
import vec.macros.*
import vec.util._a
import vec.util._v
import java.util.*
import kotlin.coroutines.CoroutineContext

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
fun Cursor.categories(dummySpec: Any? = null) = let { (psize, prows) ->
    val colIdx = this.colIdx
    val (csize) = colIdx
    val typeMementos: Vect0r<TypeMemento> = colIdx.left.map { it -> it as TypeMemento }
    val cnames = colIdx.right
    val alwaysZero = _a[0]


    Array(csize) { ix: Int ->

        val narrowed = this[ix]
        val (osize, oval) = narrowed.ordered(alwaysZero, IOMemento.listComparator(_v[typeMementos[ix]]))

        (0 until osize).map { oval(it).second(0).first }.distinct()./*toTypedArray().*/let { v ->

            Cursor(psize) { iy: Int ->

                val element = (narrowed at (iy))[0].first
                val useIndex = v.indexOf(element)

                RowVec(v.size) { vx: Int ->
                    (useIndex == vx) as Any? t2 { Scalar(
                            IOMemento.IoBoolean,
                            "${narrowed.scalars[0].second}=${v[vx]}"
                        ) as CoroutineContext
                    }
                }
            }
        }.let { curs ->
            val s = curs.colIdx.size
            when {
                s == 1 || dummySpec == DummySpec.KeepAll -> curs
                else -> curs[(0 until s) -
                        when (dummySpec) {
                            is String -> curs.colIdx[dummySpec][0]
                            is Int -> dummySpec
                            DummySpec.Last -> curs.scalars.size - 1
                            else -> 0
                        }]
            }
        }
    }.let { join(it.size t2 it::get) }
}

fun Cursor.asBitSet(): Cursor = run {
    val xsize = colIdx.size
    val r = BitSet(size * xsize)
    repeat(size) { iy ->
        (this at iy).let { (_, function) ->
            repeat(xsize) { ix ->
                if (true == (function(ix).first as? Boolean)) {
                    r[iy * xsize + ix] = true
                }
            }
        }
    }
    val (_, b) = scalars
    val prep: Array<() -> CoroutineContext> = Array(xsize) { (b(it)).`⟲` }
    size t2 { iy: Int ->
        xsize t2 { ix: Int ->
            r[iy * xsize + ix] t2 prep[ix]
        }
    }
}
