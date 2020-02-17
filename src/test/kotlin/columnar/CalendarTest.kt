package columnar

import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.chrono.*
import java.time.temporal.ChronoField
import java.time.temporal.TemporalAdjusters


object TestDate {
    @JvmStatic
    fun main(args: Array<String>) {
        val x: IsoChronology

    }
}

class CalendarTest {
    @Test
    fun testHijRah() {
        //first day of Ramadan, 9th month
        val ramadan = HijrahDate.now()
            .with(ChronoField.DAY_OF_MONTH, 1).with(ChronoField.MONTH_OF_YEAR, 9)
        println("HijrahDate : $ramadan")

        //HijrahDate -> LocalDate
        println("\n--- Ramandan 2016 ---")
        println("Start : " + LocalDate.from(ramadan))

        //until the end of the month
        println("End : " + LocalDate.from(ramadan.with(TemporalAdjusters.lastDayOfMonth())))
    }
    enum class x(val jvmProxy: Chronology) {
        a(IsoChronology.INSTANCE),
        b(ThaiBuddhistChronology.INSTANCE),
        c(MinguoChronology.INSTANCE),
        d(JapaneseChronology.INSTANCE),
        e(HijrahChronology.INSTANCE)
    }}