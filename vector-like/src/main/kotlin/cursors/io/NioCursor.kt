package cursors.io

import vec.macros.*
import java.util.*
import kotlin.collections.HashMap

object Vect02_ {
    inline val <reified F, reified S> Vect02<F, S>.left get() = this.α(Pai2<F, S>::first)
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
    inline val <reified F, reified S, reified T> V3ct0r<F, S, T>.x get() = this α (XYZ<F, S, T>::first)
    inline val <reified F, reified S, reified T> V3ct0r<F, S, T>.y get() = this α (XYZ<F, S, T>::second)
    inline val <reified F, reified S, reified T> V3ct0r<F, S, T>.z get() = this α (XYZ<F, S, T>::third)
    inline val <reified F, reified S, reified T> V3ct0r<F, S, T>.r3ify get() = this α (XYZ<F, S, T>::triple)
}
