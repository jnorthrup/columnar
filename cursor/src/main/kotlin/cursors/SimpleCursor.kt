package cursors

import cursors.context.Scalar
import vec.macros.*
import kotlin.coroutines.CoroutineContext

/**
 * this provides a tiny bit of extra safety for zero-length Cursors
 * problem: colidx wants to get scalars from empty Cursor
 *
 * Exception in thread "main" java.lang.Exception: Open time not found in meta
at cursors.ByColNameKt.get(ByColName.kt:16)
at cursors.ByColNameKt.get(ByColName.kt:31)
at org.bereft.HistoricalCsvKt.decorateView(HistoricalCsv.kt:159)
at org.bereft.HistoricalCsvKt$main$7$invokeSuspend$$inlined$collect$1.emit(Collect.kt:136)
at kotlinx.coroutines.flow.SharedFlowImpl.collect(SharedFlow.kt:351)
at kotlinx.coroutines.flow.SharedFlowImpl$collect$1.invokeSuspend(SharedFlow.kt)
at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
at kotlinx.coroutines.DispatchedTask.run(DispatchedTask.kt:106)
at kotlinx.coroutines.EventLoopImplBase.processNextEvent(EventLoop.common.kt:274)

 *
 *
 */
class SimpleCursor(
    val scalars: Vect0r<Scalar>,
    val data: Vect0r<Vect0r<*>>,
    val o: Vect0r<() -> CoroutineContext> = scalars α { it -> { it } },
    val c: Pai2<Int, (Int) -> Vect02<Any?, () -> CoroutineContext>> = data α { it.zip(o) },
) : Cursor /*by c*/ {
    override val first: Int by c::first
    override val second: (Int) -> Pai2<Int, (Int) -> Pai2<Any?, () -> CoroutineContext>> by c::second
}
