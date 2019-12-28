package columnar

import columnar.IOMemento.*
import columnar.context.Addressable
import columnar.context.Addressable.Indexable
import columnar.context.Arity
import columnar.context.Medium
import columnar.context.Medium.NioMMap
import columnar.context.Medium.NioMMap.Companion.text
import columnar.context.Ordering
import columnar.context.RecordBoundary.FixedWidth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


typealias writefn<M, R> = Function2<M, R, Unit>
typealias readfn<M, R> = Function1<M, R>


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
            val indexable = Indexable(size = { (nio.size() / nio.recordLen()).toInt() }, seek = nio.seek)

            val size1 = nio.size()
            assert(660L == size1)
            val size2 = indexable.size().toInt()
            assert(size2 == 4)
            nio.drivers = text(*columnarArity.type.toArray())

            val fourBy: suspend CoroutineScope.() -> Unit = {
                System.err.println(""+coroutineContext)
                val medium = coroutineContext[Medium.mediumKey]
                val size3 = (medium as NioMMap).size()
                val addressable = coroutineContext[Addressable.addressableKey]
                assert(addressable is Indexable)
                val size = (addressable as Indexable).size()
                assert(size == 4)

                val nioCursor = nio.values()
                nioCursor.let { (a, b) ->
                    System.err.println("" + a.toList())
                }
                System.err.println(nioCursor[3, 3].first())
                System.err.println(nioCursor[0, 0].first())
                System.err.println(nioCursor[1, 1].first())
                System.err.println(nioCursor[0, 0].first())
                System.err.println(nioCursor[0, 1].first())
                System.err.println(nioCursor[0, 2].first())
            }
runBlocking {      val coroutineScope = CoroutineScope(
    columnarArity +
            fixedWidth +
            indexable +
            nio + Ordering.RowMajor()
)
    coroutineScope.launch(block = fourBy).join() }
       runBlocking { val coroutineScope= CoroutineScope(
           columnarArity +
                   fixedWidth +
                   indexable +
                   nio    + Ordering.ColumnMajor()
       )
           coroutineScope.launch(block = fourBy).join() }


        }
    }
}

