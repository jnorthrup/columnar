package columnar

import columnar.IOMemento.*
import kotlinx.coroutines.async
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
    class Matrix(type: IOMemento, vararg val shape: Int) : Scalar(type)
    class Columnar(val type: Vect0r<IOMemento>, val names: Vect0r<String>? = null) : Arity() {
        companion object {
            fun of(vararg type: IOMemento) = Columnar(type.toVect0r())
            fun of(mapping: Iterable<Pair<String, IOMemento>>) =
                Columnar(mapping.map { it.second }.toVect0r(), mapping.map { it.first }.toVect0r())
        }
    }

    class Variadic(val types: () -> Vect0r<IOMemento>) : Arity()

    override val key: Key<Arity> get() = arityKey

    companion object {
        val arityKey = object : Key<Arity> {}
    }
}

sealed class Addressable : Element {
    class Forward(val hasRemaining: () -> Boolean, val next: () -> Unit) : Addressable()
    class Indexable(val size: Long, val seek: (Int) -> Unit) : Addressable()

    override val key: Key<Addressable> get() = addressableKey

    class Abstract<T, Q>(val size: () -> Q, val seek: (T) -> Unit) : Addressable()
    companion object {
        val addressableKey = object : Key<Addressable> {}

    }
}

sealed class Boundary : Element {
    class Tokenized(val tokenizer: (String) -> List<String>) : Boundary()
    class FixedWidth(
        val recordLen: Int,
        val coords: Vect0r<IntArray>,
        val endl: () -> Byte? = { '\n'.toByte() },
        val pad: Byte? = ' '.toByte()
    ) :
        Boundary()

    override val key: Key<Boundary> get() = boundaryKey

    companion object {
        val boundaryKey = object : Key<Boundary> {}
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
/*
typealias Decorator = (Any?) -> Any??
typealias ByteBufferNormalizer = Pair<Pair<Int, Int>, IOMemento>
typealias RowNormalizer = Array<Triple<String, ByteBufferNormalizer, Decorator>>
*/

sealed class Medium : Element {
    override val key: Key<Medium> get() =mediumKey
    abstract val seek: (Int) -> Unit
    abstract val size: Long
    abstract val recordLen: Int
companion object{
    val mediumKey=object : Key<Medium> {}
}

    class Nio(
        val mf: MappedFile,
        val drivers: Vect0r<NioMemento<ByteBuffer, *>>? = null
    ) : Medium() {
        override val seek: (Int) -> Unit = { i -> mf.mappedByteBuffer.position(i * recordLen).slice().limit(recordLen) }
        override val size = mf.randomAccessFile.length()
        override val recordLen by lazy {
            mf.mappedByteBuffer.duplicate().clear().let { mm -> while (mm.get() != '\n'.toByte()); mm.position() }
        }

        companion object {

            val IO =
                mapOf(
                    txtInt to Tokenized(
                        bb2ba `•` btoa `•` trim * String::toInt,
                        { a: ByteBuffer, b: Int? -> a.putInt(b ?: 0) }),
                    txtLong to Tokenized((bb2ba `•` btoa `•` trim * String::toLong), { a, b -> a.putLong(b) }),
                    txtFloat to Tokenized(
                        (bb2ba `•` btoa `•` trim `•` String::toFloat),
                        { a, b -> a.putFloat(b) }),
                    txtDouble to Tokenized(
                        (bb2ba `•` btoa `•` trim `•` String::toDouble),
                        { a, b -> a.putDouble(b) }),
                    txtString to Tokenized(bb2ba `•` btoa `•` trim, xInsertString),
                    txtLocalDate to Tokenized<LocalDate>(
                        bb2ba `•` btoa `•` trim `•` dateMapper,
                        { a, b -> a.putLong(b.toEpochDay()) }),
                    txtInstant to Tokenized<Instant>(
                        bb2ba `•` btoa `•` trim `•` instantMapper,
                        { a, b -> a.putLong(b.toEpochMilli()) }),
                    bInt to Fixed(4, ByteBuffer::getInt, { a, b -> a.putInt(b);Unit }),
                    bLong to Fixed(8, ByteBuffer::getLong, { a, b -> a.putLong(b);Unit }),
                    bFloat to Fixed(4, ByteBuffer::getFloat, { a, b -> a.putFloat(b);Unit }),
                    bDouble to Fixed(8, ByteBuffer::getDouble, { a, b -> a.putDouble(b);Unit }),
                    bString to Tokenized(bb2ba `•` btoa `•` trim, xInsertString),
                    bLocalDate to Fixed<LocalDate>(
                        8,
                        { it.long `•` LocalDate::ofEpochDay },
                        { a, b -> a.putLong(b.toEpochDay()) }),
                    bInstant to Fixed<Instant>(
                        8,
                        { it.long `•` Instant::ofEpochMilli },
                        { a, b -> a.putLong(b.toEpochMilli()) })
                )

            /**
             * IOMemento is a helper to simplify common bounded width bytebuffer FWF and CSV field usecases.
             *
             * the KClass type of value is part of the implementations but has not been added to the tuples
             */
            interface NioMemento<B, R> {
                /** this operation takes a bytebuffer and returns a value leaving the bytebuffer in an undefined state.
                 * The bytebuffers are assumed to be defensive slices that contain only one value and the value returned reads the whole buffer into conversion.
                 */
                val read: readfn<B, R>
                /**
                 * this operation uses a bytebuffer and an input value and writes the value into the bytebuffer leaving it in an undefined state.
                 */
                val write: writefn<B, R>
            }
        }
        open class Tokenized<T>(override val read: readfn<ByteBuffer, T>, override val write: writefn<ByteBuffer, T>) :
            NioMemento<ByteBuffer, T>

        open class Fixed<T>(
            val bytes: Int,
            override val read: readfn<ByteBuffer, T>,
            override val write: writefn<ByteBuffer, T>
        ) : NioMemento<ByteBuffer, T>

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
            ix: Int,
            rowsize: Long,
            window: Pair<Long, Long>,
            filesize: Long,
            windowSize: Long,
            buf: ByteBuffer
        ): Pair<ByteBuffer, Pair<Long, Long>> {
            var window1 = window
            var buf1 = buf
            val lix = ix.toLong()
            val seekTo = rowsize * lix
            if (seekTo >= window1.second) {
                val l = seekTo
                window1 = l to minOf(filesize - seekTo, (windowSize))
                buf1 = remap(window1, mf.channel)
            }
            val rowBuf = buf1.position(seekTo.toInt() - window1.first.toInt()).slice().limit(recordLen)
            return Pair(rowBuf, window1)
        }

/*        *//**
         *
         *//*
        suspend fun outputRow(
            cells: Vect0r<Any?>,
            rowBuf: ByteBuffer,
            recordLen: Int
        ) {
            val medium = coroutineContext[mediumKey]
            val boundary = coroutineContext[Boundary.boundaryKey]
            val arity = coroutineContext[Arity.arityKey]
//            val addressable = coroutineContext[Addressable.addressableKey]
            val ordering = coroutineContext[Ordering.orderingKey]
            when (medium) {
                is Nio -> {
                    medium
                    when (arity) {
                        is Arity.Columnar -> arity
                        when (ordering) {
                            is Ordering.ColumnMajor ->
                        }
                    }


                }
            }
        }*/
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
                    "date" to txtLocalDate,
                    "channel" to txtString,
                    "delivered" to txtFloat,
                    "ret" to txtFloat
                )
            )
            val nio = Medium.Nio(mf, Medium.Nio.IO[columnarArity.type].toVect0r())
            val coroutineContext =
                EmptyCoroutineContext +
                        columnarArity + nio +
                        Boundary.FixedWidth(
                            nio.recordLen,
                            arrayOf((0 to 10), (10 to 84), (84 to 124), (124 to 164)).map {
                                it.toList().toIntArray()
                            }.toVect0r()
                        ) +
                        Addressable.Indexable(nio.recordLen / nio.size, nio.seek) +
                        Ordering.RowMajor()

            coroutineContext.run {
                val x = async {
                    1
                }
            }
        }
    }
}


