package ports

actual fun assert(c: Boolean): Unit = kotlin.assert(c)
actual fun assert(c: Boolean, lazy: () -> String): Unit = kotlin.assert(c, lazy)