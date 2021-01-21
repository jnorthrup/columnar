package cursors.calendar

import cursors.ml.featureRange
import vec.macros.Pai2
import vec.macros.t2
import java.time.LocalDate

@JvmName("FRLocalDate")
fun <T : LocalDate> feature_range(
        seq: Sequence<T>,
        maxMinPai2: Pai2<LocalDate, LocalDate> = LocalDate.MAX t2 LocalDate.MIN
): Pai2<LocalDate, LocalDate> = featureRange(seq, maxMinPai2)