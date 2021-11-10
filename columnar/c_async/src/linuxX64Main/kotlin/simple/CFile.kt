package simple

import kotlinx.cinterop.*
import platform.posix.*
import platform.posix.mmap as posix_mmap
import platform.posix.open as posix_open

/**
opens file for syncronous read/write
 */
class CFile(
    val path: String?, O_FLAGS: Int = O_RDWR or O_SYNC,
    override val fd: Int = run {
        open(path, O_FLAGS)
    },
) : HasDescriptor {

    override fun read64(buf: ByteArray): ULong {
        val addressOf = buf.pin().addressOf(0)
        val b: CArrayPointer<ByteVar> = addressOf.reinterpret<ByteVar>()
        val read = read(fd, b, buf.size.toULong())
        require(read >= 0) { "read failed with result ${reportErr(read.toInt())}" }
        return read.toULong()
    }

    override fun close(): Int {
        val close = close(fd)
        require(close >= 0) { "close failed with result ${reportErr(close)}" }
        return close
    }

    override fun write64(buf: ByteArray): ULong {
        val b: Long = buf.pin().objcPtr().toLong()
        val write = write(fd, b.toCPointer<ByteVar>()!!, buf.size.toULong())
        require(write >= 0) { "write failed with result ${reportErr(write.toInt())}" }
        return write.toULong()
    }

    override fun seek(offset: __off_t, whence: Int): ULong {
        val off_r: __off_t /* = kotlin.Long */ = lseek(fd, offset, whence)
        require(off_r >= 0) { "seek failed with result ${reportErr(off_r.toInt())}" }
        return off_r.toULong()
    }

    companion object {
        /**
         * [manpage](https://www.man7.org/linux/man-pages/man2/strerror.2.html)
         */

        fun reportErr(res: Int) = "$res ${strerror(errno)?.toKString() ?: "<trust me>"}"

        /**
         * [manpage](https://www.man7.org/linux/man-pages/man2/open.2.html)
         */

        fun open(path: String?, O_FLAGS: Int): Int {
            val fd = posix_open(path, O_FLAGS)
            require(fd > 0) { "File::open $path returned ${reportErr(fd)}" }
            return fd
        }

        val page_size = sysconf(_SC_PAGE_SIZE)

        /**
         * [manpage](https://www.man7.org/linux/man-pages/man2/mmap.2.html)
         */
        fun mmap_base(
            __addr: kotlinx.cinterop.CValuesRef<*>? = null as CValuesRef<*>,
            __len: platform.posix.size_t = page_size.toULong(), /* = kotlin.ULong */
            /**
             *

            PROT_EXEC
            Pages may be executed.

            PROT_READ
            Pages may be read.

            PROT_WRITE
            Pages may be written.

            PROT_NONE
            Pages may not be accessed.

             */

            __prot: kotlin.Int,
            __flags: kotlin.Int,
            fd: Int,
            __offset: platform.posix.__off_t, /* = kotlin.Long */
        ): kotlinx.cinterop.COpaquePointer? /* = kotlinx.cinterop.CPointer<out kotlinx.cinterop.CPointed>? */ {
            val cPointer =
                posix_mmap(__addr, __len, __prot, __flags, fd, __offset)

            require(cPointer.toLong() != -1L) { "mmap failed with result ${reportErr(cPointer.toLong().toInt())}" }

            return cPointer

        }
  /**
         * [manpage](https://www.man7.org/linux/man-pages/man2/mmap.2.html)
         */

        fun mapBag(
            len: ULong,
            prot: Int = PROT_READ or PROT_WRITE,
            flags: Int = MAP_SHARED or MAP_ANONYMOUS,
            offset: off_t = 0L,
        ): COpaquePointer? =
            mmap_base(fd = -1, __len = len, __prot = prot, __flags = flags, __offset = offset)
    }

    fun mmap(len: ULong, prot: Int = PROT_READ, flags: Int = MAP_SHARED, offset: off_t = 0L): COpaquePointer? =
        mmap_base(fd = fd, __len = len, __prot = prot, __flags = flags, __offset = offset)


}

