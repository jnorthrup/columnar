package cursors.context

import cursors.TypeMemento
import cursors.io.IOMemento
import vec.macros.*
import kotlin.coroutines.CoroutineContext

sealed class Arity : CoroutineContext.Element {
    override val key get() = arityKey

    companion object {
        val arityKey = object :
                CoroutineContext.Key<Arity> {}
    }
}

open class Scalar(type: TypeMemento, name: String? = null) : Pai2<TypeMemento, String?> by Pai2(
        type,
        name
), Arity() {
    val name: String
        get() = this.second
                ?: "generic${(first as? IOMemento)?.name ?: first::class.java.simpleName}:${first.networkSize}"
}

/**Borg reference*/
class UniMatrix(type: TypeMemento, val shape: Vect0r<Int>, name: String? = null) : Scalar(type, name)

class Columnar(cols: Vect02<TypeMemento, String?>) :
        Vect02<TypeMemento, String?> by cols, Arity() {
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

class Variadic(val types: () -> Vect0r<TypeMemento>) : Arity()
