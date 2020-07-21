package cursors.calendar

import cursors.Cursor
import cursors.at
import cursors.context.Scalar
import cursors.io.IOMemento
import cursors.io.RowVec
import cursors.io.left
import vec.macros.*
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
            val eras: MutableList<Era>,  // jvmProxy.eras()
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


    /**
     * certain calendars should override this list
     * to reduce exception overheads
     */
    open
    val dateCat
        get() = EnumSet.of(
                ChronoField.YEAR,
                ChronoField.MONTH_OF_YEAR,
                ChronoField.DAY_OF_MONTH,
                ChronoField.DAY_OF_WEEK,
                ChronoField.ALIGNED_WEEK_OF_MONTH
        )

    open
    fun dateWiseCategories(
            localDate: LocalDate,
    ) =
            date(localDate).let { it: ChronoLocalDate? ->

                dateCat.map { chronoField: ChronoField ->
                    try {
                        chronoField.name t2 (it!![chronoField])
                    } catch (t: UnsupportedTemporalTypeException) {
                        null
                    }
                }.filterNotNull().toVect0r() as Vect02<String, Int>
            }

    /**
     * returns a cursor the length of the passed in param1
     * @param calendarCurs source timeseries cursor
     * @param localdateIndex #calendarCurs index of LocalDate
     * @param catSize the length of categories to use defaulting to all avail
     */
    fun inflate(calendarCurs: Cursor, localdateIndex: Int = 0, catSize: Int = dateWiseCategories(LocalDate.now()).size): Cursor =
            Cursor(calendarCurs.size) { iy: Int ->
                val rvec: RowVec = (calendarCurs at iy)
                val localDate: LocalDate = rvec.left[localdateIndex] as LocalDate
                val row: RowVec = dateWiseCategories(localDate) α { (nama: String, theVal: Int) ->
                    theVal t2 { Scalar(IOMemento.IoInt, "$name:$nama") }
                }
                RowVec(row.size) { ix: Int -> row[ix] }
            }
}