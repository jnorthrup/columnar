package cursors

import cursors.context.*
import cursors.context.TokenizedRow.Companion.CsvLinesCursor
import cursors.io.IOMemento.*
import cursors.io.RowVec
import cursors.io.left
import org.junit.jupiter.api.*
import vec.macros.*
import vec.util.*
import java.nio.file.*
import java.time.LocalDate
import kotlin.test.assertEquals



class CsvCursorTest {        val dt = _v[IoLocalDate, IoInt, IoInt, IoInt, IoInt, IoInt, IoInt, IoString,
        IoString, IoString, IoString, IoInt, IoInt, IoInt]


    val path = Paths.get("src/test/resources/calendar.csv")

    @Test
    fun HeapResidentCsvCursor() {
     val csvLines = Files.readAllLines(path).toVect0r()

        val curs =  CsvLinesCursor(csvLines, dt)

        val testRow: RowVec = curs at 1

        val value = testRow.left[0]
        print(value)
        assertEquals(value, LocalDate.parse("2011-01-30"))
    }
    @Test
    fun ArrayCsvCursor() {

        val curs =  TokenizedRow.CsvArraysCursor(Files.readAllLines(path), dt)

        val testRow: RowVec = curs at 1

        val value = testRow.left[0]
        print(value)
        assertEquals(value, LocalDate.parse("2011-01-30"))
    }

}
