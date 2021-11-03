package ports

actual fun assert(c: Boolean) =kotlin.assert(c)
actual fun assert(c: Boolean, lazy:()->String) =kotlin.assert(c){lazy()}