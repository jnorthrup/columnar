package cursors.context

import cursors.io.TableRoot
import vec.macros.Vect02
import vec.macros.Vect02_.right
import vec.macros.`→`
import vec.macros.get
import vec.macros.t2
import java.nio.ByteBuffer
import kotlin.coroutines.CoroutineContext

/**
 * ordering arranges the row and column IO chunking to tailor the io access patterns.
 *
 * The composable options are
 *  * Indexable Addressability Consumer
 *  * linear Addressability provider
 *  * coordinate system translations for such as pivot and inversion
 */
sealed class Ordering : CoroutineContext.Element {
    override val key: CoroutineContext.Key<Ordering> get() = orderingKey

    companion object {
        val orderingKey = object : CoroutineContext.Key<Ordering> {}
    }
}

/**
 * [x++,y,z]
 * [x,y++,z]
 */
class RowMajor : Ordering() {
    companion object {
        fun fixedWidthOf(
            nio: NioMMap,
            coords: Vect02<Int, Int>,
            defaulteol: () -> Byte = '\n'::toByte,
        ) = fixedWidth(nio, coords, defaulteol)

        fun fixedWidth(
            nio: NioMMap,
            coords: Vect02<Int, Int>,
            defaulteol: () -> Byte,
        ): FixedWidth {
            return FixedWidth(recordLen = defaulteol() `→` { endl: Byte ->
                nio.mappedFile.mappedByteBuffer.duplicate().clear().run {
                    while (get() != endl);
                    position()
                }
            }, coords = coords)
        }


        fun indexableOf(
            nio: NioMMap, fixedWidth: FixedWidth,
            mappedByteBuffer: ByteBuffer = nio.mappedFile.mappedByteBuffer,
        ): Indexable =
            Indexable(size = (nio.mappedFile.randomAccessFile.length() / fixedWidth.recordLen)::toInt) { recordIndex ->
                mappedByteBuffer.limit(fixedWidth.recordLen).slice().position(recordIndex * fixedWidth.recordLen)
            }

        //todo: move to rowMajor
        fun TableRoot.name(xy: IntArray) = this.let { (_, rootContext) ->
            (rootContext[Arity.arityKey]!! as Columnar).let { cnar ->
                cnar.right[(rootContext[orderingKey]!! as? ColumnMajor)?.let { xy[1] } ?: xy[0]]
            }
        }
    }

    /**
     * this builds a context and launches a cursor in the given NioMMap frame of reference
     */
    fun fromFwf(
        fixedWidth: FixedWidth,
        indexable: Indexable,
        nio: NioMMap,
        columnarArity: Columnar,

        ): TableRoot =
        (this + fixedWidth + indexable + nio + columnarArity).let { coroutineContext -> nio.values(coroutineContext) t2 coroutineContext }

    /**
     * this builds a context and launches a cursor in the given NioMMap frame of reference
     */
    fun fromBinary(
        fixedWidth: FixedWidth,
        indexable: Indexable,
        nio: NioMMap,
        columnarArity: Columnar,

        ): TableRoot =
        (this + fixedWidth + indexable + nio + columnarArity).let { coroutineContext -> nio.values(coroutineContext) t2 coroutineContext }

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

/**for seh
 * Associative||Abstract, Variadic,
 */
abstract class RTree : Ordering()
