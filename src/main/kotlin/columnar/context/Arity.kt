package columnar.context

import columnar.*
import kotlin.coroutines.CoroutineContext

sealed class Arity : CoroutineContext.Element {
    override val key get() = arityKey

    companion object {
        val arityKey = object :
            CoroutineContext.Key<Arity> {}
    }
}

open class Scalar(type: IOMemento, name: String? = null) : Pai2<IOMemento, String?> by Pai2(type, name), Arity()
/**Borg reference*/
class UniMatrix(type: IOMemento, val shape: Vect0r<Int>) : Scalar(type)

class Columnar(type: Vect0r<IOMemento>, names: Vect0r<String>? = null) :
    Pai2<Vect0r<IOMemento>, Vect0r<String>?> by Pai2(type, names), Arity() {
    companion object {
        fun of(vararg type: IOMemento): Columnar {
            val toVect0r: Vect0r<IOMemento> = (type).toVect0r() as Vect0r<IOMemento>
            return Columnar(toVect0r)
        }

        fun of(mapping: VPai2<String, IOMemento>) =
            Columnar(mapping α Pai2<String, IOMemento>::second, mapping α Pai2<String, IOMemento>::first)
    }
}

class Variadic(val types: () -> Vect0r<IOMemento>) : Arity()
