package ports

actual fun fromXBits(t: Int) = java.lang.Float.intBitsToFloat(t)
