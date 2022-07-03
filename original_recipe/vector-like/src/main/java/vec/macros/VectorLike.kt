@file:Suppress(
        "OVERRIDE_BY_INLINE", "NOTHING_TO_INLINE", "FunctionName", "FINAL_UPPER_BOUND",
        "UNCHECKED_CAST", "NonAsciiCharacters", "KDocUnresolvedReference", "ObjectPropertyName", "ClassName"
)

package vec.macros

import kotlinx.coroutines.flow.*
import vec.macros.Vect02_.left
import java.lang.Integer.min
import java.nio.ByteBuffer
import java.util.*
import kotlin.math.max

/**
 * semigroup
 */
typealias Vect0r<reified T> = Pai2<Int, ((Int) -> T)>
typealias   Vect02<F, S> = Vect0r<Pai2<F, S>>
typealias  V3ct0r<F, S, T> = Vect0r<Tripl3<F, S, T>>

val <T>Vect0r<T>.size: Int get() = first
@Suppress("NonAsciiCharacters")
typealias Matrix<T> = Pai2<
        /**shape*/
        IntArray,
        /**accessor*/
        (IntArray) -> T>

operator fun <T> Matrix<T>.get(vararg c: Int): T = second(c)


inline infix fun <reified O, reified R, F : (O) -> R> O.`→`(f: F): R = f(this)


operator fun <A, B, R, O : (A) -> B, G : (B) -> R> O.times(b: G): (A) -> R = { a: A -> b(this(a)) }


infix fun <A, B, R, O : (A) -> B, G : (B) -> R, R1 : (A) -> R> O.`→`(
        b: G,
): R1 = { a: A -> b(this(a)) } as R1

/**
 * G follows F
 */
inline infix fun <reified A, reified B, reified C, G : (B) -> C, F : (A) -> B, R : (A) -> C> G.`⚬`(
        f: F,
): R = { a: A -> a `→` f `→` this } as R

/**
 * (λx.M[x]) → (λy.M[y])	α-conversion
 * https://en.wikipedia.org/wiki/Lambda_calculus
 * */
inline infix fun <A, reified C, B : (A) -> C, V : Vect0r<A>> V.α(m: B) = map(m)


inline infix fun <A, reified C, B : (A) -> C, T : Iterable<A>> T.α(m: B) =
        this.map { m(it) }.toVect0r()

infix fun <A, C, B : (A) -> C> List<A>.α(m: B) =
        this.size t2 { i: Int -> m(this[i]) }


inline infix fun <A, reified C, B : (A) -> C> Array<out A>.α(m: B) =
        this.size t2 { i: Int -> m(this[i]) }


inline infix fun <reified C, B : (Int) -> C> IntArray.α(m: B) =
        (this.size) t2 { i: Int -> m(this[i]) }


inline infix fun <reified C, B : (Float) -> C> FloatArray.α(m: B) =
        (this.size) t2 { i: Int -> m(this[i]) }


inline infix fun <reified C, B : (Double) -> C> DoubleArray.α(m: B) = this.size t2 { i: Int -> m(this[i]) }


inline infix fun <reified C, B : (Long) -> C> LongArray.α(m: B) =
        Vect0r(this.size) { i: Int -> this[i] `→` m }

/*
But as soon as a groupoid has both a left and a right identity, they are necessarily unique and equal. For if e is
a left identity and f is a right identity, then f=ef=e.
*/

/**right identity*/
inline val <reified T> T.`⟲`
    get() = { this }

/*
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
suspend fun <T> Flow<T>.get(index: IntArray) = this.toList()[index].asFlow()*/

@JvmName("vlike_List_1")
inline operator fun <reified T> List<T>.get(vararg index: Int) = get(index)

@JvmName("vlike_List_Iterable2")
inline operator fun <reified T> List<T>.get(indexes: Iterable<Int>): Vect0r<T> = this.get(indexes.toList().toIntArray())

@JvmName("vlike_List_IntArray3")
inline operator fun <reified T> List<T>.get(index: IntArray): Vect0r<T> = index α ::get

@JvmName("vlike_Array_1")
inline operator fun <reified T> Array<T>.get(vararg index: Int): Vect0r<T> = index α ::get

@JvmName("vlike_Array_Iterable2")


inline operator fun <reified T> Array<T>.get(index: Iterable<Int>) = index α ::get

@JvmName("vlike_Array_IntArray3")
inline operator fun <reified T> Array<T>.get(index: IntArray) = index α ::get
/*

@JvmName("vlike_IntArray_1i")
operator fun IntArray.get(vararg index: Int) = this.get(index)

@JvmName("vlike_IntArray_Iterable2")
operator fun IntArray.get(indexes: Iterable<Int>) = this.get(indexes.toList().toIntArray())

@JvmName("vlike_IntArray_IntIntArray3")
operator fun IntArray.get(index: IntArray) = IntArray(index.size) { i: Int -> this[index[i]] }
*/

@JvmName("vlike_Vect0r_getByInt")
inline operator fun <reified T> Vect0r<T>.get(index: Int) = second(index)

@JvmName("vlike_Vect0r_getVarargInt")
operator fun <T> Vect0r<T>.get(vararg index: Int) = get(index)

@JvmName("vlike_Vect0r_getIntIterator")
operator fun <T> Vect0r<T>.get(indexes: Iterable<Int>) = this[indexes.toList().toIntArray()]

@JvmName("vlike_Vect0r_getIntArray")
operator fun <T> Vect0r<T>.get(index: IntArray) =
        Vect0r(index.size) { ix: Int -> second(index[ix]) }

@JvmName("vlike_Vect0r_toArray")
inline fun <reified T> Vect0r<T>.toArray() = this.let { (_, vf) -> Array(first) { vf(it) } }
inline fun <reified T> Vect0r<T>.toList(): List<T> = let { v ->
    object : AbstractList<T>() {
        override inline val size get() = v.first
        override inline operator fun get(index: Int) = v.second(index)
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
        Vect0r(this.size) { it: Int -> fn(second(it)) }

/* unhealthy
 fun < T,  R> Vect0r<T>.mapIndexed(cross fn: (Int, T) -> R): Vect0r<R> =
        Vect0r(first) { it: Int -> fn(it, it `→` second) }
*/

fun <T, R> Vect0r<T>.mapIndexedToList(fn: (Int, T) -> R) = List(first) { fn(it, second(it)) }

fun <T> Vect0r<T>.forEach(fn: (T) -> Unit) = repeat(size) { fn(second(it)) }

fun <T> Vect0r<T>.forEachIndexed(f: (Int, T) -> Unit) = repeat(size) { f(it, second(it)) }

fun <T> vect0rOf(vararg a: T): Vect0r<T> = Vect0r(a.size) { a[it] }

/**
 * Returns a list of pairs built from the elements of `this` array and the [other] array with the same index.
 * The returned list has length of the shortest collection.
 *
 * @sample samples.collections.Iterables.Operations.zipIterable
 */
inline infix fun <reified T, reified R> List<T>.zip(other: Vect0r<R>): List<Pai2<T, R>> =
        zip(other.toList()) { a: T, b: R -> a t2 b }


@JvmName("vvzip2f")
@Suppress("UNCHECKED_CAST")
inline fun <reified T, reified O, reified R> Vect0r<T>.zip(
        o: Vect0r<O>,
        crossinline f: (T, O) -> R,
): Vect0r<R> = size t2 { x: Int -> f(this[x], o[x]) }

@JvmName("vvzip2")
@Suppress("UNCHECKED_CAST")
inline infix fun <reified T, reified O, R : Vect02<T, O>> Vect0r<T>.zip(o: Vect0r<O>): R =
        (min(size, o.size) t2 { x: Int -> (this[x] t2 o[x]) }) as R


@JvmName("combine_Flow")
fun <T> combine(vararg s: Flow<T>): Flow<T> = flow {
    for (f: Flow<T> in s)
        f.collect(this::emit)
}

@JvmName("combine_Sequence")
fun <T> combine(vararg s: Sequence<T>): Sequence<T> = sequence {
    for (sequence: Sequence<T> in s)
        for (t in sequence)
            yield(t)
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
    Array(size) { _: Int ->
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
        Array(ks.size) { ix: Int -> get(ks[ix])!! }


/**splits a range into multiple parts for upstream reindexing utility
 * 0..11 / 3 produces [0..3, 4..7, 8..11].toVect0r()
 *
 * in order to dereference these vectors, invert the applied order
 *  val r=(0 until 743 * 347 * 437) / 437 / 347 / 743
 *
 *  the order to access these is the r[..743][..347][..437]
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
 * returns
 * Vect0r<T> / x= Vecto<Vector<T>> in x parts
 */
inline infix operator fun <reified T> Vector<T>.div(denominator: Int): Pai2<Int, (Int) -> Pai2<Int, (Int) -> T>> =
        (0 until this.size).div(denominator).α { rnge ->
            (this as Vect0r<T>).slice(rnge.first, rnge.endInclusive)
        }


/**
 * this is an unfortunate discriminator between the Int Pai2.first..second  and the Any Vect0r[0]
 */
inline val <reified T> Vect0r<T>.f1rst: T
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
val <T> Vect0r<T>.reverse: Vect0r<T>
    get() = size t2 { x -> second(size - 1 - x) }

/**
 * direct increment x offset for a cheaper slice than binary search
 */
@JvmOverloads
inline fun <reified T, reified TT : Vect0r<T>, reified TTT : Vect0r<TT>> TTT.slicex(
        start: Int = 0, endExclusive: Int = max(this.size, start),
): TTT = (this.first t2 { y: Int ->
    this.second(y).let { (_, b) ->
        (1 + endExclusive - start) t2 { x: Int -> b(x + start) }
    }
}) as TTT

/**
 * direct increment offset for a cheaper slice than binary search
 */
@JvmOverloads
inline fun <reified T> Vect0r<T>.slice(
        start: Int = 0,
        endExclusive: Int = this.size,
): Vect0r<T> = (endExclusive - start) t2 { x: Int -> this[x + start] }


/** plus operator*/
inline infix operator fun <reified T, P : Vect0r<T>> P.plus(p: P) = combine(this, p) as P

/** plus operator*/
inline infix operator fun <reified T, P : Vect0r<T>> P.plus(t: T) = combine(this, (1 t2 t.`⟲`) as P) as P


/**cloning and reifying Vect0r to ByteArray*/
inline fun <reified T : Byte> Vect0r<T>.toByteArray() = ByteArray(size) { i -> get(i) }

/**cloning and reifying Vect0r to CharArray*/
inline fun <reified T : Char> Vect0r<T>.toCharArray() = CharArray(size) { i -> get(i) }

/**cloning and reifying Vect0r to IntArray*/
inline fun <reified T : Int> Vect0r<T>.toIntArray() = IntArray(size) { i -> get(i) }

/**cloning and reifying Vect0r to LongArray*/
inline fun <reified T : Long> Vect0r<T>.toLongArray() = LongArray(size) { i -> get(i) }

/**cloning and reifying Vect0r to FloatArray*/
inline fun <reified T : Float> Vect0r<T>.toFloatArray() = FloatArray(size) { i -> get(i) }

/**cloning and reifying Vect0r to DoubleArray*/
inline fun <reified T : Double> Vect0r<T>.toDoubleArray() = DoubleArray(size) { i -> get(i) }

inline fun <reified T> Array<T>.toVect0r() = (size t2 ::get) as Vect0r<T>
inline fun <reified T> List<T>.toVect0r() = (size t2 ::get) as Vect0r<T>
inline fun IntArray.toVect0r() = (size t2 ::get) as Vect0r<Int>
inline fun LongArray.toVect0r() = (size t2 ::get) as Vect0r<Long>
inline fun DoubleArray.toVect0r() = (size t2 ::get) as Vect0r<Double>
inline fun FloatArray.toVect0r() = (size t2 ::get) as Vect0r<Float>
inline fun ByteArray.toVect0r() = (size t2 ::get) as Vect0r<Byte>
inline fun CharArray.toVect0r() = (size t2 ::get) as Vect0r<Char>
inline fun CharSequence.toVect0r() = (length t2 ::get) as Vect0r<Char>
inline fun String.toVect0r() = (length t2 ::get) as Vect0r<String>
inline fun <T : Boolean> BitSet.toVect0r() = (length() t2 { it: Int -> get(it) })


fun Vect0r<Int>.sum() = `➤`.takeIf { this.size > 0 }?.reduce(Int::plus) ?: 0
fun Vect0r<Long>.sum() = `➤`.takeIf { this.size > 0 }?.reduce(Long::plus) ?: 0L

fun Vect0r<Double>.sum() = `➤`.takeIf { this.size > 0 }?.reduce(Double::plus) ?: 0.0
fun Vect0r<Float>.sum() = `➤`.takeIf { this.size > 0 }?.reduce(Float::plus) ?: 0f

suspend inline fun <reified T> Flow<T>.toVect0r() = this.toList().toVect0r()
fun ByteBuffer.toVect0r(): Vect0r<Byte> =
        slice().let { slice -> Vect0r(slice.remaining()) { ix: Int -> slice.get(ix) } }

inline fun <reified T> Iterable<T>.toVect0r(): Vect0r<T> = this.toList() α { it }
inline fun <reified T> Sequence<T>.toVect0r(): Vect0r<T> = this.toList() α { it }

inline val <reified X> Vect0r<Vect0r<X>>.T
    get() = run {
        val shape = this[0].size t2 this.size
        val combine = combine(this)
        ((0 until combine.size) / shape.second).let { rr ->
            rr[0].map { y ->
                rr.map {
                    val i = it.toList()[y]
                    i
                }.toIntArray()

            } α combine::get
        }
    }

/**
 * Vect0r->Set */
inline fun <reified S> Vect0r<S>.toSet(opt: MutableSet<S>? = null) = (opt ?: LinkedHashSet<S>(size)).also { hs ->
    repeat(size) {
        hs.add(get(it))
    }
}


inline fun <reified S> Vect0r<S>.iterator(): Iterator<S> {
    return object : Iterator<S> {
        var x = 0
        override inline fun hasNext(): Boolean = x < size
        override inline fun next(): S = get(x++)
    }
}

/**
 * wrapper for for loops and iterable filters
 */
inline val <reified T> Vect0r<T>.`➤`: Iterable<T>
    @JvmName("iteratable")
    get() = object : Iterable<T> {
        override fun iterator(): Iterator<T> = object : Iterator<T> {
            var t = 0
            override fun hasNext(): Boolean = t < first
            override fun next(): T = second(t++)
        }
    }


/**
 * index by enum
 */
@JvmName("getIndexByEnum")
inline operator fun <reified S, reified E : Enum<E>> Vect0r<S>.get(e: E) = get(e.ordinal)

/**
 * optimize for where a smallish map has hotspots that are over-used and others that are excess overhead
 * in the more expensive things that old code does with maps
 */
@JvmName("sparseVect0rMap")
inline fun <reified K : Int, reified V> Map<K, V>.sparseVect0rMap(): Vect0r<V?> = let { top ->
    ((this as? SortedMap)?.keys ?: keys.sorted()).toIntArray().let { k ->
        0 t2 if (top.size <= 16)
            { x: Int ->
                var r: V? = null
                var i = 0
                do {
                    if (k[i++] == x) r = top[x]
                } while (i < size && r == null)
                r.also {
                    assert(it == top[x])
                }
            } else { x: Int ->
            k.binarySearch(x).takeUnless { 0 < it }?.let { i ->
                top[x].also {
                    assert(it == top[x])
                }
            }
        }
    }
}


/**
 * pay once for the conversion from a mutable map to an array map and all that implies
 */
@JvmName("sparseVect0r")
inline fun <reified K : Int, reified V> Map<K, V>.sparseVect0r(): Vect0r<V?> = let { top ->
    ((this as? SortedMap)?.entries ?: entries.sortedBy { it.key }).toTypedArray().let { entries ->
        val k = keys.toIntArray()
        0 t2 if (top.size <= 16)
            { x: Int ->
                var r: V? = null
                var i = 0
                do {
                    if (k[i++] == x) r = entries[i].value
                } while (i < size && r == null)
                r.also { assert(it == top[x]) }
            } else { x: Int ->
            k.binarySearch(x).takeUnless { 0 < it }?.let { i ->
                (entries[i].value).also {
                    assert(it == top[x])
                }
            }
        }
    }
}

@JvmOverloads
inline fun <reified F, reified S> left(v2: Vect02<F, S>) {
    v2.left
}

object Vect02_ {
    inline val <reified F, reified S> Vect02<F, S>.left get() = this α Pai2<F, S>::first
    inline val <reified F, reified S> Vect02<F, S>.right get() = this α Pai2<F, S>::second
    inline val <reified F, reified S> Vect02<F, S>.reify get() = this α Pai2<F, S>::pair

    inline fun <reified F, reified S> Vect02<F, S>.toMap(theMap: MutableMap<F, S>? = null) =
            toList().map(Pai2<F, S>::pair).let { paris ->
                (if (theMap != null) theMap.let { paris.toMap(theMap as MutableMap<in F, in S>) } else paris.toMap(
                        HashMap(
                                paris.size
                        )
                ))
            }
}

object V3ct0r_ {
    inline val <reified F, reified S, reified T> V3ct0r<F, S, T>.left get() = this α Tripl3<F, *, *>::first
    inline val <reified F, reified S, reified T> V3ct0r<F, S, T>.mid get() = this α Tripl3<F, S, T>::second
    inline val <reified F, reified S, reified T> V3ct0r<F, S, T>.right get() = this α Tripl3<F, S, T>::third
    inline val <reified F, reified S, reified T> V3ct0r<F, S, T>.reify get() = this α Tripl3<F, S, T>::triple
    inline val <reified F, reified S, reified T> V3ct0r<F, S, T>.x get() = this α (Tripl3<F, S, T>::first)
    inline val <reified F, reified S, reified T> V3ct0r<F, S, T>.y get() = this α (Tripl3<F, S, T>::second)
    inline val <reified F, reified S, reified T> V3ct0r<F, S, T>.z get() = this α (Tripl3<F, S, T>::third)
    inline val <reified F, reified S, reified T> V3ct0r<F, S, T>.r3ify get() = this α (Tripl3<F, S, T>::triple)
}

operator fun <T> Vect0r<T>.div(d: Int): Vect0r<Vect0r<T>> = (0 until size) / d α { this[it] }
