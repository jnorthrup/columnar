package columnar

import columnar.IOMemento.*
import java.nio.ByteBuffer
import java.time.Instant
import java.time.LocalDate
import kotlin.coroutines.CoroutineContext.Element
import kotlin.coroutines.CoroutineContext.Key
import kotlin.coroutines.EmptyCoroutineContext

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
    class Columnar(val type: Vect0r<IOMemento>) : Arity() {
        companion object {
            fun of(vararg type: IOMemento) = Columnar(type.toVect0r())
        }
    }

    class Variadic(val types: () -> Vect0r<IOMemento>) : Arity()

    override val key: Key<Arity> get() = theKey

    companion object {
        val theKey = object : Key<Arity> {}
    }
}

sealed class Addressable : Element {
    class Forward(val hasRemaining: () -> Boolean, val next: () -> Unit) : Addressable()
    class Indexable(val size: Long, val seek: (Int) -> Unit) : Addressable()
    class Abstract<T, Q>(val size: () -> Q, val seek: (T) -> Unit) : Addressable()
    companion object {
        val theKey = object : Key<Addressable> {}
    }

    override val key: Key<Addressable> get() = theKey
}

sealed class Boundary : Element {
    class Tokenized(val tokenizer: (String) -> List<String>) : Boundary()
    class FixedWidth(val recordLen: Int, val endl: () -> Byte? = { '\n'.toByte() }, val pad: Byte? = ' '.toByte()) :
        Boundary()

    override val key: Key<Boundary> get() = theKey

    companion object {
        val theKey = object : Key<Boundary> {}
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

    override val key: Key<Ordering> get() = theKey

    companion object {
        val theKey = object : Key<Ordering> {}
    }
}
typealias writefn<M, R> = Function2<M, R, Unit>
typealias readfn<M, R> = Function1<M, R>

abstract sealed class Medium : Element {
    class Nio(val mf: MappedFile) : Medium() {
        override val seek: (Int) -> Unit = { i -> mf.mappedByteBuffer.position(i * seekEof).slice().limit(seekEof) }
        override val size = mf.randomAccessFile.length()
        override val seekEof by lazy {
            mf.mappedByteBuffer.duplicate().clear().let { mm -> while (mm.get() != '\n'.toByte()); mm.position() }
        }

        open class Tokenized<T>(override val read: readfn<ByteBuffer, T>, override val write: writefn<ByteBuffer, T>) :
            NioMemento<ByteBuffer, T>

        open class Fixed<T>(
            val bytes: Int,
            override val read: readfn<ByteBuffer, T>,
            override val write: writefn<ByteBuffer, T>
        ) : NioMemento<ByteBuffer, T>

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

        class Kxio : Medium() {
            override val seek: (Int) -> Unit
                get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
            override val size: Long
                get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
            override val seekEof: Int
                get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        }
    }

    override val key: Key<Medium> get() = mediumKey
    abstract val seek: (Int) -> Unit
    abstract val size: Long
    abstract val seekEof: Int

    companion object {
        val mediumKey = object : Key<Medium> {}
    }
}


/**
 * the Cursos attributes appear to be interdependent on each other's advantages.
 *
 * if this is to be a  trait system, the functional objects need to look like a blackboard
 */
suspend fun main() {

    MappedFile("src/test/resources/caven4.fwf").use { mf ->
        Medium.Nio(mf).let { nio ->
            val reclen by lazy {

                nio.seekEof
            }

            val columnar = Arity.Columnar.of(
                txtLocalDate,
                txtString,
                txtFloat,
                txtFloat
            )
            val meta =
                arrayOf("date", "channel", "delivered", "ret")
                    .zip(
                        arrayOf((0 to 10), (10 to 84), (84 to 124), (124 to 164))
                    ).zip(Medium.Nio.IO[columnar.type])


            val channel = mf.channel

            val coroutineContext = EmptyCoroutineContext + (Boundary.FixedWidth(reclen) + columnar + Addressable.Indexable(
                reclen / nio.size,
                nio.seek
            ) + Ordering.RowMajor() + nio)
            coroutineContext.run {
            }

        }
    }
}



