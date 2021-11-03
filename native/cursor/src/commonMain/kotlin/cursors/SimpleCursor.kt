package cursors

import cursors.context.Scalar
import vec.macros.*
import kotlin.coroutines.CoroutineContext

/**
 * this provides a tiny bit of extra safety for zero-length Cursors
 * problem: colidx wants to get scalars from empty Cursor
 *


 *
 *
 */
class SimpleCursor(
    val scalars: Vect0r<Scalar>,
    val data: Vect0r<Vect0r<out Any?>>,
    val o: Vect0r<() -> CoroutineContext> = scalars α { it -> { it } },
    val c: Pai2<Int, (Int) -> Vect02<Any?, () -> CoroutineContext>> = data α { it.zip(o) },
) : Cursor /*by c*/ {
    override val first: Int by data::size
    override val second: (Int) -> Pai2<Int, (Int) -> Pai2<Any?, () -> CoroutineContext>> by c::second
}
