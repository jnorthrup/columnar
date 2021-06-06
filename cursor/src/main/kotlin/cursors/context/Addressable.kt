package cursors.context

import kotlin.coroutines.CoroutineContext

sealed class Addressable : CoroutineContext.Element {


    override val key get() = addressableKey

    companion object {
        val addressableKey = object :
            CoroutineContext.Key<Addressable> {}
    }
}

abstract class Forward<T> : Iterable<T>, Addressable()

class Indexable(
    /**count of records*/
    val size: () -> Int,
    val seek: (Int) -> Unit,
) : Addressable()

open class Abstract<T, Q>(val size: () -> Q, val seek: (T) -> Unit) : Addressable()
class Associative<T>(size: () -> Int, seek: (T) -> Unit) : Abstract<T, Int>(size, seek)
