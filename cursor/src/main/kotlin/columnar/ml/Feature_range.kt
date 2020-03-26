package columnar.ml

import columnar.Tw1n



@JvmName("FrInt")
fun<T:Int> feature_range(
    seq: Sequence<T>,
    maxMinTwin: Tw1n<Int> = Tw1n(Int.MAX_VALUE, Int.MIN_VALUE)
) = featureRange(seq, maxMinTwin)


@JvmName("FrLong")
fun<T:Long> feature_range(
    seq: Sequence<Long>,
    maxMinTwin: Tw1n<Long> = Tw1n(Long.MAX_VALUE, Long.MIN_VALUE)

) = featureRange(seq, maxMinTwin)

@JvmName("FrDouble")
 fun<T:Double> feature_range(seq: Sequence<T>, maxMinTwin: Tw1n<Double> = Tw1n(Double.MAX_VALUE, Double.MIN_VALUE)) =
    featureRange(seq, maxMinTwin)

@JvmName("FrFloat")
fun<T:Float> feature_range(
    seq: Sequence<T>,
    maxMinTwin: Tw1n<Float> = Tw1n(Float.MAX_VALUE, Float.MIN_VALUE)
) = featureRange(seq, maxMinTwin)

 