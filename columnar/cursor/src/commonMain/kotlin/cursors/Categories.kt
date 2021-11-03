package cursors

import cursors.context.Scalar
import cursors.io.colIdx
import cursors.io.scalars
import vec.macros.*
import kotlin.coroutines.CoroutineContext


fun Cursor.asBitSet(): Cursor = run {
    val xsize = colIdx.size
    val r = BitSet(size * xsize)
    repeat(size) { iy ->
        (this at iy).let { (_, function) ->
            repeat(xsize) { ix ->
                if (true == (function(ix).first as? Boolean))
                    r[iy * xsize + ix] = true
            }
        }
    }
    val (_: Int, b: (Int) -> Scalar) = scalars
    val prep: Array<() -> CoroutineContext> = Array(xsize) { (b(it)).`⟲` }
    size t2 { iy: Int ->
        xsize t2 { ix: Int ->
            r[iy * xsize + ix] t2 prep[ix]
        }
    }
}
