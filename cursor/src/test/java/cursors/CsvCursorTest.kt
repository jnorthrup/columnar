package cursors

import cursors.context.TokenizedRow
import cursors.context.TokenizedRow.Companion.CsvArraysCursor
import cursors.io.IOMemento.*
import cursors.io.RowVec
import junit.framework.Assert.assertEquals
import org.junit.Test
import vec.macros.Vect02_.left
import vec.macros.get
import vec.util._v
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate
import kotlin.streams.asSequence


class CsvCursorTest {
    val dt = _v[IoLocalDate as TypeMemento, IoInt, IoString, IoString, IoString, IoString, IoString, IoString,
            IoString, IoString, IoString, IoInt, IoInt, IoInt]


    val path: Path = Paths.get("src/test/resources/calendar.csv")

    @Test
    fun HeapResidentCsvCursor() {
        val csvLines = Files.readAllLines(path)

        val curs = CsvArraysCursor(csvLines, dt)

        val testRow: RowVec = curs at 1

        val value = testRow.left[0]
        print(value)
        assertEquals(value, LocalDate.parse("2011-01-30"))
    }

    @Test
    fun ArrayCsvCursor() {

        val curs = TokenizedRow.CsvArraysCursor(Files.lines(path).asSequence().asIterable(), dt)

        val testRow: RowVec = curs at 1

        val value = testRow.left[0]
        print(value)
        assertEquals(value, LocalDate.parse("2011-01-30"))
    }

}
