package cursors.io

import cursors.Cursor
import vec.macros.size
import vec.macros.toList
import vec.util.path
import java.nio.file.Files

fun Cursor.writeCSV(fn: String) {
    val eol = '\n'.toInt()
    val sep = ','.toInt()
    Files.newOutputStream(fn.path).bufferedWriter().use { fileWriter ->
        val xsize = colIdx.size
        fileWriter.appendLine(colIdx.right.toList().joinToString(","))
        for (iy in 0 until size) {
            val (_, cell) = second(iy).left
            for (ix in 0 until xsize) {
                if (0 < ix) fileWriter.write(sep)
                fileWriter.write("${cell(ix)}")
            }
            fileWriter.write(eol)
        }
    }
}