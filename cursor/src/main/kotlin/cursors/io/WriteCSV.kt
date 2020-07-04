package cursors.io

import cursors.Cursor
import vec.macros.size
import vec.macros.toList
import vec.util.fib
import vec.util.logDebug
import vec.util.path
import java.nio.file.Files

fun Cursor.writeCSV(fn: String) {

    var trigger = 0
    var countdown = 1
    val eol = '\n'.toInt()
    val sep = ','.toInt()
    Files.newOutputStream(fn.path).bufferedWriter().use { fileWriter ->
        var begin = System.currentTimeMillis()
        val xsize = colIdx.size
        fileWriter.appendLine(colIdx.right.toList().joinToString(","))
        for (iy in 0 until size) {
            val (_, cell) = second(iy).left
            for (ix in 0 until xsize) {
                if (0 < ix) fileWriter.write(sep)
                fileWriter.write("${cell(ix)}")
            }
            fileWriter.write(eol).also {
                if (--countdown == 0) {
                    logDebug {
                        //without -ea this benchmark only costs a unused variable decrement.
                        countdown = fib(++trigger)
                        val l = (System.currentTimeMillis() - begin) / 1000
                        "written $iy rows in ${l}s (${iy.toFloat()/l.toFloat()})/s" }
                }
            }
        }
    }
}