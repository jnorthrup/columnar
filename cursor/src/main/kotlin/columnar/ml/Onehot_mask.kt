package columnar.ml

import columnar.macros.t2

/**
 * Cursors creates one series of values, then optionally omits a column from cats
 */
inline fun onehot_mask(dummy: Any?, cats: List<*>) =
    when {
        dummy is DummySpec ->
            when (dummy) {
                DummySpec.First -> 0 t2 cats.first()
                DummySpec.Last -> cats.lastIndex t2 cats.last()
//                DummySpec.None -> TODO()
            }
        dummy != null -> cats.indexOf(dummy) t2 dummy
        else -> -1 t2 Unit
    }