package ports

actual fun assert(c: Boolean) {
}

actual fun assert(c: Boolean, lazy: () -> String) {
}