package ports

import kotlin.assert as kassert

actual fun assert(c: Boolean, lazy: () -> String): Unit =kassert(c)
actual fun assert(c: Boolean, lazy: () -> String): Unit =kassert(c,lazy)