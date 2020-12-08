package cursors.calendar

import java.time.LocalDate

fun daySeq(min: LocalDate, max: LocalDate): Sequence<LocalDate> {
    var marker = min
    return sequence {
        while (max > marker) {
            yield(marker)
            marker = marker.plusDays(1)
        }
    }
}