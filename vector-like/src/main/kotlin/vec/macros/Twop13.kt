/**
 *
In mathematics, a tuple is a finite ordered list (sequence) of elements. An n-tuple is a sequence (or ordered list) of n elements, where n is a non-negative integer. There is only one 0-tuple, an empty sequence, or empty tuple, as it is referred to. An n-tuple is defined inductively using the construction of an ordered pair.

Mathematicians usually write 'tuples' by listing the elements within parentheses "{\displaystyle ({\text{ }})}(\text{ })" and separated by commas; for example, {\displaystyle (2,7,4,1,7)}(2, 7, 4, 1, 7) denotes a 5-tuple. Sometimes other symbols are used to surround the elements, such as square brackets "[ ]" or angle brackets "⟨ ⟩". Braces "{ }" are only used in defining arrays in some programming languages such as C++ and Java, but not in mathematical expressions, as they are the standard notation for sets. The term tuple can often occur when discussing other mathematical objects, such as vectors.

In computer science, tuples come in many forms. In dynamically typed languages, such as Lisp, lists are commonly used as tuples.[citation needed] Most typed functional programming languages implement tuples directly as product types,[1] tightly associated with algebraic data types, pattern matching, and destructuring assignment.[2] Many programming languages offer an alternative to tuples, known as record types, featuring unordered elements accessed by label.[3] A few programming languages combine ordered tuple product types and unordered record types into a single construct, as in C structs and Haskell records. Relational databases may formally identify their rows (records) as tuples.

Tuples also occur in relational algebra; when programming the semantic web with the Resource Description Framework (RDF); in linguistics;[4] and in philosophy.[5]
 */


@file:Suppress("OVERRIDE_BY_", "OVERRIDE_BY_INLINE", "NOTHING_TO_INLINE", "FunctionName")

package vec.macros

import vec.util._a

/**inheritable version of pair  */
interface Pai2<F, S> {
    val first: F
    val second: S

    /**
     * for println and serializable usecases, offload that stuff using this method.
     */
    val pair: Pair<F, S> get() = let { first to second }

    companion object {
        inline operator fun <reified F, reified S> invoke(first: F, second: S): Pai2<F, S> =
            object : Pai2<F, S> {
                override inline val first get() = first
                override inline val second get() = second
            }


        /**
         * Pair copy ctor conversion
         */

        inline operator fun <reified F, reified S, reified P : Pair<F, S>, reified R : Pai2<F, S>> invoke(p: P) =
            object : Pai2<F, S> {
                override val first: F by p::first
                override val second: S by p::second
            }

        inline operator fun <F, S, P : Map.Entry<F, S>, R : Pai2<F, S>> invoke(p: P) = object : Pai2<F, S> {
            override val first: F by p::key
            override val second: S by p::value
        }
    }
}

inline operator fun <reified F, reified S> Pai2<F, S>.component2(): S = second
inline operator fun <reified F, reified S> Pai2<F, S>.component1(): F = first


/**inheritable version of triple
 */
interface Tripl3<out F, out S, out T>/* : Pai2<F, S> */ {
    val first: F
    val second: S
    val third: T

    /**
     * for println and serializable usecases, offload that stuff using this method.
     */
    val triple get() = Triple(first, second, third)

    companion object {

        inline operator fun <F, S, T> invoke(first: F, second: S, third: T): Tripl3<F, S, T> =
            object : Tripl3<F, S, T>/*, Pai2<F, S> by Pai2(f, s)*/ {
                override inline val first get() = first
                override inline val second get() = second
                override inline val third get() = third
            }


        inline operator fun <F, S, T> invoke(p: Triple<F, S, T>) =
            p.let { (f, s, t) -> Tripl3(f, s, t) }
    }

}

inline operator fun <F, S, T> Tripl3<F, S, T>.component3(): T = third
inline operator fun <F, S, T> Tripl3<F, S, T>.component2(): S = second
inline operator fun <F, S, T> Tripl3<F, S, T>.component1(): F = first


/**
 * means  either/both  of "tuple2" and  disambiguation of  pair =x "to" y
 */

/**
 * therblig methodology
 */
typealias XY<X, Y> = Pai2<X, Y>

typealias XYZ<X, Y, Z> = Tripl3<X, Y, Z>
/**
 * a pair with uniform types
 *
 *
 * homage to eclipse types
 */
typealias Tw1n<X> = XY<X, X>

/**
 * a factory method
 */
inline fun <reified T> Tw1n(first: T, second: T): Tw1n<T> = arrayOf(first, second).let { ar ->
    object : Pai2<T, T> {
        override inline val first get() = ar[0]
        override inline val second get() = ar[1]
    }
}

/**
 * int-type specific twin with primitive array backing store.
 */
@JvmInline
value class Tw1nt(val ia: IntArray) : Tw1n<Int> {
    override inline val first get() = ia[0]
    override inline val second get() = ia[1]
}

/**
 * long-type specific twin with primitive array backing store.
 */
@JvmInline
value class Twln(val ia: LongArray) : Tw1n<Long> {
    override inline val first get() = ia[0]
    override inline val second get() = ia[1]
}

@JvmName("twinint")
fun <T : Int> Tw1n(first: T, second: T): Tw1nt = Tw1nt(_a[first, second])

@JvmName("twinlong")
fun <T : Long> Tw1n(first: T, second: T) = Twln(_a[first, second])

@JvmName("unaryPlusAny")
operator fun <S> Tw1n<S>.unaryMinus() = _a[first, second]

@JvmName("unaryPlusI")
inline operator fun Tw1n<Int>.unaryPlus() = (-this).let { (a, b) -> a..b }
@JvmName("aSInt")
inline infix fun <reified R> Tw1n<Int>.α(noinline f: (Int) -> R) = (-this).α(f)
@JvmName("aSDouble")
inline infix fun <reified R> Tw1n<Double>.α(noinline f: (Double) -> R) = (-this).α(f)
@JvmName("aSLong")
inline infix fun <reified R> Tw1n<Long>.α(noinline f: (Long) -> R) = (-this).α(f)
@JvmName("aSFloat")
inline infix fun <reified R> Tw1n<Float>.α(noinline f: (Float) -> R) = (-this).α(f)
@JvmName("aSByte")
inline infix fun <reified R> Tw1n<Byte>.α(noinline f: (Byte) -> R) = (-this).α(f)
@JvmName("aSChar")
inline infix fun <reified R> Tw1n<Char>.α(noinline f: (Char) -> R) = (-this).α(f)
@JvmName("aSShort")
inline infix fun <reified R> Tw1n<Short>.α(noinline f: (Short) -> R) = (-this).α(f)


//pairs of enums tricks
@JvmName("unaryPlusS")
inline operator fun <T, S : Enum<S>> Tw1n<S>.unaryPlus() = (-this).let { (a, b) -> a..b }
inline infix operator fun <T : Enum<T>> Enum<T>.rangeTo(ub: Enum<T>) = this.ordinal..ub.ordinal

@JvmName("αS")
inline infix fun <reified S, reified R> Tw1n<S>.α(noinline f: (S) -> R) = (-this).α(f)
inline infix fun <reified F, reified S> F.t2(s: S) = Pai2.invoke(this, s)
inline infix fun <reified F, reified S, T> Pai2<F, S>.t3(t: T) = let { (f: F, s) ->
    Tripl3(
        f,
        s,
        t
    )
}

infix fun <F, S, T, P : Pair<F, S>> P.t3(t: T) =
    let { (a, b) -> Tripl3(a, b, t) }

infix fun <A, B, C, D> Tripl3<A, B, C>.t4(d: D) =
    let { (a: A, b: B, c: C) -> Qu4d(a, b, c, d) }

/**inheritable version of quad that also provides its first three as a triple. */
interface Qu4d<F, S, T, Z> {
    val first: F
    val second: S
    val third: T
    val fourth: Z

    data class Quad<F, S, T, Z>(val x: F, val y: S, val z: T, val w: Z)

    /**
     * for println and serializable usecases, offload that stuff using this method.
     */
    val quad: Quad<F, S, T, Z>
        get() = Quad(
            first, second, third, fourth
        )

    companion object {

        operator fun <F, S, T, Z> invoke(
            first: F,
            second: S,
            third: T,
            fourth: Z,
        ): Qu4d<F, S, T, Z> =
            object : Qu4d<F, S, T, Z> {
                override inline val first get() = first
                override inline val second get() = second
                override inline val third get() = third
                override inline val fourth get() = fourth
            }


        operator fun <F, S, T, Z> invoke(p: Quad<F, S, T, Z>) =
            p.let { (f, s, t, z) ->
                Qu4d(f, s, t, z)
            }


        operator fun <F, S, T, Z> invoke(p: Array<*>) = p.let { (f, s, t, z) ->
            @Suppress("UNCHECKED_CAST")
            (Qu4d(f as F, s as S, t as T, z as Z))
        }


        operator fun <F, S, T, Z> invoke(p: List<*>) = p.let { (f, s, t, z) ->
            @Suppress("UNCHECKED_CAST")
            (Qu4d(f as F, s as S, t as T, z as Z))
        }
    }
}

inline operator fun <reified F, reified S, reified T, reified Z> Qu4d<F, S, T, Z>.component4() = fourth
inline operator fun <reified F, reified S, reified T, reified Z> Qu4d<F, S, T, Z>.component3() = third
inline operator fun <reified F, reified S, reified T, reified Z> Qu4d<F, S, T, Z>.component2() = second
inline operator fun <reified F, reified S, reified T, reified Z> Qu4d<F, S, T, Z>.component1() = first
