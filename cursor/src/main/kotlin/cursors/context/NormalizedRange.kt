package cursors.context

import vec.macros.Tw1n
import kotlin.coroutines.CoroutineContext
import cursors.calendar.feature_range
/**
 * this counterpart to  [#cursors.calendar::feature_range] functions , takes a Tw1n, and links this to a column meta
 * @see cursors.calendar.feature_range
 * @see cursors.ml.feature_range<Double>
 */
class NormalizedRange<T>(val range: Tw1n<T>) : CoroutineContext.Element {
    companion object {
        val normalizedRangeKey = object : CoroutineContext.Key<NormalizedRange<*>> {}
    }

    override val key: CoroutineContext.Key<*>
        get() = normalizedRangeKey
}