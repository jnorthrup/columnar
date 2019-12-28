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
        override suspend fun nioCursor() = let {
            val medium = coroutineContext[mediumKey]!!
            val arity = coroutineContext[Arity.arityKey]!!
            val addressable = coroutineContext[Addressable.addressableKey]!!
            val recordBoundary = coroutineContext[RecordBoundary.boundaryKey]!!
            (medium as Medium.NioMMap).let {
                val drivers = when (arity) {
                    is Arity.Columnar -> CellDriver.Companion.Fixed.forMedium(medium)!![arity.type]
                    is Arity.Scalar -> TODO()
                    is Arity.Variadic -> TODO()
                }

                val coords = when (recordBoundary) {
                    is RecordBoundary.FixedWidth -> recordBoundary.coords
                    is RecordBoundary.Tokenized -> TODO()
                }

                val row = medium.asContextVect0r(addressable as Addressable.Indexable, recordBoundary)
                val col = { y: ByteBuffer ->
                    Vect0r({ drivers.size }, { x: Int ->
                        drivers[x] to arity.type[x] by coords[x].size
                    })
                }
                NioCursor(
                    intArrayOf(drivers.size, row.size)
                ) { coords: IntArray ->
                    val (x, y) = coords
                    val (row1, state) = row[y]
                    val (size, triple) = col(row1)
                    triple(x).let { theTriple ->
                        theTriple.let { (driver, type, size) ->
                            val rfn = { row1.duplicate() `•` driver.read }
                            @Suppress("UNCHECKED_CAST") val wfn =
                                { a: Any? ->
                                    ((driver as CellDriver<ByteBuffer, Any?>).write)(
                                        row1.duplicate(),
                                        a
                                    )
                                }
                            @Suppress("UNCHECKED_CAST")
                            rfn to wfn by theTriple as Triple<CellDriver<ByteBuffer, Any?>, IOMemento, Int>
                        }
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