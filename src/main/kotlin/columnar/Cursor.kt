package columnar

import columnar.IOMemento.*
import columnar.Medium.Companion.mediumKey
import kotlinx.coroutines.runBlocking
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.time.Instant
import java.time.LocalDate
import kotlin.coroutines.CoroutineContext.Element
import kotlin.coroutines.CoroutineContext.Key
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext

/**
iterators of mmap (or unmapped) bytes exist has a new design falling out of this decomposition:
where iterator is random-access (for fwf) or pre-indexed with an intial EOL scanner or streaming:
 * Addressability Indexed(recordCount:Int)/Iterated(Unit)
 * Arity Unary(type:T)/Variadic(type:Array<T>)
 * fixed-width(recordlen:Int)/line-parsed(recordLen=IntArray)
 * input(Function<Cursor>->T)/output(Function<Cursor,T>->Unit)
 * row([x,y]++)/column([y,x]++) sequential orientation (e.g.  [un]like apache arrow)
 */

val Pair<Int, Int>.size: Int get() = let { (a, b) -> b - a }

sealed class Arity : Element {
    open class Scalar(val type: IOMemento) : Arity()
    class Matrix(type: IOMemento, val shape: Vect0r<Int>) : Scalar(type)
    class Columnar(val type: Vect0r<IOMemento>, val names: Vect0r<String>? = null) : Arity() {
        companion object {
            fun of(vararg type: IOMemento) = Columnar(type.toVect0r())
            fun of(mapping: Iterable<Pair<String, IOMemento>>) =
                Columnar(mapping.map { it.second }.toVect0r(), mapping.map { it.first }.toVect0r())
        }
    }

    class Variadic(val types: () -> Vect0r<IOMemento>) : Arity()

    override val key get() = arityKey

    companion object {
        val arityKey = object : Key<Arity> {}
    }
}

sealed class Addressable : Element {
    class Forward(val hasRemaining: () -> Boolean, val next: () -> Unit) : Addressable()

    class Indexable(
        /**count of records*/
        val size: Int, val seek: (Int) -> Unit
    ) : Addressable()

    override val key get() = addressableKey

    class Abstract<T, Q>(val size: () -> Q, val seek: (T) -> Unit) : Addressable()
    companion object {
        val addressableKey = object : Key<Addressable> {}
    }
}

/**
 * context-level support class for Fixed or Tokenized Record boundary conditions.
 *
 *
 * This does not extend guarantees to cell-level definitions--
 * FWF cells are parsed strings, thus tokenized within fixed records, and may carry lineendings
 * csv records with fixed length fields is rare but has its place in aged messaging formats.
 * fixed-record fixed-cell (e.g. purer ISAM) formats that have varchar cannot escape basic string tokenization and trim
 * in order to pad or zero correctly
 *
 * it is assumed that record-level granularity and above are efficiently held in context details to perform outer loops
 * once the context variables are activated and conjoined per thier roles.
 *
 */
sealed class RecordBoundary : Element {
    class Tokenized(val tokenizer: (String) -> List<String>) : RecordBoundary() {
        companion object
    }

    class FixedWidth(
        val recordLen: Int,
        val coords: Vect0r<IntArray>,
        val endl: () -> Byte? = { '\n'.toByte() },
        val pad: Byte? = ' '.toByte()
    ) : RecordBoundary()

    companion object {


        val boundaryKey = object : Key<RecordBoundary> {}
    }

    override val key get() = boundaryKey

}

/**
 * CellDriver functions to read and write primitive  state instances to more persistent tiers.
 *
 * struct level abstractions exist without coroutineContext representation.  the structs must be assembled in user space
 * and passed into the context-based machinery for various transforms
 */
open class CellDriver<B, R>(
    open val read: readfn<B, R>,
    val write: writefn<B, R>
) {
    companion object {
        class Tokenized<B, R>(read: readfn<B, R>, write: writefn<B, R>) : CellDriver<B, R>(read, write) {

            companion object {
                /**coroutineContext derived map of Medium access drivers
                 *
                 */
                suspend fun currentMedium(medium: Medium?) =
                    (medium ?: coroutineContext.get(mediumKey) as? Medium.Nio)?.let {
                        mapOf(
                            IoInt to Tokenized(
                                bb2ba `•` btoa `•` trim * String::toInt,
                                { a: ByteBuffer, b: Int? -> a.putInt(b ?: 0) }),
                            IoLong to Tokenized((bb2ba `•` btoa `•` trim * String::toLong), { a, b -> a.putLong(b) }),
                            IoFloat to Tokenized(
                                (bb2ba `•` btoa `•` trim `•` String::toFloat),
                                { a, b -> a.putFloat(b) }),
                            IoDouble to Tokenized(
                                (bb2ba `•` btoa `•` trim `•` String::toDouble),
                                { a, b -> a.putDouble(b) }),
                            IoString to Tokenized(bb2ba `•` btoa `•` trim, xInsertString),
                            IoLocalDate to Tokenized(
                                bb2ba `•` btoa `•` trim `•` dateMapper,
                                { a, b -> a.putLong(b.toEpochDay()) }),
                            IoInstant to Tokenized(
                                bb2ba `•` btoa `•` trim `•` instantMapper,
                                { a, b -> a.putLong(b.toEpochMilli()) })
                        )
                    }
            }
        }

        class Fixed<B, R>(val bound: Int?, read: readfn<B, R>, write: writefn<B, R>) : CellDriver<B, R>(read, write) {
            companion object {
                /**coroutineContext derived map of Medium access drivers
                 *
                 */
                suspend fun currentMedium(medium: Medium?) =
                    (medium ?: coroutineContext.get(mediumKey) as? Medium.Nio)?.let {
                        mapOf(
                            IoInt to Fixed(
                                4,
                                ByteBuffer::getInt,
                                { a, b -> a.putInt(b);Unit }),
                            IoLong to Fixed(
                                8,
                                ByteBuffer::getLong,
                                { a, b -> a.putLong(b);Unit }),
                            IoFloat to Fixed(
                                4,
                                ByteBuffer::getFloat,
                                { a, b -> a.putFloat(b);Unit }),
                            IoDouble to Fixed(
                                8,
                                ByteBuffer::getDouble,
                                { a, b -> a.putDouble(b);Unit }),
                            /**
                             * Array-like has no constant bound.
                             */
                            IoString to Tokenized.currentMedium(medium)!![IoString]!!,
                            IoLocalDate to Fixed(
                                8,
                                { it.long `•` LocalDate::ofEpochDay },
                                { a, b: LocalDate -> a.putLong(b.toEpochDay()) }),
                            IoInstant to Fixed(
                                8,
                                { it: ByteBuffer -> it.long `•` Instant::ofEpochMilli },
                                { a, b: Instant -> a.putLong(b.toEpochMilli()) })
                        )
                    }
            }
        }
    }

}

sealed class Ordering : Element {

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

    override val key: Key<Ordering> get() = orderingKey

    companion object {
        val orderingKey = object : Key<Ordering> {}
    }
}
typealias writefn<M, R> = Function2<M, R, Unit>
typealias readfn<M, R> = Function1<M, R>

sealed class Medium : Element {
    override val key: Key<Medium> get() = mediumKey
    abstract val seek: (Int) -> Unit
    abstract val size: Long
    abstract val recordLen: Int

    companion object {
        val mediumKey = object : Key<Medium> {}
    }

    class Nio(
        val mf: MappedFile,
        val drivers: Vect0r<CellDriver<ByteBuffer, *>>? = null
    ) : Medium() {
        /**
         * seek to record offset
         */
        override val seek: (Int) -> Unit = { i -> mf.mappedByteBuffer.position(i * recordLen).slice().limit(recordLen) }
        override val size = mf.randomAccessFile.length()
        override val recordLen by lazy {
            mf.mappedByteBuffer.duplicate().clear().let { mm -> while (mm.get() != '\n'.toByte()); mm.position() }
        }

        companion object

        fun remap(
            window: Pair<
                    /**offsetToMap*/
                    Long,
                    /**sizeToMap*/
                    Long>, rafchannel: FileChannel
        ) = window.let { (offsetToMap: Long, sizeToMap: Long) ->
            rafchannel.map(FileChannel.MapMode.READ_WRITE, offsetToMap, sizeToMap)
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
            filesize: Long,
            windowSize: Long,
            state: Pair<ByteBuffer, Pair<Long, Long>>
        ): Pair<ByteBuffer, Pair<Long, Long>> {
            var (buf1, window1) = state
            val lix = rowIndex.toLong()
            val seekTo = rowsize * lix
            if (seekTo >= window1.second) {
                val l = seekTo
                window1 = l to minOf(filesize - seekTo, (windowSize))
                buf1 = remap(window1, mf.channel)
            }
            val rowBuf = buf1.position(seekTo.toInt() - window1.first.toInt()).slice().limit(recordLen)
            return Pair(rowBuf, window1)
        }
    }


    class Kxio : Medium() {
        override val seek: (Int) -> Unit
            get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        override val size: Long
            get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        override val recordLen: Int
            get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    }

}


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
            val nio = Medium.Nio(mf)
            val fixedWidth = RecordBoundary.FixedWidth(
                nio.recordLen,
                arrayOf((0 to 10), (10 to 84), (84 to 124), (124 to 164)).map {
                    it.toList().toIntArray()
                }.toVect0r()
            )
            val indexable = Addressable.Indexable((nio.recordLen / nio.size).toInt(), nio.seek)
            val rowMajor = Ordering.RowMajor()
            val coroutineContext =
                EmptyCoroutineContext +
                        columnarArity + nio +
                        fixedWidth +
                        indexable +
                        rowMajor

            coroutineContext.run {
                var state = Pair(ByteBuffer.allocate(0), Pair(-1L, -1L))
                val windowSize = (Int.MAX_VALUE.toLong() - (Int.MAX_VALUE.toLong() % fixedWidth.recordLen))
                sequence {
                    for (ix in 0 until indexable.size) {
                        state = nio.run {
                            translateMapping(
                                ix,
                                fixedWidth.recordLen,
                                nio.size,
                                windowSize, state
                            )
                        }
                        yield(state.first)
                    }
                }
            }
        }
    }
}


