package cursors.io

import cursors.Cursor
import vec.macros.size
import vec.macros.toList
import vec.util.fib
import vec.util.logDebug
import vec.util.path
import java.io.FileWriter
import java.nio.file.Files
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import kotlin.math.max

fun Cursor.writeCSV(fn: String) {

    var trigger = 0
    var countdown = 1
    val eol = '\n'.toChar()
    val sep = ','.toChar()
    Files.newOutputStream(fn.path).bufferedWriter().use { fileWriter ->
        var begin = Instant.now()
        val xsize = colIdx.size
        fileWriter.appendLine(colIdx.right.toList().joinToString(","))
        for (iy in 0 until size) {
            val (_, cell) = second(iy).left
            val r = StringBuilder()
            for (ix in 0 until xsize) {
                if (0 < ix) r.append(sep)
                val cell1 = cell(ix)

                //ignore 0's
                when (cell1) {
                    is Boolean -> if (cell1) r.append('1'.toChar())
                    is Int -> if (cell1 != 0) r.append("$cell1")
                    is Long -> if (cell1 != 0) r.append("$cell1")
                    is Double -> if (cell1 != 0.0) r.append("$cell1")
                    is Float -> if (cell1 != 0f) r.append("$cell1")
                    else r.append("$cell1")
                }
            }
            r.append(eol)
            fileWriter.write(r.toString()).also {
                if (--countdown == 0) {
                    logDebug {
                        //without -ea this benchmark only costs a unused variable decrement.
                        countdown = fib(++trigger)
                        val l = Instant.now().minusMillis(begin.toEpochMilli()).toEpochMilli()
                        val sofar = Duration.ofMillis(l)
                        val perUnit = sofar.dividedBy(max(iy, 1).toLong())
                        val remaining = perUnit.multipliedBy(size.toLong()).minus(sofar)
                        "written $iy rows in ${sofar} ${Duration.ofSeconds(1).dividedBy(perUnit)}/s remaining: $remaining est ${LocalDateTime.now().plus(remaining)} "
                    }
                }
            }
        }
    }
}