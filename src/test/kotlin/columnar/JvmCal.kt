package columnar

import java.time.chrono.*

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
}