package columnar

import columnar.IOMemento.*
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
    class Columnar(val type: Vect0r<IOMemento>) : Arity() {
        companion object {
            fun of(vararg type: IOMemento) = Columnar(type.toVect0r())
        }
    }

    class Scalar(val type: IOMemento) : Arity()
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

sealed class Traversal : Element {

    /**
     * [x++,y,z]
     * [x,y++,z]
     */
    class InstanceWise : Traversal()

    /**
     * [x,y++]
     * [x++,y]
     */
    class TypeWise : Traversal()

    /**
     * {x,y,z}+-(1|n|n^?)]
     */
    class Hilbert : Traversal()

    /**
     * [0,0,0]
     * [1,..1,..1]
     * [2,..2,..2]
     */
    class Diagonal : Traversal()

    override val key: Key<Traversal> get() = theKey

    companion object {
        val theKey = object : Key<Traversal> {}
    }
}

abstract sealed class Medium( ) : Element {
    class Nio(  val mf: MappedFile) : Medium( ) {
        override val seek: (Int) -> Unit = { i -> mf.mappedByteBuffer.position(i * seekEof).slice().limit(seekEof) }
        override val size = mf.randomAccessFile.length()
        override val seekEof by lazy {
            mf.mappedByteBuffer.duplicate().clear().let { mm ->
                while (mm.get() != '\n'.toByte());
                mm.position()
            }
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

    override val key: Key<Medium> get() = theKey
    abstract val seek: (Int) -> Unit
    abstract val size: Long
    abstract val seekEof: Int

    companion object {
        val theKey = object : Key<Medium> {}
    }
}


/**
 * the Cursos attributes appear to be interdependent on each other's advantages.
 *
 * if this is to be a  trait system, the functional objects need to look like a blackboard
 */
suspend fun main() {


    val columnar = Arity.Columnar.of(txtLocalDate, txtString, txtFloat, txtFloat)
    val meta =
        arrayOf("date", "channel", "delivered", "ret")
            .zip(
                arrayOf((0 to 10), (10 to 84), (84 to 124), (124 to 164))
            ).zip(columnar.type)


    MappedFile("src/test/resources/caven4.fwf").use { mf ->
        val nio = Medium.Nio(mf)

        val reclen by lazy {

            nio.seekEof
        }
        val channel = mf.channel
        val fixedWidth = Boundary.FixedWidth(reclen)
        val indexable =
            Addressable.Indexable(
                reclen / nio.size,
                nio.seek
            )
        val byRow = Traversal.InstanceWise()
        val coroutineContext = EmptyCoroutineContext + (fixedWidth + columnar + indexable + byRow + nio)
        coroutineContext.run {

        }

    }
}



