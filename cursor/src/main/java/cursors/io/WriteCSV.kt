package cursors.io

import cursors.Cursor
import vec.macros.Vect02_.left
import vec.macros.Vect02_.right
import vec.macros.size
import vec.macros.toList
import vec.util.fib
import vec.util.path
import java.lang.System.err
import java.nio.file.Files
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import kotlin.math.max

/**
 * writes a csv where true=1 and {null|false|0} =''
 */
fun Cursor.writeCSV(fn: String){
    var trigger = 0
    var countdown = 1
    val eol = '\n'
    val sep = ','
    Files.newOutputStream(fn.path).bufferedWriter().use { fileWriter ->
        val begin = Instant.now()
        val xsize = colIdx.size
        fileWriter.appendLine(colIdx.right.toList().joinToString(","))
        for (iy in 0 until size) {
            val (_, row) = second(iy).left
            repeat(xsize) { ix ->
                if (0 < ix) fileWriter.append(sep)
                val cell1 = row(ix)?.also { cell1 ->
                    //ignore 0's
                    when (cell1) {
                        is Boolean -> if (cell1) fileWriter.append('1')
                        is Byte -> if (cell1 != 0.toByte()) fileWriter.append("$cell1")
                        is Int -> if (cell1 != 0) fileWriter.append("$cell1")
                        is Long -> if (cell1 != 0) fileWriter.append("$cell1")
                        is Double -> if (cell1 != 0.0) fileWriter.append("$cell1")
                        is Float -> if (cell1 != 0f) fileWriter.append("$cell1")
                        else -> fileWriter.append("$cell1")
                    }
                }
            }
            fileWriter.append(eol)
                    .also {
                        if (--countdown == 0) {
                            //without -ea this benchmark only costs a unused variable decrement.
                            countdown = fib(++trigger)
                            val l = Instant.now().minusMillis(begin.toEpochMilli()).toEpochMilli()
                            val sofar = Duration.ofMillis(l)
                            val perUnit = sofar.dividedBy(max(iy, 1).toLong())
                            val remaining = perUnit.multipliedBy(size.toLong()).minus(sofar)
                            err.println(
                                    "written $iy rows in ${sofar} remaining: $remaining est ${
                                        LocalDateTime.now().plus(remaining)
                                    } "
                                    /* "times have changed"
                                                                "written $iy rows in ${sofar} ${
                                                                    Duration.ofSeconds(1).dividedBy(max (1,perUnit.))
                                                                }/s remaining: $remaining est ${LocalDateTime.now().plus(remaining)} "
                                    */
                            )
                        }
                    }
        }
    }
}
