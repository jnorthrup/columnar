package columnar.context

import columnar.*
import columnar.context.Medium.Companion.mediumKey
import java.nio.ByteBuffer
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/**
 * ordering arranges the row and column IO chunking to tailor the io access patterns.
 *
 * The composable options are
 *  * Indexable Addressability Consumer
 *  * linear Addressability provider
 *  * coordinate system translations for such as pivot and inversion
 *
 *
 */
sealed class Ordering : CoroutineContext.Element {

    abstract suspend fun nioCursor(): NioCursor
    /**
     * [x++,y,z]
     * [x,y++,z]
     */
    class RowMajor : Ordering() {
        @Suppress("UNCHECKED_CAST")
        override suspend fun nioCursor() = let {
            val medium = coroutineContext[mediumKey]!!
            val arity = coroutineContext[Arity.arityKey]!!
            val addressable = coroutineContext[Addressable.addressableKey]!!
            val recordBoundary = coroutineContext[RecordBoundary.boundaryKey]!!
            val nioDrivers = coroutineContext[NioMapper.cellmapperKey]!!
            (medium as Medium.NioMMap).let {
                val drivers = nioDrivers.drivers

                val coords = when (recordBoundary) {
                    is RecordBoundary.FixedWidth -> recordBoundary.coords
                    is RecordBoundary.Tokenized -> TODO()
                }

                val row = medium.asContextVect0r(addressable as Addressable.Indexable, recordBoundary)
                val col = { y: ByteBuffer ->
                    Vect0r({ drivers.size }, { x: Int ->
                        drivers[x] to (arity as Arity.Columnar).type[x] by coords[x].size
                    })
                }
                NioCursor(
                    intArrayOf(drivers.size, row.size)
                ) { requestCoords: IntArray ->
                    val (x, y) = requestCoords
                    val (row1, state) = row[y]
                    val (_, triple) = col(row1)
                    val triple1 = triple(x)
                    triple1.let { (driver, type, cellSize) ->
                               val (start,end) = coords[x]
                                { row1[start,end] `•` driver.read } as () -> Any to
                                { v: Any? -> (driver.write as (ByteBuffer,Any?)->Unit)(row1[start,end], v) } by
                                triple1
                    }
                }
            }
        }
    }


    /**
     * [x,y++]
     * [x++,y]
     */
    abstract class ColumnMajor : Ordering()

    /**
     * {x,y,z}+-(1|n|n^?)]
     */
    abstract class Hilbert : Ordering()

    /**
     * [0,0,0]
     * [1,..1,..1]
     * [2,..2,..2]
     */
    abstract class Diagonal : Ordering()

    /**for seh*/
    abstract class RTree : Ordering()

    override val key: CoroutineContext.Key<Ordering> get() = orderingKey

    companion object {
        val orderingKey = object :
            CoroutineContext.Key<Ordering> {}

    }
}