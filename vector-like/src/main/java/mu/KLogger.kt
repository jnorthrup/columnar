package mu

import org.slf4j.Logger
import vec.util.logDebug

  class KLogger: Logger {
    fun debug(debugTxt: () -> String): Unit {
        logDebug(debugTxt)
    }

    override fun debug(s: String) {
        logDebug { s }
    }
    override fun trace(s: String) {
        logDebug { s }
    }
      override fun info(s: String) = logDebug { s }
    fun info(function: () -> String) {
        debug(function)
    }

    override fun warn(s: String) = logDebug { s }
    fun warn(function: () -> String) = debug(function)

    fun error(s: String) = logDebug { s }
    fun error(function: () -> String) {
        debug(function)
    }
}
