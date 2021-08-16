package cursors.io

import vec.util.fib
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import kotlin.math.max

class FibonacciReporter(val size: Int? = null) {
    var trigger = 0
    var countdown = 1
    val begin = Instant.now()
    fun report(iy: Int) =
        if (--countdown == 0) {
            //without -ea this benchmark only costs a unused variable decrement.
            countdown = fib(++trigger)
            val l = Instant.now().minusMillis(begin.toEpochMilli()).toEpochMilli()
            val sofar = Duration.ofMillis(l)
            val perUnit = sofar.dividedBy(max(iy, 1).toLong())
            val remaining = size?.let { perUnit.multipliedBy(size.toLong()).minus(sofar) }
            val ofSeconds = Duration.ofSeconds(1)
            val dividedBy = ofSeconds.dividedBy(max(1, perUnit.toSeconds()))
            "logged $iy rows in $sofar $dividedBy/s " + (size?.let {
                "remaining: " + remaining + " est " + LocalDateTime.now().plus(remaining)
            } ?: "")
        } else null
}