/**
 *
In mathematics, a tuple is a finite ordered list (sequence) of elements. An n-tuple is a sequence (or ordered list) of n elements, where n is a non-negative integer. There is only one 0-tuple, an empty sequence, or empty tuple, as it is referred to. An n-tuple is defined inductively using the construction of an ordered pair.

Mathematicians usually write 'tuples' by listing the elements within parentheses "{\displaystyle ({\text{ }})}(\text{ })" and separated by commas; for example, {\displaystyle (2,7,4,1,7)}(2, 7, 4, 1, 7) denotes a 5-tuple. Sometimes other symbols are used to surround the elements, such as square brackets "[ ]" or angle brackets "⟨ ⟩". Braces "{ }" are only used in defining arrays in some programming languages such as C++ and Java, but not in mathematical expressions, as they are the standard notation for sets. The term tuple can often occur when discussing other mathematical objects, such as vectors.

In computer science, tuples come in many forms. In dynamically typed languages, such as Lisp, lists are commonly used as tuples.[citation needed] Most typed functional programming languages implement tuples directly as product types,[1] tightly associated with algebraic data types, pattern matching, and destructuring assignment.[2] Many programming languages offer an alternative to tuples, known as record types, featuring unordered elements accessed by label.[3] A few programming languages combine ordered tuple product types and unordered record types into a single construct, as in C structs and Haskell records. Relational databases may formally identify their rows (records) as tuples.

Tuples also occur in relational algebra; when programming the semantic web with the Resource Description Framework (RDF); in linguistics;[4] and in philosophy.[5]
 */

package columnar

/**
a singular reference.  front end of Pai2 and Tripl3.  */
interface Hand1l<F> {
    val first: F
    operator fun component1(): F = first
    operator fun invoke(function: (RowVec) -> Unit) = this

    companion object {
        operator fun <F> invoke(first: F) = object : Hand1l<F> {
            override val first get() = first
        }
    }
}

/**inheritable version of pair that provides it's first compnent as a Un1t*/
interface Pai2<F, S>/* : Hand1l<F>*/ {
    val first: F
    val second: S
    /**
     * for println and serializable usecases, offload that stuff using this method.
     */
    val pair get() = let { first to second }

    operator fun component2(): S = second
    operator fun component1(): F = first

    companion object {
        operator fun <F, S> invoke(first: F, second: S): Pai2<F, S> =
            object : Pai2<F, S>/*, Hand1l<F> by Hand1l.invoke(f)*/ {
                override val first get() = first
                override val second get() = second
            }

        operator fun <F, S, P : kotlin.Pair<F, S>, R : Pai2<F, S>> invoke(p: P) = p.let { (f, s) ->
            Pai2(f, s)
        }
    }
}

/**inheritable version of triple that also provides its first two as a pair.
 */
interface Tripl3<F, S, T>/* : Pai2<F, S> */ {
    val first: F
    val second: S
    val third: T
    operator fun component1(): F = first

    operator fun component2(): S = second
    operator fun component3(): T = third
    /**
     * for println and serializable usecases, offload that stuff using this method.
     */
    val triple get() = let { Triple(first, second, third) }

    companion object {
        operator fun <F, S, T> invoke(first: F, second: S, third: T): Tripl3<F, S, T> =
            object : Tripl3<F, S, T>/*, Pai2<F, S> by Pai2(f, s)*/ {
                override val first get() = first
                override val second get() = second
                override val third get() = third
            }

        operator fun <F, S, T> invoke(p: kotlin.Triple<F, S, T>) = p.let { (f, s, t) -> Tripl3(f, s, t) }
    }

}

/**
 * short for tuple1
 */
val <F> F.t1 get() = columnar.Hand1l(this)

/**
 * means  either/both  of "tuple2" and  disambiguation of  pair =x "to" y
 */

/**
 * therblig methodology
 */
typealias XY<X, Y> = Pai2<X, Y>

typealias XYZ<X, Y, Z> = Tripl3<X, Y, Z>
/**
 * homage to eclipse types
 */
typealias Tw1n<X> = XY<X, X>

infix fun <X, Y, Z, P : Pai2<X, Y>, U : Hand1l<X>, T : Hand1l<Y>> U.asLeft(u: T): P = (first t2 u.first) as P
infix fun <X, Y, Z, P : Pai2<Y, Z>, U : Hand1l<X>, T : Tripl3<X, Y, Z>> U.asLeft(p: P) =
    Tripl3(first, p.first, p.second)

infix fun <X, Y, Z, U : Hand1l<X>, T : Tripl3<X, Y, Z>> U.asLeft(t: T) = Qu4d(first, t.first, t.second, t.third)
infix fun <F, S> F.t2(s: S) = Pai2(this, s)
infix fun <F, S, T> Pai2<F, S>.t3(t: T) = let { (f: F, s) -> Tripl3(f, s, t) }

infix fun <F, S, T, P : kotlin.Pair<F, S>> P.t3(t: T) = let { (a, b) -> Tripl3(a, b, t) }
infix fun <A, B, C, D> Tripl3<A, B, C>.t4(d: D) = let { (a: A, b: B, c: C) -> Qu4d(a, b, c, d) }


/**inheritable version of quad that also provides its first three as a triple. */
interface Qu4d<F, S, T, Z>/* : Tripl3<F, S, T>*/ {
    val first: F
    val second: S
    val third: T
    val fourth: Z

    operator fun component1(): F = first
    operator fun component2(): S = second
    operator fun component3(): T = third
    operator fun component4(): Z = fourth

    data class Quad<F, S, T, Z>(val x: F, val y: S, val z: T, val w: Z)

    /**
     * for println and serializable usecases, offload that stuff using this method.
     */
    val quad
        get() = let { (a, b, c, d) ->
            Quad(a, b, c, d)
        }

    companion object {
        operator fun <F, S, T, Z> invoke(first: F, second: S, third: T, fourth: Z): Qu4d<F, S, T, Z> =
            object : Qu4d<F, S, T, Z>/*, Tripl3<F, S, T> by Tripl3(f, s, t)*/ {
                override val first: F get() = first
                override val second: S get() = second
                override val third: T get() = third
                override val fourth: Z get() = fourth
            }

        operator fun <F, S, T, Z> invoke(p: Quad<F, S, T, Z>) = p.let { (f, s, t, z) ->
            Qu4d(f, s, t, z)
        }

        operator fun <F, S, T, Z> invoke(p: Array<*>) = p.let { (f, s, t, z) ->
            @Suppress("UNCHECKED_CAST")
            Qu4d(f as F, s as S, t as T, z as Z)
        }

        operator fun <F, S, T, Z> invoke(p: List<*>) = p.let { (f, s, t, z) ->
            @Suppress("UNCHECKED_CAST")
            Qu4d(f as F, s as S, t as T, z as Z)
        }
    }
}
