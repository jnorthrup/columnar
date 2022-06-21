@file:OptIn(ExperimentalTime::class)

package vec.util

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

@OptIn(ExperimentalTime::class)

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
                val secondsSinceBegin = l.inWholeSeconds

                "logged $iy $noun in $l ${(iy.toDouble() / (secondsSinceBegin.toDouble())).toFloat()}/s " + (size?.let {
                    val ticksLeft = size - iy
                    val remaining: Duration = slice * ticksLeft
                    "remaining: $remaining est ${Clock.System.now().plus(remaining).toLocalDateTime(TimeZone.currentSystemDefault())}"
                } ?: "")
            } else null
}