package columnar

import columnar.IOMemento.*
import columnar.context.*
import columnar.context. Indexable
import columnar.context.NioMMap.Companion.text
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
    val mapping = listOf(
        "date" to IoLocalDate,
        "channel" to IoString,
        "delivered" to IoFloat,
        "ret" to IoFloat
    )
    val coords = vect0rOf((0 to 10), (10 to 84), (84 to 124), (124 to 164)).map {
        it.toList().toIntArray()
    }.toVect0r()

    val filename = "src/test/resources/caven4.fwf"
    runBlocking {
        MappedFile(filename).use { mf ->
            val columnarArity = Columnar.of(mapping)
            val nio = NioMMap(mf)
             val fixedWidth = FixedWidth(recordLen = ({
                 nio.mf.mappedByteBuffer.duplicate().clear().run {
                     run {
                         while (get() != '\n'.toByte());
                         position()
                     }
                 }
             })(), coords = coords)
            val indexable = Indexable(size = (({ nio.mf.randomAccessFile.length() })() / ({
                nio.mf.mappedByteBuffer.duplicate().clear().run {
                    run {
                        while (get() != '\n'.toByte());
                        position()
                    }
                }
            })())::toInt) { recordIndex->
                nio.mf.mappedByteBuffer.position(recordIndex * fixedWidth.recordLen ).slice().limit( fixedWidth.recordLen )
            }
            nio.drivers = text(*columnarArity.type.toArray())

            val fourBy: suspend CoroutineScope.() -> Unit = {
                System.err.println(""+coroutineContext)
                val medium = coroutineContext[Medium.mediumKey]
                val addressable = coroutineContext[Addressable.addressableKey]
                assert(addressable is Indexable)
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
            nio + RowMajor()
)
    coroutineScope.launch(block = fourBy).join() }
       runBlocking { val coroutineScope= CoroutineScope(
           columnarArity +
                   fixedWidth +
                   indexable +
                   nio    +  ColumnMajor()
       )
           coroutineScope.launch(block = fourBy).join() }


        }
    }
}

