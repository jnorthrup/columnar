package columnar

import columnar.IOMemento.*
import columnar.context.*
import columnar.context.NioMMap.Companion.text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.nio.ByteBuffer


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
    MappedFile(filename).use { mf ->
        val columnarArity = Columnar.of(mapping)
        val nio = NioMMap(mf)
        nio.drivers = text(*columnarArity.type.toArray())

        val defaulteol = { '\n'.toByte() }
        val mappedByteBuffer = nio.mf.mappedByteBuffer
        val fixedWidth = FixedWidth(endl = defaulteol, recordLen = defaulteol().let { endl ->
            mappedByteBuffer.duplicate().clear().run {
                while (get() != endl);
                position()
            }
        }, coords = coords)

        val indexable =
            Indexable(size = (nio.mf.randomAccessFile.length() / fixedWidth.recordLen)::toInt) { recordIndex ->
                val lim = { b: ByteBuffer -> fixedWidth.recordLen.let(b::limit) }
                val pos = { b: ByteBuffer -> b.position(recordIndex * fixedWidth.recordLen) }
                val sl = { b: ByteBuffer -> b.slice() }
                mappedByteBuffer.`⟲`(lim `●` sl `●` pos)
            }

        val fourBy: suspend CoroutineScope.() -> Unit = {
            System.err.println("" + coroutineContext)
            val nio = coroutineContext[Medium.mediumKey] as NioMMap
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
        runBlocking(
            RowMajor() +
                    fixedWidth +
                    indexable +
                    nio +
                    columnarArity
        ) {
            launch(block = fourBy).join()
        }
        runBlocking(
            columnarArity +
                    fixedWidth +
                    indexable +
                    nio + ColumnMajor()
        ) {
            launch(block = fourBy).join()
        }
    }
}

