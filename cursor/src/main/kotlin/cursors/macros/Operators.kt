package cursors.macros

import cursors.Cursor
import cursors.io.RowVec
import cursors.io.left
import cursors.io.right
import vec.macros.*
import kotlin.experimental.ExperimentalTypeInference

/**
 * reducer func -- operator for sum/avg/mean etc. would be nice, but we have to play nice in a type-safe language so  ∑'s just a hint  of a reducer semantic
 */
inline fun Cursor.`∑`(crossinline reducer: (Any?, Any?) -> Any?): Cursor =
    Cursor(first) { iy: Int ->
        val aggcell: RowVec = second(iy)
        val al: Vect0r<*> = aggcell.left
        RowVec(aggcell.first) { ix: Int ->
            val ac = al[ix]
            val toList = (ac as? Vect0r<*>)?.toList()
            val iterable = toList ?: (ac as? Iterable<*>)
            val any1 = iterable?.reduce(reducer)
            val any = any1 ?: ac
            any t2 aggcell[ix].second
        }

    }

/**
 * reducer func
 */
inline infix fun Cursor.α(crossinline unaryFunctor: (Any?) -> Any?): Cursor =
    Cursor(first) { iy: Int ->
        val aggcell = second(iy)
        (aggcell.left α (unaryFunctor)).zip(aggcell.right)
    }

inline val <reified C : Vect0r<R>, reified R> C.`…`: List<R> get() = this.toList()

/** left identity */
@OptIn(ExperimentalTypeInference::class)
object `⟳` {
    @BuilderInference
    inline operator fun <reified T> invoke(f: T) = { f: T -> f }
}