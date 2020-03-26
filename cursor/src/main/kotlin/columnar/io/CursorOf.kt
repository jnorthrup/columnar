package columnar.io

import columnar.*
import columnar.context.Arity
import columnar.context.*
import columnar.io.*
import columnar.context.*
import columnar.context.Scalar
import columnar.macros.*
import kotlin.coroutines.*
import columnar.*
import columnar.context.*
import columnar.macros.*
import columnar.util.*
import columnar.io.*
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
                val name =
                    cnar.right.get(ix) ?: throw(InstantiationError("Tableroot's Columnar has no names"))
                val type = cnar.left[ix]
                Scalar(type, name)
            }
        }
    }
}