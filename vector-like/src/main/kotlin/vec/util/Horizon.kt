package vec.util

import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

/** gradually compressed index accessor to underlying Cursor x values. */
fun horizon(index: Int, viewPoints: Int, datapoints: Int) = max(index,
    (datapoints.toDouble() - sin(((viewPoints.toDouble() - index-1) / (viewPoints.toDouble()-1)) * (PI / 2.0)) *( datapoints.toDouble()  )).toInt())

/** gradually compressed index accessor to underlying Cursor x values. */
fun hzInvSqr(index: Int, viewPoints: Int, datapoints: Int) = max(index,
    (1/(((viewPoints.toDouble()-index).toDouble()*datapoints.toDouble() )) *Math.pow( datapoints.toDouble(),2.0)).toInt())

fun main(args: Array<String>) {

val evens=    args.map(String::toInt).zipWithNext();
    val intRange =  (0 .. evens.lastIndex   step  2).map {
        x->
        val (a,b)=evens[ x]
        _a[::horizon /*, ::hzInvSqr*/ ].map {fn->
            println("--------$fn")
            println("showing $a of $b ${
                (0 until a ).map {
                    val viewPoints = a.toInt()
                    val datapoints = b.toInt()
                    fn(  it, viewPoints, datapoints)
                }
            }")

        }

    }

}