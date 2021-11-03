package cursors.macros

import cursors.Cursor
import cursors.at
import cursors.io.RowVec
import vec.macros.*
import vec.macros.Vect02_.left
import vec.macros.Vect02_.right

/**
 * reducer func -- operator for sum/avg/mean etc. would be nice, but we have to play nice in a type-safe language so  ∑'s just a hint  of a reducer semantic
 */
infix fun Cursor.`∑`(reducer: (Any?, Any?) -> Any?): Cursor = Cursor(first) { iy: Int ->
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
 * tr func
 */
infix fun Cursor.α(unaryFunctor: (Any?) -> Any?): Cursor = run {
    size t2 { iy: Int ->
        val row: RowVec = (this at iy)
        (row.left α (unaryFunctor)).zip(row.right)
    }
}

inline val <reified R, V : Vect0r<R>> V.`…` get() = this.toList()
