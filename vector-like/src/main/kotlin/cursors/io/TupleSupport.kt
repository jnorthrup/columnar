package cursors.io

import vec.macros.*
import java.util.*
import kotlin.collections.HashMap

object Vect02_ {
    val <F, S> Vect02<F, S>.left get() = this.α(Pai2<F, S>::first)
    val <F, S> Vect02<F, S>.right get() = this α Pai2<F, S>::second
    val <F, S> Vect02<F, S>.reify get() = this α Pai2<F, S>::pair

    inline fun <F, S> Vect02<F, S>.toMap(theMap: MutableMap<F, S>? = null) =
        toList().map(Pai2<F, S>::pair).let { paris ->
            (if (theMap != null) theMap.let { paris.toMap(theMap as MutableMap<in F, in S>) } else paris.toMap(
                LinkedHashMap(paris.size)
            ))
        }
}

object V3ct0r_ {
    val <F, S, T> V3ct0r<F, S, T>.left get() = this α Tripl3<F, *, *>::first
    val <F, S, T> V3ct0r<F, S, T>.mid get() = this α Tripl3<F, S, T>::second
    val <F, S, T> V3ct0r<F, S, T>.right get() = this α Tripl3<F, S, T>::third
    val <F, S, T> V3ct0r<F, S, T>.reify get() = this α Tripl3<F, S, T>::triple
    val <F, S, T> V3ct0r<F, S, T>.x get() = this α (XYZ<F, S, T>::first)
    val <F, S, T> V3ct0r<F, S, T>.y get() = this α (XYZ<F, S, T>::second)
    val <F, S, T> V3ct0r<F, S, T>.z get() = this α (XYZ<F, S, T>::third)
    val <F, S, T> V3ct0r<F, S, T>.r3ify get() = this α (XYZ<F, S, T>::triple)
}
