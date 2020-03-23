package columnar.calendar

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.chrono.*
import java.time.temporal.ChronoField
import java.time.temporal.UnsupportedTemporalTypeException
import java.util.*

/**
 * jvm supported calndars https://en.wikipedia.org/wiki/Category:Specific_calendars
 */
enum class JvmCal(val jvmProxy: Chronology) {
    Iso(IsoChronology.INSTANCE),
    ThaiBuddhist(ThaiBuddhistChronology.INSTANCE),
    Minguo(MinguoChronology.INSTANCE),
    Japanese(JapaneseChronology.INSTANCE),
    Hijrah(HijrahChronology.INSTANCE)
    ;

    data class dinfo(
        val id: String,//jvmProxy.id,
        val calendarType: String,// this.jvmProxy.calendarType,

        val dateEpochDay: ChronoLocalDate,// jvmProxy.dateEpochDay(   0) ,
        val dateNow: ChronoLocalDate,// jvmProxy.dateNow(),
        val eras: MutableList<Era>  // jvmProxy.eras()
    )

    fun info() = dinfo(
        jvmProxy.id,
        this.jvmProxy.calendarType,
        jvmProxy.dateEpochDay(0),
        jvmProxy.dateNow(),
        jvmProxy.eras()
    )

    fun date(localDate: LocalDate) = jvmProxy.date(
        ZonedDateTime.ofInstant(
            localDate.atStartOfDay().toInstant(
                ZoneOffset.UTC
            ), ZoneId.systemDefault()
        )
    )

    companion object {
        val dateCat = EnumSet.of(
            ChronoField.YEAR,
            ChronoField.MONTH_OF_YEAR,
            ChronoField.DAY_OF_MONTH,
            ChronoField.DAY_OF_WEEK,
            ChronoField.ALIGNED_WEEK_OF_MONTH
        )
    }

    fun DateWiseCategories(
        localDate: LocalDate
    ): List<Pair<String, Int>> {
        val date: ChronoLocalDate = date(localDate)

        val filterNotNull = dateCat.map { chronoField: ChronoField ->
            try {
                chronoField.name to date[chronoField]
            } catch (t: UnsupportedTemporalTypeException) {
                null
            }
        }.filterNotNull()
        return filterNotNull
    }

}