package cursors.tbd

import cursors.Cursor
import cursors.at
import vec.macros.Pai2
import vec.macros.Vect02
import vec.macros.Vect0r
import kotlin.coroutines.CoroutineContext

 class MonoVal(val cell: Pai2<Any?, () -> CoroutineContext>) {
     fun ctxt(): CoroutineContext = cell.second()
     val m: Any? get() = cell.first

}

 class MonoRow(val row: Vect02<Any?, () -> CoroutineContext>) :
    Vect0r<MonoVal> {
    override  val first: Int get() = row.first
    override  val second: (Int) -> MonoVal
        get() = { ix: Int ->
            MonoVal(
                row.second(ix)
            )
        }
}

 class MonoCursor(val curs: Cursor) :
    Vect0r<MonoRow> {
    override  val first: Int get() = curs.first
    override  val second: (Int) -> MonoRow
        get() = { iy: Int ->
            MonoRow((curs at (iy)))
        }
}