package mu

import vec.util.logDebug

object KLogger {
    fun debug(debugTxt: () -> String):Unit {
        logDebug(debugTxt)}
    fun debug(s: String) {
        logDebug { s }
    }

    fun info(function: () -> String) {
        debug( function)
    }

    fun warn(function: () -> String) {
        debug(function)
    }

    fun error(function: () -> String) {
        debug(function)
    }
}
