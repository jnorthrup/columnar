package vec.macros

import kotlinx.coroutines.flow.*
import java.nio.ByteBuffer
import java.util.*
import kotlin.experimental.and

/**
 * semigroup
 */
typealias Vect0r<T> = Pai2<Int, (Int) -> T>

val <T>Vect0r<T>.size: Int get() = first
@Suppress("NonAsciiCharacters")
typealias Matrix<T> = Pai2<
        /**shape*/
        IntArray,
        /**accessor*/
            (IntArray) -> T>

operator fun <T> Matrix<T>.get(vararg c: Int): T = second(c)


infix fun <O, R, F : (O) -> R> O.`→`(f: F) = f(this)


operator fun <A, B, R, O : (A) -> B, G : (B) -> R> O.times(b: G): (A) -> R =
    { a: A -> a `→` this `→` b }


infix fun <A, B, R, O : (A) -> B, G : (B) -> R, R1 : (A) -> R> O.`→`(
    b: G,
): R1 = (this * b) as R1

/**
 * G follows F
 */
infix fun <A, B, C, G : (B) -> C, F : (A) -> B, R : (A) -> C> G.`⚬`(
    f: F,
): R = { a: A -> a `→` f `→` this } as R

/**
 * (λx.M[x]) → (λy.M[y])	α-conversion
 * https://en.wikipedia.org/wiki/Lambda_calculus
 * */
infix fun <A, C, B : (A) -> C, V : Vect0r<A>> V.α(m: B) = map(fn = m)


infix fun <A, C, B : (A) -> C, T : Iterable<A>> T.α(m: B): List<C> =
    this.map { it: A -> it `→` m }


infix fun <A, C, B : (A) -> C, T : Sequence<A>> T.α(m: B): Sequence<C> =
    this.map { it: A -> it `→` m }


infix fun <A, C, B : (A) -> C, T : Flow<A>> T.α(m: B): Flow<C> =
    this.map { it: A -> it `→` m }


infix fun <A, C, B : (A) -> C> List<A>.α(m: B): Vect0r<C> =
    Vect0r(this.size) { i: Int -> this[i] `→` m }


infix fun <A, C, B : (A) -> C> Array<out A>.α(m: B): Vect0r<C> =
    Vect0r(this.size) { i: Int -> this[i] `→` m }


infix fun <C, B : (Int) -> C> IntArray.α(m: B): Vect0r<C> =
    Vect0r(this.size) { i: Int -> this[i] `→` m }


infix fun <C, B : (Float) -> C> FloatArray.α(m: B): Vect0r<C> =
    Vect0r(this.size) { i: Int -> this[i] `→` m }


infix fun <C, B : (Double) -> C> DoubleArray.α(m: B): Vect0r<C> =
    Vect0r(this.size) { i: Int -> this[i] `→` m }


infix fun <C, B : (Long) -> C> LongArray.α(m: B): Vect0r<C> =
    Vect0r(this.size) { i: Int -> this[i] `→` m }

/*
But as soon as a groupoid has both a left and a right identity, they are necessarily unique and equal. For if e is
a left identity and f is a right identity, then f=ef=e.
*/

/**right identity*/
inline val <reified T> T.`⟲`
    get() = { this }

/**right identity*/


infix fun <T, R> T.`⟲`(f: (T) -> R) = run { f(this) }

@JvmName("vlike_Sequence_1")


operator fun <T> Sequence<T>.get(vararg index: Int) = get(index)

@JvmName("vlike_Sequence_Iterable2")


operator fun <T> Sequence<T>.get(indexes: Iterable<Int>) = this[indexes.toList().toIntArray()]

@JvmName("vlike_Sequence_IntArray3")
operator fun <T> Sequence<T>.get(index: IntArray) = this.toList()[index].asSequence()

@JvmName("vlike_Flow_1")
suspend fun <T> Flow<T>.get(vararg index: Int) = get(index)

@Suppress("USELESS_CAST")
@JvmName("vlike_Flow_Iterable2")
suspend fun <T> Flow<T>.get(indexes: Iterable<Int>) = this.get(indexes.toList().toIntArray() as IntArray)

@JvmName("vlike_Flow_IntArray3")
suspend fun <T> Flow<T>.get(index: IntArray) = this.toList()[index].asFlow()

@JvmName("vlike_List_1")
operator fun <T> List<T>.get(vararg index: Int) = get(index)

@JvmName("vlike_List_Iterable2")


operator fun <T> List<T>.get(indexes: Iterable<Int>) = this[indexes.toList().toIntArray()]

@JvmName("vlike_List_IntArray3")


operator fun <T> List<T>.get(index: IntArray) = List(index.size) { i: Int -> this[index[i]] }

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
operator fun <T> Vect0r<T>.get(index: Int): T = second(index)

@JvmName("vlike_Vect0r_getVarargInt")
operator fun <T> Vect0r<T>.get(vararg index: Int): Vect0r<T> = get(index)

@JvmName("vlike_Vect0r_getIntIterator")
operator fun <T> Vect0r<T>.get(indexes: Iterable<Int>): Vect0r<T> = this[indexes.toList().toIntArray()]

@JvmName("vlike_Vect0r_getIntArray")
operator fun <T> Vect0r<T>.get(index: IntArray): Vect0r<T> =
    Vect0r(index.size) { ix: Int -> second(index[ix]) }

@JvmName("vlike_Vect0r_toArray")
inline fun <reified T> Vect0r<T>.toArray() = this.let { (_, vf) -> Array(first) { vf(it) } }
fun <T> Vect0r<T>.toList(): List<T> = let { v ->
    object : AbstractList<T>() {
        override val size: Int = v.first
        override operator fun get(index: Int) = (v[index])
    }
}

fun <T> Vect0r<T>.toSequence() = this.let { (size, vf) ->
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

fun <T, R, V : Vect0r<T>> V.map(fn: (T) -> R) =
    Vect0r(first) { it: Int -> it `→` (fn `⚬` second) }

/* unhealthy
 fun < T,  R> Vect0r<T>.mapIndexed(cross fn: (Int, T) -> R): Vect0r<R> =
        Vect0r(first) { it: Int -> fn(it, it `→` second) }
*/

fun <T, R> Vect0r<T>.mapIndexedToList(fn: (Int, T) -> R): List<R> =
    List(first) { it: Int -> fn(it, it `→` second) }

fun <T> Vect0r<T>.forEach(fn: (T) -> Unit) {
    for (ix: Int in (0 until first)) ix `→` (fn `⚬` second)
}


fun <T> Vect0r<T>.forEachIndexed(fn: (Int, T) -> Unit) {
    repeat(size) { i -> fn(i, second(i)) }
}

fun <T> vect0rOf(vararg a: T): Vect0r<T> =
    Vect0r(a.size) { it: Int -> a[it] }

/**
 * Returns a list of pairs built from the elements of `this` array and the [other] array with the same index.
 * The returned list has length of the shortest collection.
 *
 * @sample samples.collections.Iterables.Operations.zipIterable
 */
inline infix fun <reified T, reified R> List<T>.zip(other: Vect0r<R>): List<Pai2<T, R>> =
    zip(other.toList()) { a: T, b: R -> a t2 b }


@JvmOverloads
@JvmName("vvzip2f")
@Suppress("UNCHECKED_CAST")
inline fun <reified T, reified O, reified R> Vect0r<T>.zip(
    o: Vect0r<O>,
    crossinline f: (T, O) -> R = { a, b -> (a t2 b) as R },
): Vect0r<R> =
    size t2 { x: Int -> f(this[x], o[x]) }


@JvmName("combine_Flow")
fun <T> combine(vararg s: Flow<T>): Flow<T> = flow {
    for (f: Flow<T> in s)
        f.collect(this::emit)
}

@JvmName("combine_Sequence")
fun <T> combine(vararg s: Sequence<T>): Sequence<T> = sequence {
    for (sequence: Sequence<T> in s)
        for (t in sequence) yield(t)
}

@JvmName("combine_List")
fun <T> combine(vararg a: List<T>): List<T> =
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

fun IntArray.zipWithNext(): Vect02<Int, Int> = Vect0r(
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
fun Vect0r<Int>.zipWithNext(): Vect02<Int, Int> =
    Vect0r(size / 2) { i: Int ->
        val c = i * 2
        Tw1n(this[c], this[c + 1])
    }

/**
 * pairwise long zip
 */
@JvmName("zwnLong")
fun Vect0r<Long>.zipWithNext(): Vect02<Long, Long> =
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
infix operator fun IntRange.div(denominator: Int): Vect0r<IntRange> =
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
val <T> Vect0r<T>.f1rst: T
    get() = get(0)

/**
 * the last element of the Vect0r
 */
val <T> Vect0r<T>.last: T
    get() = get(size - 1)

/**
 * this will create a point in time Vect0r of the current window of (0 until size)
 * however the source Vect0r needs to have a repeatable 0 (nonmutable)
 */
val <T> Vect0r<T>.reverse: Vect0r<T>
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
fun Vect0r<Byte>.toByteArray() = ByteArray(size) { i -> get(i) }

/**cloning and reifying Vect0r to CharArray*/
fun Vect0r<Char>.toCharArray() = CharArray(size) { i -> get(i) }

/**cloning and reifying Vect0r to IntArray*/
fun Vect0r<Int>.toIntArray() = IntArray(size) { i -> get(i) }

/**cloning and reifying Vect0r to LongArray*/
fun Vect0r<Long>.toLongArray() = LongArray(size) { i -> get(i) }

/**cloning and reifying Vect0r to FloatArray*/
fun Vect0r<Float>.toFloatArray() = FloatArray(size) { i -> get(i) }

/**cloning and reifying Vect0r to DoubleArray*/
fun Vect0r<Double>.toDoubleArray() = DoubleArray(size) { i -> get(i) }

fun <T> Array<T>.toVect0r() = (size t2 ::get) as Vect0r<T>
fun IntArray.toVect0r() = (size t2 ::get) as Vect0r<Int>
fun LongArray.toVect0r() = (size t2 ::get) as Vect0r<Long>
fun DoubleArray.toVect0r() = (size t2 ::get) as Vect0r<Double>
fun FloatArray.toVect0r() = (size t2 ::get) as Vect0r<Float>
fun ByteArray.toVect0r() = (size t2 ::get) as Vect0r<Byte>
fun CharArray.toVect0r() = (size t2 ::get) as Vect0r<Char>
fun CharSequence.toVect0r() = (length t2 ::get) as Vect0r<Char>
fun String.toVect0r() = (length t2 ::get) as Vect0r<String>
fun <T> List<T>.toVect0r() = (size t2 ::get) as Vect0r<T>
fun BitSet.toVect0r() = (length() t2 { x: Int -> get(x) })

fun Vect0r<Int>.sum() = `➤`.reduce(Int::plus)
fun Vect0r<Long>.sum() = `➤`.reduce(Long::plus)
fun Vect0r<Double>.sum() = `➤`.reduce(Double::plus)
fun Vect0r<Float>.sum() = `➤`.reduce(Float::plus)

suspend fun <T> Flow<T>.toVect0r() = this.toList().toVect0r()
fun ByteBuffer.toVect0r(): Vect0r<Byte> =
    slice().let { slice -> Vect0r(slice.remaining()) { ix: Int -> slice.get(ix) } }

fun <T> Iterable<T>.toVect0r(): Vect0r<T> = this.toList().toVect0r()
fun <T> Sequence<T>.toVect0r(): Vect0r<T> = this.toList().toVect0r()

inline val <reified X> Vect0r<Vect0r<X>>.T: Vect0r<Vect0r<X>>
    get() = combine(this).toList().let { c ->
        (size t2 (f1rst.size)).let { (fs, count) ->
            count t2 { x: Int ->
                (c).slice((x * fs) until ((x + 1) * fs)).toVect0r()
            }
        }
    }

/**
 * Vect0r->Set */
inline fun <reified S> Vect0r<S>.toSet(opt: MutableSet<S>? = null) = (opt ?: LinkedHashSet<S>(size)).also { hs ->
    repeat(size) {
        hs.add(get(it))
    }
}


fun <S> Vect0r<S>.iterator(): Iterator<S> {
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


@JvmInline
value class `Vect0r➤`<S>(val p: Vect0r<S>) : Iterable<S>, RandomAccess {
    override inline fun iterator() = p.iterator()
}
