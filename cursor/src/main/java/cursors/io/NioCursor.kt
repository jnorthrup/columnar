package cursors.io

import cursors.TypeMemento
import cursors.context.CellDriver
import vec.macros.*
import vec.macros.Vect02_.left
import java.nio.ByteBuffer
import kotlin.coroutines.CoroutineContext

typealias NioMeta = Tripl3<CellDriver<ByteBuffer, Any?>, TypeMemento, Int>
typealias NioCursor = Matrix<Tripl3<() -> Any?, (Any?) -> Unit, NioMeta>>
typealias TableRoot = Pai2<NioCursor, CoroutineContext>
typealias ColMeta = Pai2<String, IOMemento>
typealias RowMeta = Vect0r<ColMeta>
typealias CellMeta = () -> CoroutineContext
typealias RowVec = Vect02<Any?, CellMeta>

typealias writefn<M, R> = Function2<M, R, Unit>
typealias readfn<M, R> = Function1<M, R>


fun stringOf(it: RowVec) = it.left.toList().map { any ->
    val isVec = any as? Vect0r<*>
    val any1 = isVec?.toList() ?: any
    any1
}.toString()
