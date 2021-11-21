package cursors.context

import cursors.TypeMemento
import cursors.io.IOMemento
import vec.macros.*
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmName
import kotlin.jvm.JvmStatic

sealed interface Arity : CoroutineContext.Element {
    override val key get() = arityKey

    companion object {
        val arityKey = object : CoroutineContext.Key<Arity> {}
    }
}

interface Scalar : Arity, Pai2<TypeMemento, String?> {

    val name: String
        get() = second
            ?: "generic${(first as? IOMemento)?.name ?: first::class.simpleName}:${first.networkSize}"

    companion object {
        @JvmStatic
        fun Scalar(type: TypeMemento, name: String? = null) = object : Scalar {
            override val first: TypeMemento
                get() = type
            override val second: String?
                get() = name
        }

        @JvmStatic
        fun Scalar(p: Pai2<TypeMemento, String?>) = Scalar(p.first, p.second)
    }
}


///**Borg reference*/
//class UniMatrix(type: TypeMemento, val shape: Vect0r<Int>, name: String? = null) : Scalar

class Columnar(cols: Vect02<TypeMemento, String?>) :
    Vect02<TypeMemento, String?> by cols, Arity {
    companion object {
        fun of(vararg type: TypeMemento): Columnar = Columnar(type α { t: TypeMemento -> t t2 null as String? })

        @JvmName("fact2")
        fun of(scalars: Vect0r<Scalar>): Columnar {
            var c = 0
            val mapping: Vect02<String, TypeMemento> = scalars α { (memento: TypeMemento, name: String?): Scalar ->
                val padStart = (c++).toString().padStart(6, '0')
                (name ?: "col$padStart") t2 memento
            }
            return Columnar(mapping.map { (a, b) -> b t2 a })
        }
    }
}

class Variadic(val types: () -> Vect0r<TypeMemento>) : Arity
