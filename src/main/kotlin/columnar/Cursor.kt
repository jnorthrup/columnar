package columnar

import columnar.IOMemento.*
import columnar.context.Addressable
import columnar.context.Arity
import columnar.context.CellDriver
import columnar.context.CellDriver.Companion.Fixed
import columnar.context.Medium.NioMMap
import columnar.context.Ordering
import columnar.context.RecordBoundary.FixedWidth
import kotlinx.coroutines.*
import java.nio.ByteBuffer
import kotlin.coroutines.EmptyCoroutineContext


typealias writefn<M, R> = Function2<M, R, Unit>
typealias readfn<M, R> = Function1<M, R>

typealias NioCursor = Matrix<Triple<() -> Any, (Any?) -> Unit, Triple<CellDriver<ByteBuffer, Any?>, IOMemento, Int>>>


/**
 * the Cursos attributes appear to be interdependent on each other's advantages.
 *
 * if this is to be a  trait system, the functional objects need to look like a blackboard
 */
suspend fun main() {
    runBlocking {
        MappedFile("src/test/resources/caven4.fwf").use { mf ->
            val columnarArity = Arity.Columnar.of(
                listOf(
                    "date" to IoLocalDate,
                    "channel" to IoString,
                    "delivered" to IoFloat,
                    "ret" to IoFloat
                )
            )
            val nio = NioMMap(mf)
            val fixedWidth = FixedWidth(
                nio.recordLen(),
                arrayOf((0 to 10), (10 to 84), (84 to 124), (124 to 164)).map {
                    it.toList().toIntArray()
                }.toVect0r()
            )
            val indexable = Addressable.Indexable(size = { (nio.recordLen() / nio.size()).toInt() }, seek = nio.seek)
            val rowMajor = Ordering.RowMajor()

            /**
             * for java readers,  these elements are same as reifiable threadlocals
             */
            val coroutineContext =
                EmptyCoroutineContext +
                        columnarArity +
                        fixedWidth +
                        indexable +
                        rowMajor + nio

            val newCoroutineContext = newCoroutineContext(
                columnarArity +
                        fixedWidth +
                        indexable +
                        rowMajor + nio
            )
            CoroutineScope(newCoroutineContext).launch {
                this.ensureActive()
                 rowMajor.nioCursor().let{
                     (a,b )->
                     System.err.println(""+a.toList())
                 }
            }.join()

        }
    }
}


