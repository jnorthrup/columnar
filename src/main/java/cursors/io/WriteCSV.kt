package cursors.io

import cursors.Cursor
import vec.macros.Vect02_
import vec.macros.size
import vec.macros.toList
import vec.util.FibonacciReporter
import vec.util.debug
import vec.util.logDebug
import vec.util.path
import java.nio.file.Files

/**
 * writes a csv where true=1 and {null|false|0} =''
 */
@JvmOverloads
fun Cursor.writeCSV(filename: String, sep: Char = ',', eol: Char = '\n') {
    Files.newOutputStream(filename.path).bufferedWriter().use { fileWriter ->
        val reporter by lazy { FibonacciReporter(size, "Csv Rows") }
        val xsize = colIdx.size
        fileWriter.appendLine(Vect02_(colIdx).right.toList().joinToString(","))
        for (iy in 0 until size) {
            val (_, row) = Vect02_(second(iy)).left
            repeat(xsize) { ix ->
                if (0 < ix) fileWriter.append(sep)
                row(ix)?.also { cell1 ->
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
            fileWriter.append(eol).debug {
                reporter.report(iy)?.debug { logDebug { it } } }
        }
    }
}
