package ports

actual fun nativeOrder1() = if (java.nio.ByteOrder.nativeOrder() == java.nio.ByteOrder.BIG_ENDIAN) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN