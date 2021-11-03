package cursors

import cursors.context.Scalar
import cursors.io.colIdx
import cursors.io.scalars
import ports.BitSet
import vec.macros.Pai2
import vec.macros.`⟲`
import vec.macros.size
import vec.macros.t2
import kotlin.coroutines.CoroutineContext


fun Cursor.asBitSet(): Cursor = run {
    val xsize = colIdx.size
    val r = BitSet(size * xsize)
    repeat(size) { iy ->
        val (_: Int, fOfX: (Int) -> Pai2<Any?, () -> CoroutineContext>) = this at iy
        repeat(xsize) { ix -> if (true == fOfX(ix).first) r[iy * xsize + ix] = true }
    }
    val (_: Int, b: (Int) -> Scalar) = scalars
    val prep: Array<() -> CoroutineContext> = Array(xsize) { (b(it)).`⟲` }
    size t2 { iy: Int ->
        xsize t2 { ix: Int ->
            r[iy * xsize + ix] t2 prep[ix]
        }
    }
}
