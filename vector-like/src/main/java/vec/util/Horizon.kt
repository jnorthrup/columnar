package vec.util

import kotlin.math.PI
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

/** gradually compressed index accessor to underlying Cursor x values. */
@JvmOverloads
fun horizon(
    index: Int,
    viewPoints: Int,
    datapoints: Int,
    dpDouble: Double = datapoints.toDouble(),
    vpDouble: Double = viewPoints.toDouble()
): Int {
    return max(
        index,
        (dpDouble - 1 - sin((vpDouble - index) / vpDouble * (PI / 2.0)) * dpDouble - 1).toInt()
    )
}

/** gradually compressed index accessor to underlying Cursor x values. */
@JvmOverloads
fun hzInvSqr(index: Int, datapoints: Int, dpDub: Double = datapoints.toDouble(), vpDub: Double): Int = max(
    index,
    (1.0 / ((vpDub - index.toDouble()) * dpDub) * dpDub.pow(2.0)).toInt()
)

fun main(args: Array<String>) {

    val evens = args.map(String::toInt).zipWithNext();
    val intRange = (0..evens.lastIndex step 2).map { x ->
        val (a, b) = evens[x]
        val arrayOfKFunctions: Array<(Int, Int, Int) -> Int> =
            _a[::horizon as (Int, Int, Int) -> Int, ::hzInvSqr as (Int, Int, Int) -> Int]
        arrayOfKFunctions.map { fn: (Int, Int, Int) -> Int ->
            println("--------$fn")
            println("showing $a of $b ${
                (0 until a).map {
                    val viewPoints = a.toInt()
                    val datapoints = b.toInt()
                    fn(it, viewPoints, datapoints)
                }
            }")
        }
    }
}