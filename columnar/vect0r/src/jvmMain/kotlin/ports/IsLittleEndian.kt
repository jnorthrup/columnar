package ports

import java.nio.ByteOrder

actual fun isLittleEndian(): Boolean=ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN