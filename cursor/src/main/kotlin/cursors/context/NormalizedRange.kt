package cursors.context

import vec.macros.Tw1n
import kotlin.coroutines.CoroutineContext


/**
 * this counterpart to feature_range functions , takes a Tw1n, and links this to a column meta
 */
class NormalizedRange<T>(range: Tw1n<T>) : CoroutineContext.Element {
    companion object {
        val normalizedRangeKey = object : CoroutineContext.Key<NormalizedRange<*>> {}
    }

    override val key: CoroutineContext.Key<*>
        get() = normalizedRangeKey

}