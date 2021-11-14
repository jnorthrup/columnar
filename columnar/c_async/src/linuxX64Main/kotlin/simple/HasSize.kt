package simple

import kotlinx.cinterop.*
import platform.linux.BLKGETSIZE64
import platform.posix.fstat
import platform.posix.ioctl
import platform.posix.stat
import simple.HasDescriptor.Companion.S_ISBLK
import simple.HasDescriptor.Companion.S_ISREG

interface HasSize : HasDescriptor {
    val  size: ULong
        get(): ULong = memScoped {
            val st: stat = alloc()
            val fstat = fstat(fd, st.ptr as CValuesRef<stat>)
            require(fstat >= 0) { HasPosixErr.reportErr(fstat) }
            if (S_ISBLK(st.st_mode )) {
                val bytes: CPointerVar<LongVar> = alloc()
                val posixIoctl = ioctl(fd, BLKGETSIZE64, bytes)
                require(0 == posixIoctl) { HasPosixErr.reportErr(posixIoctl) }
                return bytes.pointed!!.value.toULong()
            }
            require(S_ISREG(st.st_mode )) { "size only for BLK or Regular File" }
            return st.st_size.toULong()
        }
}