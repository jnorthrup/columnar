package vec.util

import ports.assert


fun logDebug(debugTxt: () -> String) = try {
    assert(false) { debugTxt() }
} catch (_: AssertionError) {
    println("debug: $debugTxt")
}