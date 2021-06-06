package cursors.context

import cursors.TypeMemento
import cursors.io.*
import cursors.io.Vect02_.Companion.left
import vec.macros.*
import vec.util.*
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.time.Instant
import java.time.LocalDate
import kotlin.coroutines.CoroutineContext
import kotlin.math.min

/**
 *
 */
typealias  MMapWindow = Tw1n<Long>
/**
 *
 */
/**
 *
 */
typealias  NioCursorState = Pai2<ByteBuffer, MMapWindow>

sealed class Medium : CoroutineContext.Element {
    override val key: CoroutineContext.Key<Medium> get() = mediumKey

    @Deprecated("These should be  local in the Indexable ")
    abstract val seek: (Int) -> Unit

    @Deprecated("These should be  local in the Indexable ")
    abstract val size: () -> Long

    @Deprecated("These should be  local in the FixedWidth ")
    abstract var recordLen: () -> Int

    companion object {
        val mediumKey = object : CoroutineContext.Key<Medium> {}
    }
}

class Kxio(override var recordLen: () -> Int) : Medium() {
    override val seek: (Int) -> Unit
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val size: () -> Long
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
}

val canary = ByteBuffer.allocate(
    0
) t2 (-1L t2 -1L)

class NioMMap(
    val mappedFile: MappedFile,
    /**by default this will fetch text but other Mementos can be passed in as non-default
     */
    var drivers: Array<CellDriver<ByteBuffer, Any?>>? = null,
    var state: Pai2<ByteBuffer, Pai2<Long, Long>> = if (mappedFile.length.toLong() > Integer.MAX_VALUE.toLong()) canary else
        mappedFile.mappedByteBuffer.let {
            it t2 (0L t2 it.remaining().toLong())
        },
) : Medium() {
    lateinit var fixedWidth: FixedWidth

    @Suppress("UNCHECKED_CAST")
            /*suspend*/ fun values(coroutineContext: CoroutineContext): NioCursor = this.run {
        val ordering =
            coroutineContext[Ordering.orderingKey]!! //todo: revisit whether to nuke ordering or proceed with non-backwards x,y
        val arity = coroutineContext[Arity.arityKey]!!
        val addressable = coroutineContext[Addressable.addressableKey]!!
        fixedWidth = coroutineContext[RecordBoundary.boundaryKey]!! as FixedWidth

        val drivers = drivers ?: text((arity as Columnar).left /*assuming fwf here*/)
        val coords = fixedWidth.coords /* todo: fixedwidth is Optional; this code is expected to do CSV someday, but we need to
             propogate the null as a hard error for now. */

        val asContextVect0r: Vect02<ByteBuffer, Pai2<Long, Long>> =
            asContextVect0r(addressable as Indexable, fixedWidth)
        (asContextVect0r t2 { _: ByteBuffer ->
            Vect0r(drivers.size) { x: Int ->
                (drivers[x] t2 (arity as Columnar).left[x]) t3 coords[x].span
            }
        }).let { (row: Vect0r<NioCursorState>, col: (ByteBuffer) -> Vect0r<NioMeta>) ->
            NioCursor(intArrayOf(drivers.size, row.size)) { (x: Int, y: Int): IntArray ->
                mappedDriver(row, y, col, x, coords)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun mappedDriver(
        row: Vect0r<NioCursorState>,
        y: Int,
        col: (ByteBuffer) -> Vect0r<NioMeta>,
        x: Int,
        coords: Vect02<Int, Int>,
    ): Tripl3<() -> Any, (Any?) -> Unit, NioMeta> = let {
        val (start: Int, end: Int) = coords[x]
        val (outbuff: ByteBuffer) = row[y]
        val (_: Int, triple: Function<NioMeta>) = col(outbuff)

        val triple1 = triple(x)
        val (driver: CellDriver<ByteBuffer, Any?>) = triple1
        { outbuff[start, end] `→` driver.read } as () -> Any t2
                { v: Any? ->
                    val byteBuffer = outbuff[start, end]
                    byteBuffer.let {
                        val (_, b) = driver
                        b(byteBuffer.duplicate(), v)
                    }
                } t3 triple1
    }

    companion object {
        fun text(m: Vect0r<TypeMemento>): Array<CellDriver<ByteBuffer, Any?>> =
            Tokenized.mapped[m] as Array<CellDriver<ByteBuffer, Any?>>

        fun binary(m: Vect0r<IOMemento>): Array<CellDriver<ByteBuffer, Any?>> =
            m.toList().map {
                Fixed.mapped[it]
            }.toTypedArray() as Array<CellDriver<ByteBuffer, Any?>>
    }

    fun asContextVect0r(
        indexable: Indexable,
        fixedWidth: FixedWidth,
    ): Vect02<ByteBuffer, MMapWindow> =
        Vect0r(indexable.size()) { ix: Int ->
            translateMapping(
                ix,
                fixedWidth.recordLen
            )
        }

    /**
     * seek to record offset
     */
    override val seek: (Int) -> Unit = { newPosition: Int ->
        mappedFile.mappedByteBuffer.position(newPosition * recordLen()).slice().limit(recordLen())
    }
    override val size = { mappedFile.randomAccessFile.length() }

    @Suppress("ControlFlowWithEmptyBody")
    override var recordLen = {

        fixedWidth.recordLen
    }
    val windowSize: Long by lazy { Int.MAX_VALUE.toLong() - (Int.MAX_VALUE.toLong() % recordLen()) }

    fun remap(rafchannel: FileChannel, window: MMapWindow): ByteBuffer =
        window.let { (offsetToMap: Long, sizeToMap: Long) ->
            rafchannel.map(mappedFile.mapMode, offsetToMap, sizeToMap)
                .also { System.err.println("remap:" + window.pair) }
        }

    /**
     * any seek on a large volume (over MAXINT size) need to be sure there is a mapped extent.
     * this will perform necessary mapping changes to an existing context state.
     *
     * this will also use the context buffer to prepare a rowbuf slice
     *
     * @return
     */

    fun translateMapping(
        rowIndex: Int,
        rowsize: Int,
    ): NioCursorState = run {

        var reuse = false
        lateinit var pbuf: ByteBuffer
        val (memo1, memo2: MMapWindow) = state
        state
        var (buf1, window1) = state
        val lix = rowIndex.toLong()
        val seekTo = rowsize * lix


        val first = window1.first
        val l = window1.second - rowsize
        when {
            seekTo >= first && (seekTo <= l) -> reuse = true
            else -> {
                window1 = (seekTo t2 min(size() - seekTo, windowSize))
                val mappedByteBuffer = remap(mappedFile.channel, window1)
                buf1 = mappedByteBuffer
            }
        }
        pbuf = buf1
        val rowBuf =
            buf1.position(seekTo.toInt() - first.toInt())
                .slice().limit(recordLen())
        (rowBuf t2 window1).also {
            when {
                reuse ->
                    try {
                        if (logReuseCountdown > 0)
                            logDebug { "reuse( $memo1, ${memo2.pair})" }
                                .also { logReuseCountdown-- }
                    } catch (a: AssertionError) {
                    }
                else -> it.let { (_, window) ->
                    state = (pbuf t2 window)
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
    open val write: writefn<B, R>,
) {
    operator fun component1(): (B) -> R = read
    operator fun component2(): (B, R) -> Unit = write
}

class Tokenized<B, R>(read: readfn<B, R>, write: writefn<B, R>) : CellDriver<B, R>(read, write) {
    companion object {
        /**coroutineContext derived map of Medium access drivers
         */

        val mapped: Map<TypeMemento, Tokenized<ByteBuffer, out Any>> = mapOf(
            IOMemento.IoInt as TypeMemento to Tokenized(
                (::bb2ba `→` ::btoa `→` ::trim * String::toInt) as readfn<ByteBuffer, Int>,
                { a, b: Int -> a.putInt(b) }),
            IOMemento.IoByte as TypeMemento to Tokenized(
                (::bb2ba `→` ::btoa `→` ::trim * String::toInt * Int::toUByte * UByte::toByte) as readfn<ByteBuffer, Byte>,
                { a, b: Byte -> a.put((b.toInt() and 0xff).toByte()) }),
            IOMemento.IoLong to Tokenized(
                ::bb2ba `→` ::btoa `→` ::trim * String::toLong,
                { a, b: Long -> a.putLong(b) }),
            IOMemento.IoFloat to Tokenized(
                ::bb2ba `→` ::btoa `→` ::trim `→` String::toFloat,
                { a, b: Float -> a.putFloat(b) }),
            IOMemento.IoDouble to Tokenized(
                ::bb2ba `→` ::btoa `→` ::trim `→` String::toDouble,
                { a, b: Double -> a.putDouble(b) }),
            IOMemento.IoString to Tokenized(
                ::bb2ba `→` ::btoa `→` ::trim, xInsertString
            ),
            IOMemento.IoLocalDate to Tokenized(
                dateMapper `⚬` ::trim `⚬` ::btoa `⚬` ::bb2ba,
                { a, b: LocalDate -> a.putLong(b.toEpochDay()) }),
            IOMemento.IoInstant to Tokenized(
                ::bb2ba `→` ::btoa `→` ::trim `→` instantMapper,
                { a, b: Instant -> a.putLong(b.toEpochMilli()) })
        )
    }
}

class Fixed<B, R>(val bound: Int, read: readfn<B, R>, write: writefn<B, R>) :
    CellDriver<B, R>(read, write) {
    companion object {
        /**coroutineContext derived map of Medium access drivers
         *
         */
        val mapped: Map<TypeMemento, CellDriver<ByteBuffer, out Any>> = mapOf(
            IOMemento.IoInt as TypeMemento to Fixed(
                4,
                ByteBuffer::getInt
            ) { a, b -> a.putInt(b);Unit },
            IOMemento.IoByte as TypeMemento to Fixed(
                1,
                ByteBuffer::get
            ) { a, b -> a.put((b.toInt() and 0xff.toInt()).toByte());Unit },
            IOMemento.IoLong to Fixed(
                8,
                ByteBuffer::getLong
            ) { a, b -> a.putLong(b);Unit },
            IOMemento.IoFloat to Fixed(
                4,
                ByteBuffer::getFloat
            ) { a, b -> a.putFloat(b);Unit },
            IOMemento.IoDouble to Fixed(
                8,
                ByteBuffer::getDouble
            ) { a, b -> a.putDouble(b);Unit },
            IOMemento.IoLocalDate to Fixed(
                8,
                { it.long `→` LocalDate::ofEpochDay },
                { a, b: LocalDate -> a.putLong(b.toEpochDay()) }),
            IOMemento.IoInstant to Fixed(
                8,
                { it.long `→` Instant::ofEpochMilli },
                { a, b: Instant -> a.putLong(b.toEpochMilli()) }),
            IOMemento.IoString to /*Array-like has no constant bound. */ (Tokenized.mapped[IOMemento.IoString]
                ?: error(""))
        )
    }
}