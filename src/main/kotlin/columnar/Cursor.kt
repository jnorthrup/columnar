package columnar

import columnar.IOMemento.*
import columnar.context.*
import columnar.context.Addressable.Indexable
import columnar.context.Medium.NioMMap
import columnar.context.RecordBoundary.FixedWidth
import kotlinx.coroutines.*
import java.nio.ByteBuffer


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
            val recordLen = nio.recordLen()
            val fixedWidth = FixedWidth(
                recordLen,
                arrayOf((0 to 10), (10 to 84), (84 to 124), (124 to 164)).map {
                    it.toList().toIntArray()
                }.toVect0r()
            )
            val indexable = Indexable(size = {
                val recordLen1 = nio.recordLen()
                val size = nio.size()
                val toInt = (size/recordLen1).toInt()
                toInt
            }, seek = nio.seek)
            val rowMajor = Ordering.RowMajor()

            val size1 = (nio as NioMMap).size()
            assert(660L== size1)
            val size2 = indexable.size().toInt()
            assert(size2 ==4 )


            CoroutineScope(columnarArity +
                    fixedWidth +
                    indexable +
                    rowMajor + nio).launch {

                val medium = coroutineContext[Medium.mediumKey]
                val size3 = (medium as NioMMap).size()
                val addressable = coroutineContext[Addressable.addressableKey]
                assert(addressable is Indexable)
                val size = (addressable as Indexable).size()
                assert(size ==4)

                rowMajor.nioCursor().let { (a, b) ->
                    System.err.println("" + a.toList())
                }
            }.join()

        }
    }
}


