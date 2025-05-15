package org.slf4j

interface Logger {
    fun info(s: String)
    fun debug(s: String)
    fun trace(s: String)
    fun warn(s: String)
    fun isInfoEnabled():Boolean=true
    fun isDebugEnabled():Boolean=true
    fun isTraceEnabled():Boolean=true
    fun isWarnEnabled():Boolean=true
    fun info(s: String, o: Any) = info("it $o")
    fun debug(s: String, o: Any) = debug("it $o")
    fun trace(s: String, o: Any) = trace("it $o")
    fun warn(s: String, o: Any) = warn("it $o")
    fun info(s: String, o: Any, o2: Any) = info("it $o $o2")
    fun debug(s: String, o: Any, o2: Any) = debug("it $o $o2")
    fun trace(s: String, o: Any, o2: Any) = trace("it $o $o2")
    fun warn(s: String, o: Any, o2: Any) = warn("it $o $o2")
    fun info(s: String, vararg o: Any) = info("it $o")
    fun debug(s: String, vararg o: Any) = debug("it $o")
    fun trace(s: String, vararg o: Any) = trace("it $o")
    fun warn(s: String, vararg o: Any) = warn("it $o")
}
