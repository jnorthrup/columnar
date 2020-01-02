package columnar.context

import columnar.IOMemento
import columnar.Vect0r
import columnar.toVect0r
import kotlin.coroutines.CoroutineContext

sealed class Arity : CoroutineContext.Element {
    override val key get() = arityKey

    companion object {
        val arityKey = object :
            CoroutineContext.Key<Arity> {}
    }
}

open class Scalar(val type: IOMemento, name: String? = null) : Arity()
/**Borg reference*/
class UniMatrix(type: IOMemento, val shape: Vect0r<Int>) : Scalar(type)

class Columnar(val type: Vect0r<IOMemento>, val names: Vect0r<String>? = null) : Arity() {
    companion object {
        fun of(vararg type: IOMemento) = Columnar(type.toVect0r() as Vect0r<IOMemento>)
        fun of(mapping: Iterable<Pair<String, IOMemento>>) =
            Columnar(mapping.map { it.second }.toVect0r(), mapping.map { it.first }.toVect0r())
    }
}

class Variadic(val types: () -> Vect0r<IOMemento>) : Arity()
