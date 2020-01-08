package columnar.context

import columnar.*
import columnar.context.Arity.Companion.arityKey
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.ensurePresent
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.lang.System.err
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.time.Instant
import java.time.LocalDate
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.math.min

typealias  MMapWindow = Tw1n<Long>
typealias  NioCursorState = Pai2<ByteBuffer, MMapWindow>

sealed class Medium : CoroutineContext.Element {
    override val key: CoroutineContext.Key<Medium> get() = mediumKey
    @Deprecated("These should be vastly more local in the Indexable ")
    abstract val seek: (Int) -> Unit
    @Deprecated("These should be vastly more local in the Indexable ")
    abstract val size: () -> Long
    @Deprecated("These should be vastly more local in the FixedWidth ")
    abstract val recordLen: () -> Int

    companion object {
        val mediumKey = object :
            CoroutineContext.Key<Medium> {}
    }


}

class Kxio : Medium() {
    override val seek: (Int) -> Unit
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val size: () -> Long
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val recordLen: () -> Int
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
}

val canary = ByteBuffer.allocate(
    0
) t2 (-1L t2 -1L)

class NioMMap(
    val mf: MappedFile, var drivers: Array<CellDriver<ByteBuffer, Any?>>? = null,
    val state: ThreadLocal<NioCursorState> = ThreadLocal.withInitial { canary }
) : Medium() {
    @Suppress("UNCHECKED_CAST")
    suspend fun values(): NioCursor = let {
        val medium = this
        val ordering = coroutineContext[Ordering.orderingKey]!!
        val arity = coroutineContext[arityKey]!!
        val addressable = coroutineContext[Addressable.addressableKey]!!
        val recordBoundary = coroutineContext[RecordBoundary.boundaryKey]!!
        medium.let {
            val drivers = medium.drivers ?: text((arity as Columnar).first /*assuming fwf here*/)
            val coords = when (recordBoundary) {
                is FixedWidth -> recordBoundary.coords
                is TokenizedRow -> TODO()
            }

            fun NioAbstractionLayer(): Pai2<Vect0r<NioCursorState>, (ByteBuffer) -> Vect0r<Tripl3<CellDriver<ByteBuffer, Any?>, IOMemento, Int>>> =
                medium.asContextVect0r(addressable as Indexable, recordBoundary) t2 { y: ByteBuffer ->
                    Vect0r(drivers.size.`⟲`) { x: Int ->
                        drivers[x] t2 (arity as Columnar).first[x] t3 coords[x].size
                    }
                }
            when (ordering) {
                is RowMajor -> NioAbstractionLayer().let { (row: Vect0r<NioCursorState>, col: (ByteBuffer) -> Vect0r<Tripl3<CellDriver<ByteBuffer, Any?>, IOMemento, Int>>): Pai2<Vect0r<NioCursorState>, (ByteBuffer) -> Vect0r<Tripl3<CellDriver<ByteBuffer, Any?>, IOMemento, Int>>> ->
                    NioCursor(
                        intArrayOf(drivers.size, row.size)
                    ) { (x: Int, y: Int): IntArray ->
                        dfn(row, y, col, x, coords)
                    }

                }
                is ColumnMajor -> NioAbstractionLayer().let { (row: Vect0r<NioCursorState>, col: (ByteBuffer) -> Vect0r<Tripl3<CellDriver<ByteBuffer, Any?>, IOMemento, Int>>): Pai2<Vect0r<NioCursorState>, (ByteBuffer) -> Vect0r<Tripl3<CellDriver<ByteBuffer, Any?>, IOMemento, Int>>> ->

                    NioCursor(
                        intArrayOf(row.size, drivers.size)
                    ) { (y: Int, x: Int): IntArray ->
                        dfn(row, y, col, x, coords)
                    }
                }
                is Hilbert -> TODO()
                is RTree -> TODO()
            }
        } as NioCursor
    }

    @Suppress("UNCHECKED_CAST")
    private fun dfn(
        row: Vect0r<NioCursorState>,
        y: Int,
        col: (ByteBuffer) -> Vect0r<Tripl3<CellDriver<ByteBuffer, Any?>, IOMemento, Int>>,
        x: Int,
        coords: Vect0r<IntArray>
    ) = let {
        coords[x].let { (start, end) ->
            row[y].let { (row1) ->
                col(row1).let { (_, triple) ->
                    triple(x).let { triple1 ->
                        triple1.let { (driver: CellDriver<ByteBuffer, Any?>) ->
                            { row1[start, end] `→` driver.read } as () -> Any t2
                                    { v: Any? -> driver.write(row1[start, end], v) } t3
                                    triple1
                        }
                    }
                }
            }
        }
    }

    companion object {
        fun text(m: Vect0r<IOMemento>): Array<CellDriver<ByteBuffer, Any?>> {
            val arrayOfTokenizeds = Tokenized.mapped[m]
            return arrayOfTokenizeds as Array<CellDriver<ByteBuffer, Any?>>
        }

        fun binary(m: Vect0r<IOMemento>): Array<CellDriver<ByteBuffer, Any?>> {
            val arrayOfCellDrivers = Fixed.mapped[m]
            return arrayOfCellDrivers as Array<CellDriver<ByteBuffer, Any?>>
        }
    }

    fun asContextVect0r(
        indexable: Indexable,
        fixedWidth: FixedWidth
    ) = Vect0r(indexable.size) { ix: Int ->
        runBlocking {

            translateMapping(
                ix,
                fixedWidth.recordLen
            )
        }
    }

    /**
     * seek to record offset
     */
    override val seek: (Int) -> Unit = {
        mf.mappedByteBuffer.get().position(it * recordLen()).slice().limit(recordLen())
    }
    override val size = { mf.randomAccessFile.length() }
    override val recordLen = {
        mf.mappedByteBuffer.get().duplicate().clear().run {
            run {
                while (get() != '\n'.toByte());
                position()
            }
        }
    }
    val windowSize by lazy { Int.MAX_VALUE.toLong() - (Int.MAX_VALUE.toLong() % recordLen()) }

    fun remap(
        rafchannel: FileChannel, window: MMapWindow
    ) = window.let { (offsetToMap: Long, sizeToMap: Long) ->
        rafchannel.map(mf.mapMode, offsetToMap, sizeToMap).also { System.err.println("remap:" + window.pair) }
    }

    /**
     * any seek on a large volume (over MAXINT size) need to be sure there is a mapped extent.
     * this will perform necessary mapping changes to an existing context state.
     *
     * this will also use the context buffer to prepare a rowbuf slice
     *
     * @return
     */
    suspend fun translateMapping(
        rowIndex: Int,
        rowsize: Int
    ): NioCursorState {

        var reuse = false
        lateinit var pbuf: ByteBuffer
        val (memo1, memo2) = state.get()
        return withContext(state.asContextElement()) {
            state.ensurePresent()
            var (buf1, window1) = state.get()
            val lix = rowIndex.toLong()
            val seekTo = rowsize * lix
            if (seekTo in (window1.first..(window1.second - rowsize))) reuse = true
            else {
                val recordOffset0 = seekTo
                window1 = (recordOffset0 t2 min(size() - seekTo, windowSize))
                buf1 = remap(mf.channel, (window1))

            }
            pbuf = buf1
            val rowBuf =
                buf1./* pbuf should rarely be mutated.  clear().*/position(seekTo.toInt() - window1.first.toInt())
                    .slice().limit(recordLen())
            rowBuf t2 window1
        }.also {
            when {
                reuse ->
                    try {
                        assert(true) { "reuse( $memo1, ${memo2.pair})".also(err::println) }
                    } catch (a: AssertionError) {
                    }
                else -> it.let { (_, window) ->
                    state.set(pbuf t2 window)
                }
            }
        }
    }
}

/**
 * CellDriver functions to read and write primitive  state instances to more persistent tiers.
 *
 * struct level abstractions exist without coroutineContext representation.  the structs must be assembled in user space
 * and passed into the context-based machinery for various transforms
 */
open class CellDriver<B, R>(
    open val read: readfn<B, R>,
    open val write: writefn<B, R>
)

class Tokenized<B, R>(read: readfn<B, R>, write: writefn<B, R>) : CellDriver<B, R>(read, write) {
    companion object {
        /**coroutineContext derived map of Medium access drivers
         */

        val mapped = mapOf(
            IOMemento.IoInt to Tokenized(
                bb2ba `→` btoa `→` trim * String::toInt,
                { a, b -> a.putInt(b) }),
            IOMemento.IoLong to Tokenized(
                (bb2ba `→` btoa `→` trim * String::toLong),
                { a, b -> a.putLong(b) }),
            IOMemento.IoFloat to Tokenized(
                bb2ba `→` btoa `→` trim `→` String::toFloat,
                { a, b -> a.putFloat(b) }),
            IOMemento.IoDouble to Tokenized(
                bb2ba `→` btoa `→` trim `→` String::toDouble,
                { a, b -> a.putDouble(b) }),
            IOMemento.IoString to Tokenized(
                bb2ba `→` btoa `→` trim,
                xInsertString
            ),
            IOMemento.IoLocalDate to Tokenized(
                dateMapper `⚬` trim `⚬` btoa `⚬` bb2ba,
                { a, b -> a.putLong(b.toEpochDay()) }),
            IOMemento.IoInstant to Tokenized(
                bb2ba `→` btoa `→` trim `→` instantMapper,
                { a, b -> a.putLong(b.toEpochMilli()) })
        )
    }
}

class Fixed<B, R>(val bound: Int, read: readfn<B, R>, write: writefn<B, R>) :
    CellDriver<B, R>(read, write) {
    companion object {
        /**coroutineContext derived map of Medium access drivers
         *
         */
        val mapped = mapOf(
            IOMemento.IoInt to Fixed(
                4,
                ByteBuffer::getInt,
                { a, b -> a.putInt(b);Unit }),
            IOMemento.IoLong to Fixed(
                8,
                ByteBuffer::getLong,
                { a, b -> a.putLong(b);Unit }),
            IOMemento.IoFloat to Fixed(
                4,
                ByteBuffer::getFloat,
                { a, b -> a.putFloat(b);Unit }),
            IOMemento.IoDouble to Fixed(
                8,
                ByteBuffer::getDouble,
                { a, b -> a.putDouble(b);Unit }),
            IOMemento.IoLocalDate to Fixed(
                8,
                { it.long `→` LocalDate::ofEpochDay },
                { a, b: LocalDate -> a.putLong(b.toEpochDay()) }),
            IOMemento.IoInstant to Fixed(
                8,
                { it.long `→` Instant::ofEpochMilli },
                { a, b: Instant -> a.putLong(b.toEpochMilli()) }),
            IOMemento.IoString to /*Array-like has no constant bound. */ Tokenized.mapped[IOMemento.IoString]!!
        )
    }
}