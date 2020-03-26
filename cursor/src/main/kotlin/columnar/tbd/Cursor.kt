package columnar.tbd

import columnar.Cursor
import columnar.Pai2
import columnar.Vect02
import columnar.Vect0r
import kotlin.coroutines.CoroutineContext

inline class MonoVal(val cell: Pai2<Any?, () -> CoroutineContext>)   {
    inline fun ctxt(): CoroutineContext = cell.second()
    inline  val  m:Any? get()=cell.first

}

inline class MonoRow(val row: Vect02<Any?, () -> CoroutineContext>) :
    Vect0r<MonoVal> {
    override inline val first: Int get() = row.first
    override inline val second: (Int) -> MonoVal
        get() = { ix: Int ->
            MonoVal(
                row.second(ix)
            )
        }
}

inline class MonoCursor(val curs: Cursor) :
    Vect0r<MonoRow> {
    override inline val first: Int get() = curs.first
    override inline val second: (Int) -> MonoRow
        get() = { iy: Int ->
            MonoRow(curs.second(iy))
        }
}