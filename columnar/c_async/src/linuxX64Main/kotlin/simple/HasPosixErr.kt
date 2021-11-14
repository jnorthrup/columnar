package simple

import kotlinx.cinterop.toKString
import platform.posix.errno
import platform.posix.strerror

interface HasPosixErr {
    companion object {
        /**strerror [manpage](https://www.man7.org/linux/man-pages/man2/strerror.2.html) */
        fun reportErr(res: Int): String = "$res ${strerror(errno)?.toKString() ?: "<trust me>"}"
    }
}