package columnar

import kotlinx.coroutines.flow.*
import kotlin.experimental.ExperimentalTypeInference

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

operator fun <T> Matrix<T>.get(vararg c: Int): T = second(c)

@UseExperimental(ExperimentalTypeInference::class)
@BuilderInference
infix fun <O, R, F : (O) -> R> O.`→`(f: F) = this.let(f)

@BuilderInference
@UseExperimental(ExperimentalTypeInference::class)
operator fun <A, B, R, O : (A) -> B, G : (B) -> R> O.times(b: G) = { a: A -> a `→` this `→` (b) }

@BuilderInference
@UseExperimental(ExperimentalTypeInference::class)
infix fun <A, B, R, O : (A) -> B, G : (B) -> R> O.`→`(b: G) = this * b

/**
 * G follows F
 */
@UseExperimental(ExperimentalTypeInference::class)
@BuilderInference
infix fun <A, B, C, G : (B) -> C, F : (A) -> B> G.`⚬`(f: F) = { a: A -> a `→` f `→` this }

/**
 * (λx.M[x]) → (λy.M[y])	α-conversion
 * https://en.wikipedia.org/wiki/Lambda_calculus
 * */
@UseExperimental(ExperimentalTypeInference::class)
@BuilderInference
infix fun <A, C, B : (A) -> C, V : Vect0r<A>, R : Vect0r<C>> V.α(m: B): Vect0r<C> = map(m)


@BuilderInference
@UseExperimental(ExperimentalTypeInference::class)
infix fun <A, C, B : (A) -> C, T : Iterable<A>> T.α(m: B) = this.map { it `→` m }

@BuilderInference
@UseExperimental(ExperimentalTypeInference::class)
infix fun <A, C, B : (A) -> C, T : Sequence<A>> T.α(m: B) = this.map { it `→` m }

@BuilderInference
@UseExperimental(ExperimentalTypeInference::class)
infix fun <A, C, B : (A) -> C, T : Flow<A>> T.α(m: B) = this.map { it `→` m }

@BuilderInference
@UseExperimental(ExperimentalTypeInference::class)
infix fun <A, C, B : (A) -> C> List<A>.α(m: B): Vect0r<C> = Vect0r(this.size) { i: Int -> this[i] `→` m }

@BuilderInference
@UseExperimental(ExperimentalTypeInference::class)
infix fun <A, C, B : (A) -> C> Array<A>.α(m: B): Vect0r<C> = Vect0r(this.size) { i: Int -> this[i] `→` m }

@BuilderInference
@UseExperimental(ExperimentalTypeInference::class)
infix fun <C, B : (Int) -> C> IntArray.α(m: B): Vect0r<C> = Vect0r(this.size) { i: Int -> this[i] `→` m }

@BuilderInference
@UseExperimental(ExperimentalTypeInference::class)
infix fun <C, B : (Float) -> C> FloatArray.α(m: B): Vect0r<C> = Vect0r(this.size) { i: Int -> this[i] `→` m }

@BuilderInference
@UseExperimental(ExperimentalTypeInference::class)
infix fun <C, B : (Double) -> C> DoubleArray.α(m: B): Vect0r<C> = Vect0r(this.size) { i: Int -> this[i] `→` m }

@BuilderInference
@UseExperimental(ExperimentalTypeInference::class)
infix fun <C, B : (Long) -> C> LongArray.α(m: B): Vect0r<C> = Vect0r(this.size) { i: Int -> this[i] `→` m }

/*
But as soon as a groupoid has both a left and a right identity, they are necessarily unique and equal. For if e is
a left identity and f is a right identity, then f=ef=e.
*/
/** left identity */
@UseExperimental(ExperimentalTypeInference::class)
object `⟳` {
    @BuilderInference
    operator fun <T> invoke(t: T) = { t: T -> t }
}

/**right identity*/
@BuilderInference
@UseExperimental(ExperimentalTypeInference::class)
val <T> T.`⟲`
    get() = { this }

/**right identity*/
@BuilderInference
@UseExperimental(ExperimentalTypeInference::class)
infix fun <T, R> T.`⟲`(f: (T) -> R) = run { f(this) }

@JvmName("vlike_Sequence_1")
@BuilderInference
@UseExperimental(ExperimentalTypeInference::class)
operator fun <T> Sequence<T>.get(vararg index: Int) = get(index)

@JvmName("vlike_Sequence_Iterable2")
@BuilderInference
@UseExperimental(ExperimentalTypeInference::class)
operator fun <T> Sequence<T>.get(indexes: Iterable<Int>) = this[indexes.toList().toIntArray()]

@JvmName("vlike_Sequence_IntArray3")
@BuilderInference
@UseExperimental(ExperimentalTypeInference::class)
operator fun <T> Sequence<T>.get(index: IntArray) = this.toList()[index].asSequence()

@JvmName("vlike_Flow_1")
@BuilderInference
@UseExperimental(ExperimentalTypeInference::class)
suspend fun <T> Flow<T>.get(vararg index: Int) = get(index)

@Suppress("USELESS_CAST")
@JvmName("vlike_Flow_Iterable2")
@BuilderInference
@UseExperimental(ExperimentalTypeInference::class)
suspend fun <T> Flow<T>.get(indexes: Iterable<Int>) = this.get(indexes.toList().toIntArray() as IntArray)

@JvmName("vlike_Flow_IntArray3")
@BuilderInference
@UseExperimental(ExperimentalTypeInference::class)
suspend fun <T> Flow<T>.get(index: IntArray) = this.toList()[index].asFlow()

@JvmName("vlike_List_1")
@BuilderInference
@UseExperimental(ExperimentalTypeInference::class)
operator fun <T> List<T>.get(vararg index: Int) = get(index)

@JvmName("vlike_List_Iterable2")
@BuilderInference
@UseExperimental(ExperimentalTypeInference::class)
operator fun <T> List<T>.get(indexes: Iterable<Int>) = this[indexes.toList().toIntArray()]

@JvmName("vlike_List_IntArray3")
@BuilderInference
@UseExperimental(ExperimentalTypeInference::class)
operator fun <T> List<T>.get(index: IntArray) = List(index.size) { i: Int -> this[index[i]] }

@JvmName("vlike_Array_1")
@BuilderInference
@UseExperimental(ExperimentalTypeInference::class)
inline operator fun <reified T> Array<T>.get(vararg index: Int) = get(index)

@JvmName("vlike_Array_Iterable2")
@BuilderInference
@UseExperimental(ExperimentalTypeInference::class)
inline operator fun <reified T> Array<T>.get(indexes: Iterable<Int>) = this[indexes.toList().toIntArray()]

@JvmName("vlike_Array_IntArray3")
@BuilderInference
@UseExperimental(ExperimentalTypeInference::class)
inline operator fun <reified T> Array<T>.get(index: IntArray) = Array(index.size) { i: Int -> this[index[i]] }
/*

@JvmName("vlike_IntArray_1i")
operator fun IntArray.get(vararg index: Int) = this.get(index)

@JvmName("vlike_IntArray_Iterable2")
operator fun IntArray.get(indexes: Iterable<Int>) = this.get(indexes.toList().toIntArray())

@JvmName("vlike_IntArray_IntIntArray3")
operator fun IntArray.get(index: IntArray) = IntArray(index.size) { i: Int -> this[index[i]] }
*/

@JvmName("vlike_Vect0r_get")
operator fun <T> Vect0r<T>.get(index: Int): T = second(index)

@JvmName("vlike_Vect0r_1")
operator fun <T> Vect0r<T>.get(vararg index: Int): Vect0r<T> = get(index)

@JvmName("vlike_Vect0r_Iterable2")
operator fun <T> Vect0r<T>.get(indexes: Iterable<Int>): Vect0r<T> = this[indexes.toList().toIntArray()]

@JvmName("vlike_Vect0r_IntArray3")
operator fun <T> Vect0r<T>.get(index: IntArray): Vect0r<T> = Vect0r(index.size) { ix: Int -> second(index[ix]) }

inline fun <reified T> Vect0r<T>.toArray() = this.let { (_, vf) -> Array(first) { vf(it) } }
fun <T> Vect0r<T>.toList(): List<T> = let { v ->
    object : AbstractList<T>() {
        override val size: Int = v.first
        override operator fun get(index: Int) = v.second(index)
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

fun <T, R, V : Vect0r<T>> V.map(fn: (T) -> R): Vect0r<R> = Vect0r(first) { it `→` (fn `⚬` second) }
fun <T, R> Vect0r<T>.mapIndexed(fn: (Int, T) -> R): Vect0r<R> = Vect0r(first) { fn(it, it `→` second) }
fun <T, R> Vect0r<T>.mapIndexedToList(fn: (Int, T) -> R) = List(first) { fn(it, it `→` second) }
fun <T> Vect0r<T>.forEach(fn: (T) -> Unit) {
    for (ix in (0 until first)) ix `→` (fn `⚬` second)
}


fun <T> Vect0r<T>.forEachIndexed(fn: (Int, T) -> Unit) {
    for (ix in (0 until first)) fn(ix, ix `→` second)
}

fun <T> vect0rOf(vararg a: T): Vect0r<T> = Vect0r(a.size) { a[it] }

/**
 * Returns a list of pairs built from the elements of `this` array and the [other] array with the same index.
 * The returned list has length of the shortest collection.
 *
 * @sample samples.collections.Iterables.Operations.zipIterable
 */
inline infix fun <T, reified R> List<T>.zip(other: Vect0r<R>): List<Pai2<T, R>> =
    zip(other.toList()) { a, b -> a t2 b }

@Suppress("UNCHECKED_CAST")
fun <T, O> Vect0r<T>.zip(o: Vect0r<O>): Vect02<T, O> =
    Vect0r(this.first) { i: Int -> (this[i] t2 o[i]) }

@BuilderInference
@UseExperimental(ExperimentalTypeInference::class)
fun <T> Array<T>.toVect0r(): Vect0r<T> = Vect0r(size) { ix: Int -> this[ix] }

@BuilderInference
@UseExperimental(ExperimentalTypeInference::class)
fun <T> List<T>.toVect0r(): Vect0r<T> = Vect0r(size) { ix: Int -> this[ix] }

@BuilderInference
@UseExperimental(ExperimentalTypeInference::class)
suspend fun <T> Flow<T>.toVect0r(): Vect0r<T> = this.toList().toVect0r()

@BuilderInference
@UseExperimental(ExperimentalTypeInference::class)
fun <T> Iterable<T>.toVect0r(): Vect0r<T> = this.toList().toVect0r()

@BuilderInference
@UseExperimental(ExperimentalTypeInference::class)
fun <T> Sequence<T>.toVect0r(): Vect0r<T> = this.toList().toVect0r()

@JvmName("combine_Flow")
@BuilderInference
@UseExperimental(ExperimentalTypeInference::class)
fun <T> combine(@BuilderInference vararg s: Flow<T>) = flow {
    @BuilderInference
    for (f in s) {
        f.collect {
            emit(it)
        }
    }
}

@JvmName("combine_Sequence")
@BuilderInference
@UseExperimental(ExperimentalTypeInference::class)
fun <T> combine(vararg s: Sequence<T>) = sequence {
    @BuilderInference
    for (sequence in s) {
        for (t in sequence) {
            yield(t)
        }
    }
}

@JvmName("combine_List")
fun <T> combine(vararg a: List<T>) =
    a.sumBy { it.size }.let { size ->
        var x = 0
        var y = 0
        List(size) { i ->
            if (y >= a[x].size) {
                ++x
                y = 0
            }
            a[x][y++]
        }
    }

@JvmName("combine_Array")
inline fun <reified T> combine(vararg a: Array<T>) = a.sumBy { it.size }.let { size ->
    var x = 0
    var y = 0; Array(size) { i ->
    if (y >= a[x].size) {
        ++x; y = 0
    }
    a[x][y++]
}
}

fun IntArray.zipWithNext(): Vect02<Int, Int> = Vect0r(
    size / 2
) { i: Int ->
    val c = i * 2
    Tw1n(this[c], this[c + 1])
}

fun Vect0r<Int>.zipWithNext(): Vect02<Int, Int> = Vect0r(
    size / 2
) { i: Int ->
    val c = i * 2
    Tw1n(this[c], this[c + 1])
}


//array-like mapped map
@BuilderInference
@UseExperimental(ExperimentalTypeInference::class)
inline operator fun <reified K, reified V> Map<K, V>.get(@BuilderInference ks: Vect0r<K>) =
    this.get(*ks.toList().toTypedArray())

@BuilderInference
@UseExperimental(ExperimentalTypeInference::class)
inline operator fun <reified K, reified V> Map<K, V>.get(@BuilderInference ks: Iterable<K>) =
    this.get(*ks.toList().toTypedArray())

@BuilderInference
@UseExperimental(ExperimentalTypeInference::class)
inline operator fun <K, reified V> Map<K, V>.get(@BuilderInference vararg ks: K) =
    Array(ks.size) { ix -> ks[ix].let(this::get)!! }

@BuilderInference
@UseExperimental(ExperimentalTypeInference::class)
infix operator fun IntRange.div(@BuilderInference denominator: Int): Vect0r<IntRange> =
    (this t2 (last - first + (1 - first)) / denominator).let { (_: IntRange, subSize: Int): Pai2<IntRange, Int> ->
        Vect0r((denominator)) { x: Int ->
            (subSize * x).let { lower ->
                lower..last.coerceAtMost(lower + subSize - 1)
            }
        }
    }

fun <T> Vect0r<T>.last() = get(size - 1)
