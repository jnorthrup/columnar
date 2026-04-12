package cursors.io


import cursors.Cursor
import cursors.TypeMemento
import cursors.context.Arity
import cursors.context.Columnar
import cursors.context.Scalar.Companion.Scalar
import vec.macros.*
import vec.macros.Vect02
import kotlin.coroutines.CoroutineContext

fun cursorOf(root: TableRoot): Cursor = root.let { (nioc: NioCursor, crt: CoroutineContext): TableRoot ->
    val (xy: IntArray, mapper: (IntArray) -> Tripl3<() -> Any?, (Any?) -> Unit, NioMeta>) = nioc
    val (xsize: Int, ysize: Int) = xy
    Vect0r(ysize) { iy ->
        Vect0r(xsize) { ix ->
            val (a: () -> Any?) = mapper(intArrayOf(ix, iy))
            a() t2 {
                val cnar: Columnar =
                    crt[Arity.arityKey] as Columnar
                //todo define spreadsheet context linkage; insert a matrix of (Any?)->Any? to crt as needed
                // and call in a cell through here
                val name: String =
                    ((cnar as Vect02<Nothing, String?>).right)[ix] ?: throw(InstantiationError("Tableroot's Columnar has no names"))
                val type =     ((cnar as Vect02<TypeMemento,Nothing >) . left )[ix]
                Scalar(type, name)
            }
        }
    }
}
