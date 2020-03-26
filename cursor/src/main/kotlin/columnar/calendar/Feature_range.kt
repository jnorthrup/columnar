package columnar.calendar

import columnar.Pai2
import columnar.Tw1n
import columnar.ml.featureRange
import columnar.t2
import java.time.LocalDate

@JvmName("FRLocalDate")
fun <T : LocalDate> feature_range(
    seq: Sequence<T>,
    maxMinPai2: Pai2<LocalDate, LocalDate> = LocalDate.MAX t2 LocalDate.MIN
): Pai2<LocalDate, LocalDate>  = featureRange(seq,maxMinPai2)