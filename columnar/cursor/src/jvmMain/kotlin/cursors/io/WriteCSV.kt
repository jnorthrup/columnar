package cursors.io

import cursors.Cursor
import kotlin.io.path.Path
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import vec.macros.Vect02_.left
import vec.macros.Vect02_.right
import vec.macros.size
import vec.macros.toList
import vec.util.FibonacciReporter
import vec.util.fib
import vec.util.path
import java.nio.file.Files
import kotlin.math.max
import kotlin.time.Duration

fun Cursor.writeCSV(fn: String) {
    var trigger = 0
    var countdown = 1
    val eol = '\n'.toChar()
    val sep = ','.toChar()
    Files.newOutputStream(fn.path).bufferedWriter().use { fileWriter ->
        val begin = Clock.System.now()
        val xsize = colIdx.size
        fileWriter.appendln(colIdx.right.toList().joinToString(","))
        val fibonacciReporter = FibonacciReporter(size)
        for (iy in 0 until size) {
            val (_, row) = second(iy).left
            repeat(xsize) { ix ->
                if (0 < ix) fileWriter.append(sep)
                val cell1 = row(ix)

                //ignore 0's
                when (cell1) {
                    is Boolean -> if (cell1) fileWriter.append('1'.toChar())
                    is Byte -> if (cell1 != 0.toByte()) fileWriter.append("$cell1")
                    is Int -> if (cell1 != 0) fileWriter.append("$cell1")
                    is Long -> if (cell1 != 0) fileWriter.append("$cell1")
                    is Double -> if (cell1 != 0.0) fileWriter.append("$cell1")
                    is Float -> if (cell1 != 0f) fileWriter.append("$cell1")
                    else -> fileWriter.append("$cell1")
                }
            }


            fileWriter.append(eol)
                .also {fibonacciReporter.report(iy)}

                    }
                }
        }
