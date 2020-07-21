package cursors.tbd

import vec.macros.`→`
import java.lang.Integer.signum
import kotlin.math.abs

/**
 *
 * Generalized Hilbert ('gilbert') space-filling curve for arbitrary-sized
 * 2D rectangular grids.
 *
 * translated from: https://github.com/jakubcerveny/gilbert/blob/master/gilbert2d.py
 */

fun gilbertCurve(width: Int, height: Int, next: (Int, Int) -> Unit) =
        if (width >= height) gilbertCurve2D(0, 0, width, 0, 0, height, next) else gilbertCurve2D(0, 0, 0, height, width, 0, next)

fun gilbertCurve2D(xPrime: Int, yPrime: Int, ax: Int, ay: Int, bx: Int, by: Int, next: (Int, Int) -> Unit) {

    /**
     * performance may benefit from this being a Mutable Array passed in
     */
    var x = xPrime
    var y = yPrime


    val w = abs(ax + ay)
    val h = abs(bx + by)
    val dax = signum(ax)
    val day = signum(ay) //unit major direction
    val dbx = signum(bx)
    val dby = signum(by) //unit orthogonal direction
    when {
        h == 1 -> {
            //trivial row fill
            for (i in 0 until w) {
                next(x, y)
                x += dax
                y += day
            }
        }
        w == 1 -> {
            //trivial column fill
            for (i in 0 until h) {
                next(x, y)
                x += dbx
                y += dby
            }
        }
        else -> {
            var ax2 = ax / 2
            var ay2 = ay / 2
            var bx2 = bx / 2
            var by2 = by / 2
            val w2 = abs(ax2 + ay2)
            val h2 = abs(bx2 + by2)
            when {
                2 * w > 3 * h -> {
                    if (w2 % 2 != 0 && w > 2) {
                        //prefer even steps
                        ax2 += dax
                        ay2 += day
                    }

                    //long case: split in two parts only
                    gilbertCurve2D(x, y, ax2, ay2, bx, by, next)
                    gilbertCurve2D(x + ax2, y + ay2, ax - ax2, ay - ay2, bx, by, next)
                }
                else -> {
                    if (h2 % 2 != 0 && h > 2) {
                        //prefer even steps
                        bx2 += dbx
                        by2 += dby
                    }

                    //standard case: one step up, one long horizontal, one step down
                    gilbertCurve2D(x, y, bx2, by2, ax2, ay2, next)
                    gilbertCurve2D(x + bx2, y + by2, ax, ay, bx - bx2, by - by2, next)
                    gilbertCurve2D(x + (ax - dax) + (bx2 - dbx), y + (ay - day) + (by2 - dby),
                            -bx2, -by2, -(ax - ax2), -(ay - ay2), next)
                }
            }
        }
    }
}


fun audit(x: Int, y: Int) = x to y `→` ::println

fun main(vararg args: String) = gilbertCurve(width = 16, height = 33, ::audit)
