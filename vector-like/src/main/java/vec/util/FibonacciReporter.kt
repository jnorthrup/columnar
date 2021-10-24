@file:OptIn(ExperimentalTime::class)

package vec.util

import kotlinx.datetime.Clock
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

class FibonacciReporter(val size: Int? = null, val noun: String = "rows") {
    var trigger = 0
    var countdown = 1
    val begin = TimeSource.Monotonic.markNow()
    fun report(iy: Int) =
        if (--countdown == 0) {
            //without -ea this benchmark only costs a unused variable decrement.
            countdown = fib(++trigger)
            val l = begin.elapsedNow()
            val slice = l / max(1, iy)

            "logged $iy $noun in $l ${slice.inWholeSeconds}/s " + (size?.let {
                val remaining: Duration = slice.times((size - iy))
                "remaining: $remaining est ${Clock.System.now().plus(remaining)}"
            } ?: "")
        } else null
}