package columnar.context

import kotlin.coroutines.CoroutineContext

sealed class Addressable : CoroutineContext.Element {
    abstract class Forward<T> : Iterable<T>, Addressable()

    class Indexable(
        /**count of records*/
        val size: () -> Int, val seek: (Int) -> Unit
    ) : Addressable()

    override val key get() = addressableKey

    class Abstract<T, Q>(val size: () -> Q, val seek: (T) -> Unit) : Addressable()
    companion object {
        val addressableKey = object :
            CoroutineContext.Key<Addressable> {}
    }
}