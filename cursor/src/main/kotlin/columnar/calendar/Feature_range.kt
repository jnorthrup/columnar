package columnar.calendar

import columnar.macros.Pai2
import columnar.macros.t2
import columnar.ml.featureRange
import java.time.LocalDate

@JvmName("FRLocalDate")
inline fun <reified T : LocalDate> feature_range(
    seq: Sequence<T>,
    maxMinPai2: Pai2<LocalDate, LocalDate> = LocalDate.MAX t2 LocalDate.MIN
): Pai2<LocalDate, LocalDate> = featureRange(seq, maxMinPai2)