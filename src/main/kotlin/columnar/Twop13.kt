/**
 *
In mathematics, a tuple is a finite ordered list (sequence) of elements. An n-tuple is a sequence (or ordered list) of n elements, where n is a non-negative integer. There is only one 0-tuple, an empty sequence, or empty tuple, as it is referred to. An n-tuple is defined inductively using the construction of an ordered pair.

Mathematicians usually write 'tuples' by listing the elements within parentheses "{\displaystyle ({\text{ }})}(\text{ })" and separated by commas; for example, {\displaystyle (2,7,4,1,7)}(2, 7, 4, 1, 7) denotes a 5-tuple. Sometimes other symbols are used to surround the elements, such as square brackets "[ ]" or angle brackets "⟨ ⟩". Braces "{ }" are only used in defining arrays in some programming languages such as C++ and Java, but not in mathematical expressions, as they are the standard notation for sets. The term tuple can often occur when discussing other mathematical objects, such as vectors.

In computer science, tuples come in many forms. In dynamically typed languages, such as Lisp, lists are commonly used as tuples.[citation needed] Most typed functional programming languages implement tuples directly as product types,[1] tightly associated with algebraic data types, pattern matching, and destructuring assignment.[2] Many programming languages offer an alternative to tuples, known as record types, featuring unordered elements accessed by label.[3] A few programming languages combine ordered tuple product types and unordered record types into a single construct, as in C structs and Haskell records. Relational databases may formally identify their rows (records) as tuples.

Tuples also occur in relational algebra; when programming the semantic web with the Resource Description Framework (RDF); in linguistics;[4] and in philosophy.[5]
*/

package columnar

/**
a singular reference.  front end of Pai2 and Tripl3.
unfortunate overlap with FP "Unit" but nothing else could be perturbed with a 1 quite as nicely.
 */
interface Un1t<F> {
    val first: F
    operator fun component1(): F = first

    companion object {
        operator fun <F> invoke(f: F) = object : Un1t<F> {
            override val first get() = f
        }
    }
}

/**inheritable version of pair that provides it's first compnent as a Un1t*/
interface Pai2<F, S> : Un1t<F> {
    val second: S
    /**
     * for println and serializable usecases, offload that stuff using this method.
     */
    val pair get() = let { (a, b) -> a to b }

    operator fun component2(): S = second

    companion object {
        operator fun <F, S> invoke(f: F, s: S): Pai2<F, S> = object : Pai2<F, S>, Un1t<F> by Un1t.invoke(f) {
            override val second get() = s
        }

        operator fun <F, S, P : kotlin.Pair<F, S>, R : Pai2<F, S>> invoke(p: P) = p.let { (f, s) ->
            Pai2(
                f,
                s
            )
        }
    }
}

/**inheritable version of triple that also provides its first two as a pair.
 */
interface Tripl3<F, S, T> : Pai2<F, S> {
    val third: T
    /**
     * for println and serializable usecases, offload that stuff using this method.
     */
    val triple get() = let { (a, b, c) -> Triple(a, b, c) }

    operator fun component3(): T = third

    companion object {
        operator fun <F, S, T> invoke(f: F, s: S, t: T): Tripl3<F, S, T> =
            object : Tripl3<F, S, T>, Pai2<F, S> by Pai2(
                f,
                s
            ) {
                override val third get() = t
            }

        operator fun <F, S, T> invoke(p: kotlin.Triple<F, S, T>) = p.let { (f, s, t) ->
            Tripl3(
                f,
                s,
                t
            )
        }
    }
}

/**inheritable version of quad that also provides its first three as a triple.
 */
interface Qu4d<F, S, T, Z> : Tripl3<F, S, T> {
    val fourth: Z

    data class Quad<F, S, T, Z>(val x: F, val y: S, val z: T, val w: Z)

    /**
     * for println and serializable usecases, offload that stuff using this method.
     */
    val quad
        get() = /*let { (a, b, c) -> Triple(a, b, c) }*/{
            let { (a, b, c, d) -> Quad(a, b, c, d) }
        }

    operator fun component4(): Z = fourth

    companion object {
        operator fun <F, S, T, Z> invoke(f: F, s: S, t: T, z: Z): Qu4d<F, S, T, Z> =
            object : Qu4d<F, S, T, Z>, Tripl3<F, S, T> by Tripl3(f, s, t) {
                override val fourth: Z get() = z
            }

        operator fun <F, S, T, Z> invoke(p: Quad<F, S, T, Z>) = p.let { (f, s, t, z) ->
            Qu4d(f, s, t, z)
        }
        operator fun <F, S, T, Z> invoke(p: Array<*>) = p.let { (f, s, t, z) ->
            @Suppress("UNCHECKED_CAST")
            Qu4d(f as F, s as S, t as  T, z as Z)
        }

        operator fun <F, S, T, Z> invoke(p: List<*>) = p.let { (f, s, t, z) ->
            @Suppress("UNCHECKED_CAST")
            Qu4d(f as F, s as S, t as  T, z as Z)
        }
    }
}


/**
 * short for tuple1
 */
val  <F> F.t1 get() =   columnar.Un1t(this)

/**
 * means  either/both  of "tuple2" and  disambiguation of  pair =x "to" y
 */
infix fun <F, S> F.t2(s: S) =   let { f -> Pai2(f, s) }
/**
 * means  either/both  of "tuple3" and  progression of  triple =x "to" y "by" z
 */

infix fun <F, S, T, P : Pai2<F, S>> P.t3(t: T) = let { (a, b) -> Tripl3(a, b, t) }
infix fun <F, S, T, P : kotlin.Pair<F, S>> P.t3(t: T) = let { (a, b) -> Tripl3(a, b, t) }

/**
 * therblig methodology
 */
typealias XY<X, Y> = Pai2<X, Y>

typealias XYZ<X, Y, Z> = Tripl3<X, Y, Z>
/**
 * homage to eclipse types
 */
typealias Tw1n<X> = XY<X, X>