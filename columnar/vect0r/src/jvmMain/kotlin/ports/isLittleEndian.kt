package ports

import java.nio.ByteOrder


actual val isLittleEndian: Boolean by lazy { ByteOrder.nativeOrder()== ByteOrder.LITTLE_ENDIAN }