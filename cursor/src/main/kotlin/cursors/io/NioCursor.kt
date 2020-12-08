package cursors.io

import cursors.TypeMemento
import cursors.context.CellDriver
import vec.macros.*
import java.nio.ByteBuffer
import kotlin.coroutines.CoroutineContext

typealias NioMeta = Tripl3<CellDriver<ByteBuffer, Any?>, TypeMemento, Int>
typealias NioCursor = Matrix<Tripl3<() -> Any?, (Any?) -> Unit, NioMeta>>
typealias TableRoot = Pai2<NioCursor, CoroutineContext>
typealias ColMeta = Pai2<String, IOMemento>
typealias RowMeta = Vect0r<ColMeta>
typealias RowVec = Vect02<Any?, () -> CoroutineContext>

typealias writefn<M, R> = Function2<M, R, Unit>
typealias readfn<M, R> = Function1<M, R>


fun stringOf(it: RowVec) = it.left.toList().map { any ->
    val isVec = any as? Vect0r<*>
    val any1 = isVec?.toList() ?: any
    any1
}.toString()

val <F, S> Vect02<F, S>.left get() = this.α(Pai2<F, S>::first)
val <F, S> Vect02<F, S>.right get() = this α Pai2<F, S>::second
val <F, S> Vect02<F, S>.reify get() = this α Pai2<F, S>::pair


val <F, S, T> V3ct0r<F, S, T>.x get() = this α (XYZ<F, S, T>::first)
val <F, S, T> V3ct0r<F, S, T>.y get() = this α (XYZ<F, S, T>::second)
val <F, S, T> V3ct0r<F, S, T>.z get() = this α (XYZ<F, S, T>::third)
val <F, S, T> V3ct0r<F, S, T>.r3ify get() = this α (XYZ<F, S, T>::triple)

