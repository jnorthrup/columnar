package cursors.io

import cursors.Cursor
import cursors.context.TokenizedRow
import java.nio.file.Files
import java.nio.file.Paths

object CsvReader {
    fun csvReader() = this
    fun open(pathToLabels: String): Cursor = TokenizedRow.CsvArraysCursor(Files.readAllLines(Paths.get(pathToLabels)))
}

