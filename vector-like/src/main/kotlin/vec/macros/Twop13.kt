/**
 *
In mathematics, a tuple is a finite ordered list (sequence) of elements. An n-tuple is a sequence (or ordered list) of n elements, where n is a non-negative integer. There is only one 0-tuple, an empty sequence, or empty tuple, as it is referred to. An n-tuple is defined inductively using the construction of an ordered pair.

Mathematicians usually write 'tuples' by listing the elements within parentheses "{\displaystyle ({\text{ }})}(\text{ })" and separated by commas; for example, {\displaystyle (2,7,4,1,7)}(2, 7, 4, 1, 7) denotes a 5-tuple. Sometimes other symbols are used to surround the elements, such as square brackets "[ ]" or angle brackets "⟨ ⟩". Braces "{ }" are only used in defining arrays in some programming languages such as C++ and Java, but not in mathematical expressions, as they are the standard notation for sets. The term tuple can often occur when discussing other mathematical objects, such as vectors.

In computer science, tuples come in many forms. In dynamically typed languages, such as Lisp, lists are commonly used as tuples.[citation needed] Most typed functional programming languages implement tuples directly as product types,[1] tightly associated with algebraic data types, pattern matching, and destructuring assignment.[2] Many programming languages offer an alternative to tuples, known as record types, featuring unordered elements accessed by label.[3] A few programming languages combine ordered tuple product types and unordered record types into a single construct, as in C structs and Haskell records. Relational databases may formally identify their rows (records) as tuples.

Tuples also occur in relational algebra; when programming the semantic web with the Resource Description Framework (RDF); in linguistics;[4] and in philosophy.[5]
 */


@file:Suppress("OVERRIDE_BY_", "OVERRIDE_BY_INLINE", "NOTHING_TO_INLINE")

package vec.macros

import org.bouncycastle.asn1.x500.style.RFC4519Style.c
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
        inline operator fun <F, S> invoke(first: F, second: S): Pai2<F, S> =
            object : Pai2<F, S> {
                override val first get() = first
                override val second get() = second
            }


        /**
         * Pair copy ctor conversion
         */

        inline operator fun <F, S, P : Pair<F, S>, R : Pai2<F, S>> invoke(p: P) =
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

inline operator fun <F, S> Pai2<F, S>.component2(): S = second
inline operator fun <F, S> Pai2<F, S>.component1(): F = first


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
                override val first get() = first
                override val second get() = second
                override val third get() = third
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
        override val first get() = ar[0]
        override val second get() = ar[1]
    }
}

/**
 * int-type specific twin with primitive array backing store.
 */
@JvmInline
value class Tw1nt(val ia: IntArray) : Tw1n<Int> {
    override val first get() = ia[0]
    override val second get() = ia[1]
}

/**
 * long-type specific twin with primitive array backing store.
 */
@JvmInline
value class Twln(val ia: LongArray) : Tw1n<Long> {
    override val first get() = ia[0]
    override val second get() = ia[1]
}

@JvmName("twinint")
fun <T : Int> Tw1n(first: T, second: T): Tw1nt = Tw1nt(_a[first, second])

@JvmName("twinlong")
fun <T : Long> Tw1n(first: T, second: T) = Twln(_a[first, second])

inline infix fun <F, S> F.t2(s: S) = Pai2<F, S>(this, s)

inline infix fun Int.t2(s: Int) = Twint2(toLong() shl 32 or s.toLong() and 0xFFFF_FFFFL)

inline class Twint2(val l: Long) : Pai2<Int, Int> {
    override val first  get() = (l.shr(32)) .toInt()
    override val second  get() = l.toInt()
}


inline infix fun <F, S, T> Pai2<F, S>.t3(t: T) = let { (f: F, s) ->
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
                override val first get() = first
                override val second get() = second
                override val third get() = third
                override val fourth get() = fourth
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

inline operator fun <F, S, T, Z> Qu4d<F, S, T, Z>.component4() = fourth
inline operator fun <F, S, T, Z> Qu4d<F, S, T, Z>.component3() = third
inline operator fun <F, S, T, Z> Qu4d<F, S, T, Z>.component2() = second
inline operator fun <F, S, T, Z> Qu4d<F, S, T, Z>.component1() = first
