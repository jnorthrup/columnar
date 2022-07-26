@file:Suppress(
        "OVERRIDE_BY_INLINE", "NOTHING_TO_INLINE", "FunctionName", "FINAL_UPPER_BOUND",
        "UNCHECKED_CAST", "NonAsciiCharacters", "KDocUnresolvedReference", "ObjectPropertyName", "ClassName"
)

package vec.macros

import kotlinx.coroutines.flow.*
import vec.macros.Vect02_.left
import vec.util.debug
import java.lang.Integer.*
import java.nio.ByteBuffer
import java.util.*
import kotlin.math.max

/**
 * semigroup
 */
typealias Vect0r<reified T> = Pai2<Int, ((Int) -> T)>
typealias   Vect02<F, S> = Vect0r<Pai2<F, S>>
typealias  V3ct0r<F, S, T> = Vect0r<Tripl3<F, S, T>>

inline val <T>  Vect0r<T>.size: Int get() = first
@Suppress("NonAsciiCharacters")
typealias Matrix<T> = Pai2<
        /**shape*/
        IntArray,
        /**accessor*/
        (IntArray) -> T>

operator fun <T> Matrix<T>.get(vararg c: Int): T = second(c)

infix fun <O, R, F : (O) -> R> O.`→`(f: F): R = f(this)

operator fun <A, B, R, O : (A) -> B, G : (B) -> R> O.times(b: G): (A) -> R = { a: A -> b(this(a)) }

infix fun <A, B, R, O : (A) -> B, G : (B) -> R, R1 : (A) -> R> O.`→`(
        b: G,
): R1 = { a: A -> b(this(a)) } as R1

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
infix fun <A, C, B : (A) -> C, V : Vect0r<A>> V.α(m: B): Pai2<Int, (Int) -> C> = map(m)


infix fun <A, C, B : (A) -> C, T : Iterable<A>> T.α(m: B): Vect0r<C> =
        this.map { m(it) }.toVect0r()

infix fun <A, C, B : (A) -> C> List<A>.α(m: B): Pai2<Int, (Int) -> C> =
        this.size t2 { i: Int -> m(this[i]) }


infix fun <A, C, B : (A) -> C> Array<out A>.α(m: B): Pai2<Int, (Int) -> C> =
        this.size t2 { i: Int -> m(this[i]) }

infix fun <C, B : (Int) -> C> IntArray.α(m: B): Pai2<Int, (Int) -> C> =
        (this.size) t2 { i: Int -> m(this[i]) }

infix fun <C, B : (Float) -> C> FloatArray.α(m: B): Pai2<Int, (Int) -> C> =
        (this.size) t2 { i: Int -> m(this[i]) }

infix fun <C, B : (Double) -> C> DoubleArray.α(m: B): Pai2<Int, (Int) -> C> = this.size t2 { i: Int -> m(this[i]) }

infix fun <C, B : (Long) -> C> LongArray.α(m: B): Pai2<Int, (Int) -> C> =
        Vect0r(this.size) { i: Int -> this[i] `→` m }

/*
But as soon as a groupoid has both a left and a right identity, they are necessarily unique and equal. For if e is
a left identity and f is a right identity, then f=ef=e.
*/

val <T> T.rightIdentity: () -> T get() = { this }

/**right identity*/
val <T> T.`⟲`: () -> T get() = rightIdentity

/**
 * Vect0r->Set */
fun <S> Vect0r<S>.toSet(opt: MutableSet<S>? = null): MutableSet<S> = (opt
        ?: LinkedHashSet<S>(size)).also { hs -> hs.addAll(this.`➤`) }

@JvmInline
value class IterableVect0r<T>(val v: Vect0r<T>) : Iterable<T>, Vect0r<T> {
    inline override fun iterator(): Iterator<T> = v.iterator()
    inline override val first: Int
        inline get() = v.first
    inline override val second: (Int) -> T
        inline get() = v.second
}

operator fun <T> Vect0r<T>.iterator(): Iterator<T> = object : Iterator<T> {
    var t = 0
    override fun hasNext(): Boolean = t < size
    override fun next(): T = second(t++)
}

@JvmName("vlike_List_1")
inline operator fun <reified T> List<T>.get(vararg index: Int): Vect0r<T> = get(index)

@JvmName("vlike_List_Iterable2")
inline operator fun <reified T> List<T>.get(indexes: Iterable<Int>): Pai2<Int, (Int) -> T> = this[indexes.toList().toIntArray()]

@JvmName("vlike_List_IntArray3")
inline operator fun <reified T> List<T>.get(index: IntArray): Vect0r<T> = index α ::get

@JvmName("vlike_Array_1")
operator fun <T> Array<T>.get(vararg index: Int): Vect0r<T> = index α ::get

@JvmName("vlike_Array_Iterable2")
operator fun <T> Array<T>.get(index: Iterable<Int>): Vect0r<T> = index α ::get

@JvmName("vlike_Array_IntArray3")
operator fun <T> Array<T>.get(index: IntArray): Pai2<Int, (Int) -> T> = index α ::get

@JvmName("vlike_Vect0r_getByInt")
operator fun <T> Vect0r<T>.get(index: Int): T = second(index)

@JvmName("vlike_Vect0r_getVarargInt")
operator fun <T> Vect0r<T>.get(vararg index: Int): Pai2<Int, (Int) -> T> = get(index)

@JvmName("vlike_Vect0r_getIntIterator")
operator fun <T> Vect0r<T>.get(indexes: Iterable<Int>): Pai2<Int, (Int) -> T> = this[indexes.toList().toIntArray()]

@JvmName("vlike_Vect0r_getIntArray")
operator fun <T> Vect0r<T>.get(index: IntArray): Pai2<Int, (Int) -> T> = Vect0r(index.size) { ix: Int -> second(index[ix]) }

@JvmName("vlike_Vect0r_toArray")
inline fun <reified T> Vect0r<T>.toArray() = this.let { (_, vf) -> Array<T>(first) { vf(it) } }


fun <T> Vect0r<T>.toList(): List<T> = let { v ->
    object : AbstractList<T>() {
        override inline val size get() = v.first
        override inline operator fun get(index: Int) = v.second(index)
    }
}

fun <T> Vect0r<T>.toSequence(): Sequence<T> = this.let { (size, vf) ->
    sequence {
        for (ix in 0 until size)
            yield(vf(ix))
    }
}

fun <T> Vect0r<T>.toFlow(): Flow<T> = this.let { (size, vf) ->
    flow {
        for (ix in 0 until size)
            emit(vf(ix))
    }
}

fun <T, R, V : Vect0r<T>> V.map(fn: (T) -> R): Pai2<Int, (Int) -> R> =
        Vect0r(this.size) { it: Int -> fn(second(it)) }

/* unhealthy
 fun < T,  R> Vect0r<T>.mapIndexed(cross fn: (Int, T) -> R): Vect0r<R> =
        Vect0r(first) { it: Int -> fn(it, it `→` second) }
*/

fun <T, R> Vect0r<T>.mapIndexedToList(fn: (Int, T) -> R): List<R> = List(first) { fn(it, second(it)) }

fun <T> Vect0r<T>.forEach(fn: (T) -> Unit): Unit = repeat(size) { fn(second(it)) }

fun <T> Vect0r<T>.forEachIndexed(f: (Int, T) -> Unit): Unit = repeat(size) { f(it, second(it)) }

fun <T> vect0rOf(vararg a: T): Vect0r<T> = Vect0r(a.size) { a[it] }

/**
 * Returns a list of pairs built from the elements of `this` array and the [other] array with the same index.
 * The returned list has length of the shortest collection.
 *
 * @sample samples.collections.Iterables.Operations.zipIterable
 */
infix fun <T, R> List<T>.zip(other: Vect0r<R>): List<Pai2<T, R>> =
        zip(other.`➤`) { a: T, b: R -> a t2 b }

@JvmName("vvzip2f")
fun <T, O, R> Vect0r<T>.zip(o: Vect0r<O>, f: (T, O) -> R) = size t2 { x: Int -> f(this[x], o[x]) }

@JvmName("vvzip2")
@Suppress("UNCHECKED_CAST")
infix fun <T, O, R : Vect02<T, O>> Vect0r<T>.zip(o: Vect0r<O>): R =
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
fun <T> Vect0r<T>.zipWithNext(): Vect02<T, T> =
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
inline operator fun <reified K, reified V> Map<K, V>.get(ks: Vect0r<K>): Array<V> = this.get(*ks.toList().toTypedArray())

/**
 * forwarding syntactic sugar
 */
inline operator fun <reified K, reified V> Map<K, V>.get(ks: Iterable<K>): Array<V> = this.get(*ks.toList().toTypedArray())

/**
 * pulls out an array of Map values for an vararg array of keys
 */
inline operator fun <K, reified V> Map<K, V>.get(vararg ks: K): Array<V> = Array(ks.size) { ix: Int -> get(ks[ix])!! }


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
infix operator fun <T> Vector<T>.div(denominator: Int): Pai2<Int, (Int) -> Pai2<Int, (Int) -> T>> =
        (0 until this.size).div(denominator).α { rnge ->
            (this as Vect0r<T>).slice(rnge.first, rnge.last)
        }


/**
 * this is an unfortunate discriminator between the Int Pai2.first..second  and the Vect0r[0]
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
    get() = this.size t2 { x -> second(size - 1 - x) }

/**
 * direct increment x offset for a cheaper slice than binary search
 */
@JvmOverloads
fun <T, TT : Vect0r<T>, TTT : Vect0r<TT>> TTT.slicex(
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
@JvmName("vect0rSlice")
fun <T> Vect0r<T>.slice(
        start: Int = 0,
        endExclusive: Int = this.size,
): Vect0r<T> = (endExclusive - start) t2 { x: Int -> this[x + start] }


/** plus operator*/
infix operator fun <T, P : Vect0r<T>> P.plus(p: P): P = combine(this, p) as P

/** plus operator*/
infix operator fun <T, P : Vect0r<T>> P.plus(t: T): P = combine(this, (1 t2 t.`⟲`) as P) as P


/**cloning and reifying Vect0r to ByteArray*/
fun <T : Byte> Vect0r<T>.toByteArray(): ByteArray = ByteArray(size) { i -> get(i) }

/**cloning and reifying Vect0r to CharArray*/
fun <T : Char> Vect0r<T>.toCharArray(): CharArray = CharArray(size) { i -> get(i) }

/**cloning and reifying Vect0r to IntArray*/
fun <T : Int> Vect0r<T>.toIntArray(): IntArray = IntArray(size) { i -> get(i) }

/**cloning and reifying Vect0r to IntArray*/
fun <T : Boolean> Vect0r<T>.toBooleanArray(): BooleanArray = BooleanArray(size) { i -> get(i) }

/**cloning and reifying Vect0r to LongArray*/
fun <T : Long> Vect0r<T>.toLongArray(): LongArray = LongArray(size) { i -> get(i) }

/**cloning and reifying Vect0r to FloatArray*/
fun <T : Float> Vect0r<T>.toFloatArray(): FloatArray = FloatArray(size) { i -> get(i) }

/**cloning and reifying Vect0r to DoubleArray*/
fun <T : Double> Vect0r<T>.toDoubleArray(): DoubleArray = DoubleArray(size) { i -> get(i) }

@JvmName("AToVec")
fun <T> Array<T>.toVect0r(): Vect0r<T> = (size t2 ::get) as Vect0r<T>

@JvmName("LToVec")
fun <T> List<T>.toVect0r(): Vect0r<T> = (size t2 ::get) as Vect0r<T>

@JvmName("IAToVec")
fun IntArray.toVect0r(): Vect0r<Int> = (size t2 ::get) as Vect0r<Int>

@JvmName("LAToVec")
fun LongArray.toVect0r(): Vect0r<Long> = (size t2 ::get) as Vect0r<Long>

@JvmName("DAToVec")
fun DoubleArray.toVect0r(): Vect0r<Double> = (size t2 ::get) as Vect0r<Double>

@JvmName("FAToVec")
fun FloatArray.toVect0r(): Vect0r<Float> = (size t2 ::get) as Vect0r<Float>

@JvmName("BAToVec")
fun ByteArray.toVect0r(): Vect0r<Byte> = (size t2 ::get) as Vect0r<Byte>

@JvmName("CAToVec")
fun CharArray.toVect0r(): Vect0r<Char> = (size t2 ::get) as Vect0r<Char>

@JvmName("CSToVec")
fun CharSequence.toVect0r(): Vect0r<Char> = (length t2 ::get) as Vect0r<Char>

@JvmName("StrToVec")
fun String.toVect0r(): Vect0r<String> = (length t2 ::get) as Vect0r<String>
fun <T : Boolean> BitSet.toVect0r(): Pai2<Int, (Int) -> Boolean> = (length() t2 { it: Int -> get(it) })


fun Vect0r<Int>.sum(): Int = takeIf { this.size > 0 }?.`➤`?.reduce(Int::plus) ?: 0
fun Vect0r<Long>.sum(): Long = takeIf { this.size > 0 }?.`➤`?.reduce(Long::plus) ?: 0L
fun Vect0r<Double>.sum(): Double = takeIf { this.size > 0 }?.`➤`?.reduce(Double::plus) ?: 0.0
fun Vect0r<Float>.sum(): Float = takeIf { this.size > 0 }?.`➤`?.reduce(Float::plus) ?: 0f

@JvmName("FlowToVec")
suspend fun <T> Flow<T>.toVect0r(): Vect0r<T> = this.toList().toVect0r()

@JvmName("BBToVec")
fun ByteBuffer.toVect0r(): Vect0r<Byte> =
        slice().let { slice -> Vect0r(slice.remaining()) { ix: Int -> slice.get(ix) } }


@JvmName("IntRangeToVec")
fun IntRange.toVect0r(): Vect0r<Int> {
    fun SimpleIntRangeToVec(): Vect0r<Int> = last.inc() - first t2 { x -> first + x }
    return if (this.step != 1 || this.first >= this.last) this.toList() α { it } else SimpleIntRangeToVec()
}

@JvmName("IterableToVec")
fun <T> Iterable<T>.toVect0r(): Vect0r<T> = this.toList() α { it }

@JvmName("SeqToVec")
fun <T> Sequence<T>.toVect0r(): Vect0r<T> = this.toList() α { it }

val <X> Vect0r<Vect0r<X>>.T: Pai2<Int, (Int) -> Pai2<Int, (Int) -> X>>
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
 * wrapper for for-loops and iterable filters
 */
val <T> Vect0r<T>.`➤`: IterableVect0r<T>
    get() = IterableVect0r(this)

/**
 * provides unbounded access to first and last rows beyond the existing bounds of 0 until size
 */
val <T> Vect0r<T>.infinite: Vect0r<T>
    get() = Int.MAX_VALUE t2 { x: Int ->
        this.second(when {
            x < 0 -> 0
            size <= x -> size.dec()
            else -> x
        })
    }

/**
 * index by enum
 */
@JvmName("getIndexByEnum")
operator fun <S, E : Enum<E>> Vect0r<S>.get(e: E): S = get(e.ordinal)

/**
 * optimize for where a smallish map has hotspots that are over-used and others that are excess overhead
 * in the more expensive things that old code does with maps
 */
@JvmName("sparseVect0rMap")
fun <K : Int, V> Map<K, V>.sparseVect0rMap() = let { top ->
    ((this as? SortedMap)?.keys ?: keys.sorted()).toIntArray().let { k ->
        0 t2 if (top.size <= 16) { x: Int ->
            var r: V? = null
            var i = 0
            do {
                if (k[i++] == x) r = top[x]
            } while (i < size && r == null)
            r.also {
                assert(it == top[x])
            }
        } else { x: Int ->
            k.binarySearch(x).takeUnless { 0 < it }?.let {
                top[x].debug {
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
fun <K : Int, V> Map<K, V>.sparseVect0r(): Vect0r<V?> = let { top ->
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
fun <F, S> left(v2: Vect02<F, S>) {
    v2.left
}

object Vect02_ {
    val <F, S> Vect02<F, S>.left: Pai2<Int, (Int) -> F> get() = this α Pai2<F, S>::first
    val <F, S> Vect02<F, S>.right: Pai2<Int, (Int) -> S> get() = this α Pai2<F, S>::second
    val <F, S> Vect02<F, S>.reify: Pai2<Int, (Int) -> Pair<F, S>> get() = this α Pai2<F, S>::pair

    fun <F, S> Vect02<F, S>.toMap(theMap: MutableMap<F, S>? = null): MutableMap<F, S> =
            toList().map(Pai2<F, S>::pair).let { paris ->
                theMap?.let { paris.toMap(theMap) } ?: paris.toMap(HashMap(paris.size))
            }
}

object V3ct0r_ {
    val <F, S, T> V3ct0r<F, S, T>.left: Pai2<Int, (Int) -> F> get() = this α Tripl3<F, *, *>::first
    val <F, S, T> V3ct0r<F, S, T>.mid: Pai2<Int, (Int) -> S> get() = this α Tripl3<F, S, T>::second
    val <F, S, T> V3ct0r<F, S, T>.right: Pai2<Int, (Int) -> T> get() = this α Tripl3<F, S, T>::third
    val <F, S, T> V3ct0r<F, S, T>.x: Pai2<Int, (Int) -> F> get() = this α Tripl3<F, S, T>::first
    val <F, S, T> V3ct0r<F, S, T>.y: Pai2<Int, (Int) -> S> get() = this α Tripl3<F, S, T>::second
    val <F, S, T> V3ct0r<F, S, T>.z: Pai2<Int, (Int) -> T> get() = this α Tripl3<F, S, T>::third
    fun <F, S, T> V3ct0r<F, S, T>.reify(): Pai2<Int, (Int) -> Triple<F, S, T>> = this α Tripl3<F, S, T>::triple
}

operator fun <T> Vect0r<T>.div(d: Int): Vect0r<Vect0r<T>> = (0 until size) / d α { this[it] }
