package columnar.context

import columnar.*
import columnar.context.Medium.*
import columnar.context.Medium.Companion.mediumKey
import columnar.context.Medium.NioMMap.Companion.CellDriver
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

     /**
     * [x++,y,z]
     * [x,y++,z]
     */
    class RowMajor : Ordering()


    /**
     * [x,y++]
     * [x++,y]
     */
      class ColumnMajor : Ordering()

    /**
     * {x,y,z}+-(1|n|n^?)]
     */
      class Hilbert : Ordering()

    /**
     * [0,0,0]
     * [1,..1,..1]
     * [2,..2,..2]
     */
      class Diagonal : Ordering()

    /**for seh*/
    abstract class RTree : Ordering()

    override val key: CoroutineContext.Key<Ordering> get() = orderingKey

    companion object {
        val orderingKey = object : CoroutineContext.Key<Ordering> {}
    }
}
typealias NioCursor = Matrix<Triple<() -> Any?, (Any?) -> Unit, Triple<CellDriver<ByteBuffer, Any?>, IOMemento, Int>>>
