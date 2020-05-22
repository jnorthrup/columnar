/**
 *
In mathematics, a tuple is a finite ordered list (sequence) of elements. An n-tuple is a sequence (or ordered list) of n elements, where n is a non-negative integer. There is only one 0-tuple, an empty sequence, or empty tuple, as it is referred to. An n-tuple is defined inductively using the construction of an ordered pair.

Mathematicians usually write 'tuples' by listing the elements within parentheses "{\displaystyle ({\text{ }})}(\text{ })" and separated by commas; for example, {\displaystyle (2,7,4,1,7)}(2, 7, 4, 1, 7) denotes a 5-tuple. Sometimes other symbols are used to surround the elements, such as square brackets "[ ]" or angle brackets "⟨ ⟩". Braces "{ }" are only used in defining arrays in some programming languages such as C++ and Java, but not in mathematical expressions, as they are the standard notation for sets. The term tuple can often occur when discussing other mathematical objects, such as vectors.

In computer science, tuples come in many forms. In dynamically typed languages, such as Lisp, lists are commonly used as tuples.[citation needed] Most typed functional programming languages implement tuples directly as product types,[1] tightly associated with algebraic data types, pattern matching, and destructuring assignment.[2] Many programming languages offer an alternative to tuples, known as record types, featuring unordered elements accessed by label.[3] A few programming languages combine ordered tuple product types and unordered record types into a single construct, as in C structs and Haskell records. Relational databases may formally identify their rows (records) as tuples.

Tuples also occur in relational algebra; when programming the semantic web with the Resource Description Framework (RDF); in linguistics;[4] and in philosophy.[5]
 */


@file:Suppress("OVERRIDE_BY_INLINE")

package vec.macros

/**
 * 1-ary tuple interface, a handle.  was conceived as something that provided "first" by inheritance,
 * so it's mostly vestigial dead code for now.
 */
interface Hand1l<out F> {
    val first: F
    operator fun component1(): F = first
    operator fun invoke() = first

    companion object {
        operator fun <F> invoke(first: F) = object : Hand1l<F> {
            override val first get() = first
        }
    }
}

/**inheritable version of pair  */
interface Pai2<F, S> {
    val first: F
    val second: S
    operator fun component1(): F = first
    operator fun component2(): S = second

    /**
     * for println and serializable usecases, offload that stuff using this method.
     */
    val pair get() = let { first to second }

    @Suppress("OVERRIDE_BY_INLINE")
    companion object {
        inline operator fun <reified F, reified S> invoke(first: F, second: S): Pai2<F, S> =
                object : Pai2<F, S> {
                    override inline val first get() = first
                    override inline val second get() = second
                }

        /**
         * Pair copy ctor conversion
         */
        inline operator fun <reified F, reified S, reified P : Pair<F, S>, reified R : Pai2<F, S>> invoke(p: P): R =
                p.run { (first t2 second) as R }
    }
}
/*
inline operator fun <reified F, reified S> Pai2<F, S>.component2(): S = second
inline operator fun <reified F, reified S> Pai2<F, S>.component1(): F = first*/

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
        inline operator fun <reified F, reified S, reified T> invoke(first: F, second: S, third: T): Tripl3<F, S, T> =
                object : Tripl3<F, S, T>/*, Pai2<F, S> by Pai2(f, s)*/ {
                    override inline val first get() = first
                    override inline val second get() = second
                    override inline val third get() = third
                }

        inline operator fun <reified F, reified S, reified T> invoke(p: Triple<F, S, T>) =
                p.let { (f, s, t) -> Tripl3(f, s, t) }
    }

}

inline operator fun <reified F, reified S, reified T> Tripl3<F, S, T>.component3(): T = third
inline operator fun <reified F, reified S, reified T> Tripl3<F, S, T>.component2(): S = second
inline operator fun <reified F, reified S, reified T> Tripl3<F, S, T>.component1(): F = first


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
typealias Tw1n<reified X> = XY<X, X>

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
inline class Tw1nt(val ia: IntArray) : Tw1n<Int> {
    override inline val first get() = ia[0]
    override inline val second get() = ia[1]

}

/**
 * long-type specific twin with primitive array backing store.
 */
inline class Twln(val ia: LongArray) : Tw1n<Long> {
    override inline val first get() = ia[0]
    override inline val second get() = ia[1]
}

@JvmName("twinint")
inline fun <reified T : Int> Tw1n(first: T, second: T) = Tw1nt(intArrayOf(first, second))

@JvmName("twinlong")
inline fun <reified T : Long> Tw1n(first: T, second: T) = Twln(longArrayOf(first, second))




inline infix fun <reified X, reified Y, Z, P : Pai2<X, Y>, U : Hand1l<X>, T : Hand1l<Y>> U.asLeft(u: T) =
        (first t2 u.first) as P

inline infix fun <reified X, reified Y, reified Z, P : Pai2<Y, Z>, U : Hand1l<X>, T : Tripl3<X, Y, Z>> U.asLeft(
        p: P,
) = Tripl3(first, p.first, p.second)

inline infix fun <reified X, reified Y, reified Z, U : Hand1l<X>, T : Tripl3<X, Y, Z>> U.asLeft(t: T) =
        Qu4d(first, t.first, t.second, t.third)

inline infix fun <reified F, reified S> F.t2(s: S) = Pai2(this, s)
inline infix fun <reified F, reified S, reified T> Pai2<F, S>.t3(t: T) = let { (f: F, s) ->
    Tripl3(
            f,
            s,
            t
    )
}

inline infix fun <reified F, reified S, reified T, P : Pair<F, S>> P.t3(t: T) =
        let { (a, b) -> Tripl3(a, b, t) }

inline infix fun <reified A, reified B, reified C, reified D> Tripl3<A, B, C>.t4(d: D) =
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
        inline operator fun <reified F, reified S, reified T, reified Z> invoke(
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

        inline operator fun <reified F, reified S, reified T, reified Z> invoke(p: Quad<F, S, T, Z>) =
                p.let { (f, s, t, z) ->
                    Qu4d(f, s, t, z)
                }

        inline operator fun <reified F, reified S, reified T, reified Z> invoke(p: Array<*>) = p.let { (f, s, t, z) ->
            @Suppress("UNCHECKED_CAST")
            (Qu4d(f as F, s as S, t as T, z as Z))
        }

        inline operator fun <reified F, reified S, reified T, reified Z> invoke(p: List<*>) = p.let { (f, s, t, z) ->
            @Suppress("UNCHECKED_CAST")
            (Qu4d(f as F, s as S, t as T, z as Z))
        }
    }
}

inline operator fun <reified F, reified S, reified T, reified Z> Qu4d<F, S, T, Z>.component4() = fourth
inline operator fun <reified F, reified S, reified T, reified Z> Qu4d<F, S, T, Z>.component3() = third
inline operator fun <reified F, reified S, reified T, reified Z> Qu4d<F, S, T, Z>.component2() = second
inline operator fun <reified F, reified S, reified T, reified Z> Qu4d<F, S, T, Z>.component1() = first
