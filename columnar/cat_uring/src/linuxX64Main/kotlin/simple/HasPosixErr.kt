package simple

import kotlinx.cinterop.toKString
import platform.posix.errno
import platform.posix.strerror


interface HasPosixErr {
    companion object {
        /**strerror [manpage](https://www.man7.org/linux/man-pages/man2/strerror.2.html) */
        fun reportErr(res: Any?): String = "$res ${strerror(errno)?.toKString() ?: "<trust me>"}"

        /**
         * more terse when errno
         */
        fun posixRequires(mustBe: Boolean, res: Any?) {
            require(mustBe) { HasPosixErr.reportErr(res) }
        }

        /**a non-throwing require*/
        fun warning(cond: Boolean, lazyz: () -> Any?) = Unit.also { if (!cond) println("**warning: ${lazyz()}") }

    }
}

