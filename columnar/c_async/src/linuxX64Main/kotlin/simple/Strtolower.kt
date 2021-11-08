package simple

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.get
import kotlinx.cinterop.set
import platform.posix.tolower

fun strtolower(str: CPointer<ByteVar>): Unit {
    var c = 0
    var b = 0
    while (str[c].toInt().also { b = it } != 0) str[c++] = tolower(b).toUByte().toByte()
}