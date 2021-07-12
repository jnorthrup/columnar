@file:Suppress("NOTHING_TO_INLINE")

package vec.macros

import kotlinx.coroutines.flow.*
import java.nio.ByteBuffer
import java.util.*

/**
 * semigroup
 */
typealias Vect0r<T> = Pai2<Int, (Int) -> T>

inline val <T>Vect0r<T>.size: Int get() = first
@Suppress("NonAsciiCharacters")
typealias Matrix<T> = Pai2<
        /**shape*/
        IntArray,
        /**accessor*/
            (IntArray) -> T>

inline operator fun <T> Matrix<T>.get(vararg c: Int): T = second(c)


inline infix fun <O, R, F : (O) -> R> O.`→`(f: F) = f(this)


inline operator fun <A, B, R, O : (A) -> B, G : (B) -> R> O.times(b: G): (A) -> R =
    { a: A -> b(this(a)) }


inline infix fun <A, B, R, O : (A) -> B, G : (B) -> R, R1 : (A) -> R> O.`→`(
    b: G,
): R1 = ({ a: A -> b(this(a)) }) as R1

/**
 * G follows F
 */
inline infix fun <A, B, C, G : (B) -> C, F : (A) -> B, R : (A) -> C> G.`⚬`(
    f: F,
): R = { a: A -> a `→` f `→` this } as R

/**
 * (λx.M[x]) → (λy.M[y])	α-conversion
 * https://en.wikipedia.org/wiki/Lambda_calculus
 * */
inline infix fun <reified A, reified C, B : (A) -> C, reified V : Vect0r<A>> V.α(m: B) = map(fn = m)


inline infix fun <A, C, B : (A) -> C, T : Iterable<A>> T.α(m: B): List<C> =
    this.map { it: A -> m(it) }


inline infix fun <A, C, B : (A) -> C, T : Sequence<A>> T.α(m: B): Sequence<C> =
    this.map { it: A -> m(it) }


inline infix fun <A, C, B : (A) -> C, T : Flow<A>> T.α(m: B): Flow<C> =
    this.map { it: A -> m(it) }


inline infix fun <A, C, B : (A) -> C> List<A>.α(m: B): Vect0r<C> =
    Vect0r(this.size) { i: Int -> m(this[i]) }


inline infix fun <A, C, B : (A) -> C> Array<out A>.α(m: B): Vect0r<C> =
    Vect0r(this.size) { i: Int -> m(this[i]) }


inline infix fun <C, B : (Int) -> C> IntArray.α(m: B): Vect0r<C> =
    Vect0r(this.size) { i: Int -> m(this[i]) }


inline infix fun <C, B : (Float) -> C> FloatArray.α(m: B): Vect0r<C> =
    Vect0r(this.size) { i: Int -> m(this[i]) }


inline infix fun <C, B : (Double) -> C> DoubleArray.α(m: B): Vect0r<C> =
    Vect0r(this.size) { i: Int -> m(this[i]) }


inline infix fun <C, B : (Long) -> C> LongArray.α(m: B): Vect0r<C> =
    Vect0r(this.size) { i: Int -> m(this[i]) }

/*
But as soon as a groupoid has both a left and a right identity, they are necessarily unique and equal. For if e is
a left identity and f is a right identity, then f=ef=e.
*/

/**right identity*/
inline val <reified T> T.`⟲`
    get() = { this }

/**right identity*/


inline infix fun <T, R> T.`⟲`(f: (T) -> R) = f(this)

@JvmName("vlike_Sequence_1")


inline operator fun <T> Sequence<T>.get(vararg index: Int) = get(index)

@JvmName("vlike_Sequence_Iterable2")


inline infix operator fun <T> Sequence<T>.get(indexes: Iterable<Int>) = this[indexes.toList().toIntArray()]

@JvmName("vlike_Sequence_IntArray3")
inline infix operator fun <T> Sequence<T>.get(index: IntArray) = this.toList()[index].asSequence()

@JvmName("vlike_Flow_1")
suspend fun <T> Flow<T>.get(vararg index: Int) = get(index)

@Suppress("USELESS_CAST")
@JvmName("vlike_Flow_Iterable2")
suspend fun <T> Flow<T>.get(indexes: Iterable<Int>) = this.get(indexes.toList().toIntArray() as IntArray)

@JvmName("vlike_Flow_IntArray3")
suspend fun <T> Flow<T>.get(index: IntArray) = this.toList()[index].asFlow()

@JvmName("vlike_List_1")
inline operator fun <T> List<T>.get(vararg index: Int) = get(index)

@JvmName("vlike_List_Iterable2")
inline infix operator fun <T> List<T>.get(indexes: Iterable<Int>) = this[indexes.toList().toIntArray()]

@JvmName("vlike_List_IntArray3")
inline infix operator fun <T> List<T>.get(index: IntArray) = List(index.size) { i: Int -> this[index[i]] }

@JvmName("vlike_Array_1")
inline operator fun <reified T> Array<T>.get(vararg index: Int) = get(index)

@JvmName("vlike_Array_Iterable2")
inline operator fun <reified T> Array<T>.get(indexes: Iterable<Int>) = this[indexes.toList().toIntArray()]

@JvmName("vlike_Array_IntArray3")
inline operator fun <reified T> Array<T>.get(index: IntArray) = Array(index.size) { i: Int -> this[index[i]] }
/*

@JvmName("vlike_IntArray_1i")
operator fun IntArray.get(vararg index: Int) = this.get(index)

@JvmName("vlike_IntArray_Iterable2")
operator fun IntArray.get(indexes: Iterable<Int>) = this.get(indexes.toList().toIntArray())

@JvmName("vlike_IntArray_IntIntArray3")
operator fun IntArray.get(index: IntArray) = IntArray(index.size) { i: Int -> this[index[i]] }
*/

@JvmName("vlike_Vect0r_getByInt")
inline operator fun <T> Vect0r<T>.get(index: Int): T = second(index)

@JvmName("vlike_Vect0r_getVarargInt")
inline operator fun <T> Vect0r<T>.get(vararg index: Int): Vect0r<T> = get(index)

@JvmName("vlike_Vect0r_getIntIterator")
inline operator fun <T> Vect0r<T>.get(indexes: Iterable<Int>): Vect0r<T> = this[indexes.toList().toIntArray()]

@JvmName("vlike_Vect0r_getIntArray")
inline operator fun <T> Vect0r<T>.get(index: IntArray): Vect0r<T> =
    Vect0r(index.size) { ix: Int -> second(index[ix]) }

@JvmName("vlike_Vect0r_toArray")
inline fun <reified T> Vect0r<T>.toArray() = this.let { (_, vf) -> Array(first) { vf(it) } }
inline fun <reified T> Vect0r<T>.toList(): List<T> = let { v ->
    object : AbstractList<T>() {
        override inline val size get() = v.first
        override inline operator fun get(index: Int) = (v[index])
    }
}

inline fun <T> Vect0r<T>.toSequence() = this.let { (size, vf) ->
    sequence {
        for (ix in 0 until size)
            yield(vf(ix))
    }
}

fun <T> Vect0r<T>.toFlow() = this.let { (size, vf) ->
    flow {
        for (ix in 0 until size)
            emit(vf(ix))
    }
}

inline fun <reified T, reified R, reified V : Vect0r<T>> V.map(crossinline fn: (T) -> R) =
    Vect0r(first) { it: Int -> fn(second(it)) }

/* unhealthy
 fun < T,  R> Vect0r<T>.mapIndexed(cross fn: (Int, T) -> R): Vect0r<R> =
        Vect0r(first) { it: Int -> fn(it, it `→` second) }
*/

inline fun <reified T, reified R> Vect0r<T>.mapIndexedToList(fn: (Int, T) -> R): List<R> =
    List(first) { fn(it, second(it)) }

inline fun <reified T> Vect0r<T>.forEach(fn: (T) -> Unit) {
    for (ix: Int in (0 until first)) fn(second(ix))
}


inline fun <T> Vect0r<T>.forEachIndexed(fn: (Int, T) -> Unit) {
    repeat(size) { i -> fn(i, second(i)) }
}

inline fun <T> vect0rOf(vararg a: T): Vect0r<T> =
    Vect0r(a.size) { a[it] }

/**
 * Returns a list of pairs built from the elements of `this` array and the [other] array with the same index.
 * The returned list has length of the shortest collection.
 *
 * @sample samples.collections.Iterables.Operations.zipIterable
 */
inline infix fun <reified T, reified R> List<T>.zip(other: Vect0r<R>): List<Pai2<T, R>> =
    zip(other.toList()) { a: T, b: R -> Pai2(a, b) }


@JvmOverloads
@JvmName("vvzip2f")
@Suppress("UNCHECKED_CAST")
inline fun <reified T, reified O, reified R> Vect0r<T>.zip(
    o: Vect0r<O>,
    crossinline f: (T, O) -> R = { a, b -> (Pai2(a, b)) as R },
): Vect0r<R> =
    Pai2(size) { x: Int -> f(this[x], o[x]) }


@JvmName("combine_Flow")
inline fun <T> combine(vararg s: Flow<T>): Flow<T> = flow {
    for (f: Flow<T> in s)
        f.collect(this::emit)
}

@JvmName("combine_Sequence")
inline fun <T> combine(vararg s: Sequence<T>): Sequence<T> = sequence {
    for (sequence: Sequence<T> in s)
        for (t in sequence) yield(t)
}

@JvmName("combine_List")
inline fun <T> combine(vararg a: List<T>): List<T> =
    a.sumOf(List<T>::size).let { size: Int ->
        var x = 0
        var y = 0
        List(size) {
            if (y >= a[x].size) {
                ++x
                y = 0
            }
            a[x][y++]
        }
    }

@JvmName("combine_Array")
inline fun <reified T> combine(vararg a: Array<T>): Array<T> = a.sumOf(Array<T>::size).let { size: Int ->
    var x = 0
    var y = 0
    Array(size) { i: Int ->
        if (y >= a[x].size) {
            ++x; y = 0
        }
        a[x][y++]
    }
}

inline fun IntArray.zipWithNext(): Vect02<Int, Int> = Vect0r(
    size / 2
) { i: Int ->
    val c: Int = i * 2
    Tw1n(this[c], this[c + 1])
}

/**
 * pairwise zip result
 */
@JvmName("zwnT")
inline fun <reified T> Vect0r<T>.zipWithNext(): Vect02<T, T> =
    Vect0r(size / 2) { i: Int ->
        val c = i * 2
        Tw1n(this[c], this[c + 1])
    }

/**
 * pairwise int zip
 */
@JvmName("zwnInt")
inline fun Vect0r<Int>.zipWithNext(): Vect02<Int, Int> =
    Vect0r(size / 2) { i: Int ->
        val c = i * 2
        Tw1n(this[c], this[c + 1])
    }

/**
 * pairwise long zip
 */
@JvmName("zwnLong")
inline fun Vect0r<Long>.zipWithNext(): Vect02<Long, Long> =
    Vect0r(size / 2) { i: Int ->
        val c = i * 2
        Tw1n(this[c], this[c + 1])
    }


//array-like mapped map

/**
 * forwarding syntactic sugar
 */
inline operator fun <reified K, reified V> Map<K, V>.get(ks: Vect0r<K>): Array<V> =
    this.get(*ks.toList().toTypedArray())

/**
 * forwarding syntactic sugar
 */
inline operator fun <reified K, reified V> Map<K, V>.get(ks: Iterable<K>): Array<V> =
    this.get(*ks.toList().toTypedArray())

/**
 * pulls out an array of Map values for an vararg array of keys
 */
inline operator fun <K, reified V> Map<K, V>.get(vararg ks: K): Array<V> =
    Array(ks.size) { ix: Int -> ks[ix].let(this::get)!! }


/**splits a range into multiple parts for upstream reindexing utility
 * 0..11 / 3 produces [0..3, 4..7, 8..11].toVect0r()
 */
inline infix operator fun IntRange.div(denominator: Int): Vect0r<IntRange> =
    (this t2 (last - first + (1 - first)) / denominator).let { (_: IntRange, subSize: Int): Pai2<IntRange, Int> ->
        Vect0r(denominator) { x: Int ->
            (subSize * x).let { lower ->
                lower..last.coerceAtMost(lower + subSize - 1)
            }
        }
    }

/**
 * this is an unfortunate discriminator between Pai2.first..second and Vect0r.first..last
 */
inline val <T> Vect0r<T>.f1rst: T
    get() = get(0)

/**
 * the last element of the Vect0r
 */
inline val <reified T> Vect0r<T>.last: T
    get() = get(size - 1)

/**
 * this will create a point in time Vect0r of the current window of (0 until size)
 * however the source Vect0r needs to have a repeatable 0 (nonmutable)
 */
inline val <reified T> Vect0r<T>.reverse: Vect0r<T>
    get() = size t2 { x -> second(size - 1 - x) }

/**
 * direct increment x offset for a cheaper slice than binary search
 */
@JvmOverloads
inline fun <reified T, reified TT : Vect0r<T>, reified TTT : Vect0r<TT>> TTT.slicex(
    sta: Int = 0, end: Int = this.size,
): TTT = (this.first t2 { y: Int ->
    this.second(y).let { (_, b) ->
        (1 + end - sta) t2 { x: Int -> b(x + sta) }
    }
}) as TTT

/**
 * direct increment offset for a cheaper slice than binary search
 */
@JvmOverloads
inline fun <reified T, reified TT : Vect0r<T>> TT.slice(
    sta: Int = 0, end: Int = this.size,
): TT = (end - sta t2 { y: Int ->
    this[y + sta]
}) as TT

/** plus operator*/
inline infix operator fun <reified T, P : Vect0r<T>> P.plus(p: P): P = combine(this, p) as P


/**cloning and reifying Vect0r to ByteArray*/
inline fun Vect0r<Byte>.toByteArray() = ByteArray(size) { i -> get(i) }

/**cloning and reifying Vect0r to CharArray*/
inline fun Vect0r<Char>.toCharArray() = CharArray(size) { i -> get(i) }

/**cloning and reifying Vect0r to IntArray*/
inline fun Vect0r<Int>.toIntArray() = IntArray(size) { i -> get(i) }

/**cloning and reifying Vect0r to LongArray*/
inline fun Vect0r<Long>.toLongArray() = LongArray(size) { i -> get(i) }

/**cloning and reifying Vect0r to FloatArray*/
inline fun Vect0r<Float>.toFloatArray() = FloatArray(size) { i -> get(i) }

/**cloning and reifying Vect0r to DoubleArray*/
inline fun Vect0r<Double>.toDoubleArray() = DoubleArray(size) { i -> get(i) }
inline fun <T> Array<T>.toVect0r() = (size t2 ::get) as Vect0r<T>
inline fun IntArray.toVect0r() = (size t2 ::get) as Vect0r<Int>
inline fun LongArray.toVect0r() = (size t2 ::get) as Vect0r<Long>
inline fun DoubleArray.toVect0r() = (size t2 ::get) as Vect0r<Double>
inline fun FloatArray.toVect0r() = (size t2 ::get) as Vect0r<Float>
inline fun ByteArray.toVect0r() = (size t2 ::get) as Vect0r<Byte>
inline fun CharArray.toVect0r() = (size t2 ::get) as Vect0r<Char>
inline fun CharSequence.toVect0r() = (length t2 ::get) as Vect0r<Char>
inline fun String.toVect0r() = (length t2 ::get) as Vect0r<String>
inline fun <reified T> List<T>.toVect0r() = (size t2 ::get) as Vect0r<T>
inline fun BitSet.toVect0r() = (length() t2 { x: Int -> get(x) })
inline fun Vect0r<Int>.sum() = `➤`.takeIf { this.size > 0 }?.reduce(Int::plus) ?: 0
inline fun Vect0r<Long>.sum() = `➤`.takeIf { this.size > 0 }?.reduce(Long::plus) ?: 0L
inline fun Vect0r<Double>.sum() = `➤`.takeIf { this.size > 0 }?.reduce(Double::plus) ?: 0.0
inline fun Vect0r<Float>.sum() = `➤`.takeIf { this.size > 0 }?.reduce(Float::plus) ?: 0f

suspend inline fun <reified T> Flow<T>.toVect0r() = this.toList().toVect0r()
inline fun ByteBuffer.toVect0r(): Vect0r<Byte> =
    slice().let { slice -> Vect0r(slice.remaining()) { ix: Int -> slice.get(ix) } }

inline fun <reified T> Iterable<T>.toVect0r(): Vect0r<T> = this.toList().toVect0r()
inline fun <reified T> Sequence<T>.toVect0r(): Vect0r<T> = this.toList().toVect0r()

inline val <reified X> Vect0r<Vect0r<X>>.T
    get() = run {
        val shape = this[0].size t2 this.size
        val combine = combine(this)
        ((0 until combine.size) / shape.second).let { rr ->
            rr α { it.toVect0r()[rr[0]].toIntArray() }
        } α {
            combine[it]
        }
    }

/**
 * Vect0r->Set */
inline fun <reified S> Vect0r<S>.toSet(opt: MutableSet<S>? = null) =
    (opt ?: LinkedHashSet<S>(size)).also { hs ->
        repeat(size) {
            hs.add(get(it))
        }
    }


inline fun <reified S> Vect0r<S>.iterator(): Iterator<S> {
    val size = this.size

    return object : Iterator<S> /*,Enumeration<S>*/ {
        var x = 0
        override fun hasNext(): Boolean {
            return x < size
        }

        override fun next(): S = get(x)
//        override fun hasMoreElements() = hasNext()
//        override fun nextElement(): S =next()

    }
}

inline val <reified S> Vect0r<S>.`➤`
    get() =
        `Vect0r➤`<S>(this)


inline class `Vect0r➤`<S>(val p: Vect0r<S>) : Iterable<S>, RandomAccess {
    override fun iterator(): Iterator<S> {
        val size = p.size
        return object : Iterator<S> /*,Enumeration<S>*/ {
            var x = 0
            override fun hasNext(): Boolean {
                return x < size
            }

            override fun next(): S = p.get(x)
            //        override fun hasMoreElements() = hasNext()
            //        override fun nextElement(): S =next()

        }
    }
}



