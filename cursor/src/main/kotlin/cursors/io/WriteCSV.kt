package cursors.io

import cursors.Cursor
import cursors.at
import vec.macros.map
import vec.macros.size
import vec.macros.toList
import java.nio.file.Files
import java.nio.file.Paths

fun Cursor.writeCSV(fn: String) {
    Files.newOutputStream(
            Paths.get(fn)
    ).bufferedWriter().use {
        fileWriter ->
        fileWriter.appendLine(colIdx.right.toList().joinToString(","))
        (0 until size).forEach {
            fileWriter.appendLine((this at it).left.map(Any?::toString).toList().joinToString(","))
        }
    }
}