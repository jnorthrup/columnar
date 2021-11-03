package ports

actual fun Double.toXBits(): Long = java.lang.Double.doubleToRawLongBits(this)