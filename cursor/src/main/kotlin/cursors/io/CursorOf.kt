package cursors.io

import cursors.Cursor
import cursors.context.Arity
import cursors.context.Columnar
import cursors.context.Scalar.Companion.Scalar
import cursors.io.Vect02_.left
import cursors.io.Vect02_.right
import vec.macros.*
import kotlin.collections.*
import kotlin.collections.component2
import kotlin.coroutines.CoroutineContext

fun cursorOf(root: TableRoot): Cursor = root.let { (nioc: NioCursor, crt: CoroutineContext): TableRoot ->
    val (xy: IntArray, mapper: (IntArray) -> Tripl3<() -> Any?, (Any?) -> Unit, NioMeta>) = nioc
    val (xsize: Int, ysize: Int) = xy
    vec.macros.Vect0r(ysize) { iy ->
        vec.macros.Vect0r(xsize) { ix ->
            val (a: () -> Any?) = mapper(intArrayOf(ix, iy))
            a() t2 {
                val cnar: Columnar =
                    crt[Arity.arityKey] as Columnar
                //todo define spreadsheet context linkage; insert a matrix of (Any?)->Any? to crt as needed
                // and call in a cell through here
                val name =
                    cnar.right[ix] ?: throw(InstantiationError("Tableroot's Columnar has no names"))
                val type = cnar.left[ix]
                Scalar(type, name)
            }
        }
    }
}