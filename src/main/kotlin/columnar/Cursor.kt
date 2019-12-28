package columnar

import columnar.CellDriver.Companion.Fixed
import columnar.IOMemento.*
import columnar.Medium.Companion.mediumKey
import columnar.Medium.NioMMap
import columnar.RecordBoundary.FixedWidth
import kotlinx.coroutines.runBlocking
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.time.Instant
import java.time.LocalDate
import kotlin.coroutines.CoroutineContext.Element
import kotlin.coroutines.CoroutineContext.Key
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.math.min


sealed class Arity : Element {
    open class Scalar(val type: IOMemento, name: String? = null) : Arity()
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
    abstract class Forward<T> : Iterable<T>, Addressable()

    class Indexable(
        /**count of records*/
        val size: () -> Int, val seek: (Int) -> Unit
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
 * even fixed-record fixed-cell (e.g. purer ISAM) formats have to use external size variables for varchar
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
        val endl: () -> Byte? = '\n'::toByte,
        val pad: () -> Byte? = ' '::toByte
    ) : RecordBoundary()

    companion object {


        val boundaryKey = object : Key<RecordBoundary> {}
    }

    override val key get() = boundaryKey

}

typealias writefn<M, R> = Function2<M, R, Unit>
typealias readfn<M, R> = Function1<M, R>

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
                suspend fun forMedium(medium: Medium?) =
                    (medium ?: coroutineContext.get(mediumKey) as? NioMMap)?.let {
                        mapOf(
                            IoInt to Tokenized(bb2ba `•` btoa `•` trim * String::toInt, { a, b -> a.putInt(b) }),
                            IoLong to Tokenized((bb2ba `•` btoa `•` trim * String::toLong), { a, b -> a.putLong(b) }),
                            IoFloat to Tokenized(
                                bb2ba `•` btoa `•` trim `•` String::toFloat,
                                { a, b -> a.putFloat(b) }),
                            IoDouble to Tokenized(
                                bb2ba `•` btoa `•` trim `•` String::toDouble,
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

        class Fixed<B, R>(val bound: Int, read: readfn<B, R>, write: writefn<B, R>) : CellDriver<B, R>(read, write) {
            companion object {
                /**coroutineContext derived map of Medium access drivers
                 *
                 */
                suspend fun forMedium(medium: Medium?) =
                    (medium ?: coroutineContext.get(mediumKey) as? NioMMap)?.let {
                        mapOf(
                            IoInt to Fixed(4, ByteBuffer::getInt, { a, b -> a.putInt(b);Unit }),
                            IoLong to Fixed(8, ByteBuffer::getLong, { a, b -> a.putLong(b);Unit }),
                            IoFloat to Fixed(4, ByteBuffer::getFloat, { a, b -> a.putFloat(b);Unit }),
                            IoDouble to Fixed(8, ByteBuffer::getDouble, { a, b -> a.putDouble(b);Unit }),
                            IoLocalDate to Fixed(
                                8,
                                { it.long `•` LocalDate::ofEpochDay },
                                { a, b: LocalDate -> a.putLong(b.toEpochDay()) }),
                            IoInstant to Fixed(
                                8,
                                { it.long `•` Instant::ofEpochMilli },
                                { a, b: Instant -> a.putLong(b.toEpochMilli()) }),
                            IoString to
                                    /**
                                     * Array-like has no constant bound.
                                     */
                                    Tokenized.forMedium(medium)!![IoString]!!
                        )
                    }
            }
        }
    }

}

typealias NioCursor = Matrix<Triple<() -> Any, (Any?) -> Unit, Triple<CellDriver<ByteBuffer, Any?>, IOMemento, Int>>>


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
sealed class Ordering : Element {

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
            (medium as NioMMap).let {
                val drivers = when (arity) {
                    is Arity.Columnar -> Fixed.forMedium(medium)!![arity.type]
                    is Arity.Scalar -> TODO()
                    is Arity.Variadic -> TODO()
                }

                val coords = when (recordBoundary) {
                    is FixedWidth -> recordBoundary.coords
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

    override val key: Key<Ordering> get() = orderingKey

    companion object {
        val orderingKey = object : Key<Ordering> {}

    }
}

data class NioCursor1(
    val shape: IntArray,
    val access: (IntArray) -> Triple<() -> Any, (Any?) -> Unit, Triple<CellDriver<ByteBuffer, out Any>, IOMemento, Int>>
)


sealed class Medium : Element {
    override val key: Key<Medium> get() = mediumKey
    abstract val seek: (Int) -> Unit
    abstract val size: () -> Long
    abstract val recordLen: () -> Int

    companion object {
        val mediumKey = object : Key<Medium> {}
    }

    class NioMMap(
        val mf: MappedFile,
        val drivers: Vect0r<CellDriver<ByteBuffer, *>>? = null
    ) : Medium() {


        fun asContextVect0r(
            indexable: Addressable.Indexable,
            fixedWidth: FixedWidth,
            state: () -> Pair<ByteBuffer, Pair<Long, Long>> = { Pair(ByteBuffer.allocate(0), Pair(-1L, -1L)) }
        ) = Vect0r(indexable.size, { ix ->
            translateMapping(
                ix,
                fixedWidth.recordLen,
                state()
            )
        })

        suspend fun asSequence(): Sequence<ByteBuffer> {
            val indexable = coroutineContext[Addressable.addressableKey]
            val fixedWidth = coroutineContext[RecordBoundary.boundaryKey]

            var state = Pair(ByteBuffer.allocate(0), Pair(-1L, -1L))
            val cvec = asContextVect0r(
                indexable as Addressable.Indexable,
                fixedWidth as FixedWidth,
                { -> state })
            return sequence {
                for (ix in 0 until cvec.size) {
                    state = cvec[ix]
                    yield(state.first)
                }
            }
        }

        /**
         * seek to record offset
         */
        override val seek: (Int) -> Unit = {
            mf.mappedByteBuffer.position(it * recordLen()).slice().limit(recordLen())
        }
        override val size = { mf.randomAccessFile.length() }
        override val recordLen = {
            mf.mappedByteBuffer.duplicate().clear().run {
                run {
                    while (get() != '\n'.toByte());
                    position()
                }
            }
        }
        val windowSize by lazy { Int.MAX_VALUE.toLong() - (Int.MAX_VALUE.toLong() % recordLen()) }


        fun remap(
            rafchannel: FileChannel, window: Pair<Long, Long>
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
            rowsize: Int, state: Pair<ByteBuffer, Pair<Long, Long>>
        ): Pair<ByteBuffer, Pair<Long, Long>> {
            var (buf1, window1) = state
            val lix = rowIndex.toLong()
            val seekTo = rowsize * lix
            if (seekTo >= window1.second) {
                val recordOffset0 = seekTo
                window1 = recordOffset0 to min(size() - seekTo, windowSize)
                buf1 = remap(mf.channel, window1)
            }
            val rowBuf = buf1.position(seekTo.toInt() - window1.first.toInt()).slice().limit(recordLen())
            return Pair(rowBuf, window1)
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
            val nio = NioMMap(mf)
            val fixedWidth = FixedWidth(
                nio.recordLen(),
                arrayOf((0 to 10), (10 to 84), (84 to 124), (124 to 164)).map {
                    it.toList().toIntArray()
                }.toVect0r()
            )
            val indexable = Addressable.Indexable(size = { (nio.recordLen() / nio.size()).toInt() }, seek = nio.seek)
            val rowMajor = Ordering.RowMajor()

            /**
             * for java readers,  these elements are same as reifiable threadlocals
             */
            val coroutineContext =
                EmptyCoroutineContext +
                        columnarArity +
                        fixedWidth +
                        indexable +
                        rowMajor + nio

            coroutineContext.run {
                val drivers = Fixed.forMedium(nio)!![columnarArity.type]
                val coords = fixedWidth.coords
                val cursor = rowMajor.nioCursor()

            }
        }
    }
}


