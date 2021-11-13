package cursors.calendar

import vec.macros.Pai2
import vec.macros.t2
import vec.ml.featureRange
import java.time.LocalDate

@JvmName("FRLocalDate")
fun <T : LocalDate> featureRange(
    seq: Iterable<T>,
    maxMinPai2: Pai2<LocalDate, LocalDate> = LocalDate.MAX t2 LocalDate.MIN,
): Pai2<LocalDate, LocalDate> = featureRange(seq, maxMinPai2)