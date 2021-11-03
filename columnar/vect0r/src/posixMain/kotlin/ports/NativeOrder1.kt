package ports

import platform.posix.BIG_ENDIAN

actual fun nativeOrder1(): ByteOrder =if (platform.posix.BYTE_ORDER == BIG_ENDIAN) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN