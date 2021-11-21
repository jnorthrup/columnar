package ports

actual fun Float.toXBits(): Int=   java.lang.Float.floatToRawIntBits(this)