package cursors

import cursors.io.*
import cursors.macros.α
import org.roaringbitmap.RoaringBitmap
import org.roaringbitmap.RoaringBitmapSupplier
import org.roaringbitmap.longlong.ImmutableLongBitmapDataProvider
import org.roaringbitmap.longlong.Roaring64Bitmap

import vec.macros.*
import java.util.*
import kotlin.coroutines.CoroutineContext



fun Cursor.asRBitSet(): Cursor = run {
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
