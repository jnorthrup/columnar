package simple

import platform.posix.*
import kotlinx.cinterop.*
import linux_uring.AT_FDCWD
import simple.HasPosixErr.Companion.posixRequires
import simple.HasPosixErr.Companion.reportErr
import simple.HasPosixErr.Companion.warning
import platform.posix.mmap as posix_mmap
import platform.posix.open as posix_open

/**
opens file for syncronous read/write
 */
class PosixFile(
    val path: String?, O_FLAGS: Int = O_RDWR or O_SYNC,
    override val fd: Int = run {
        posix_open(path, O_FLAGS)
    },
) : HasDescriptor, HasSize {
    override fun read64(buf: ByteArray): ULong {
        val addressOf = buf.pin().addressOf(0)
        val b: CArrayPointer<ByteVar> = addressOf.reinterpret<ByteVar>()
        val read = read(fd, b, buf.size.toULong())
        posixRequires(read >= 0) { "read failed with result ${reportErr(read.toInt())}" }
        return read.toULong()
    }

    override fun close(): Int {
        val close = close(fd)
        posixRequires(close >= 0) { "close failed with result ${reportErr(close)}" }
        return close
    }

    override fun write64(buf: ByteArray): ULong {
        val b: Long = buf.pin().objcPtr().toLong()
        val write = write(fd, b.toCPointer<ByteVar>()!!, buf.size.toULong())
        posixRequires(write >= 0) { "write failed with result ${reportErr(write.toInt())}" }
        return write.toULong()
    }


    /**lseek [manpage](https://www.man7.org/linux/man-pages/man2/lseek.2.html) */
    override fun seek(offset: __off_t, whence: Int): ULong {
        val offr: __off_t = lseek(fd, offset, whence)
        posixRequires(offr >= 0) { "seek failed with result ${reportErr(res = offr.toInt())}" }
        return offr.toULong()
    }

    /** mmap [manpage](https://www.man7.org/linux/man-pages/man2/mmap.2.html) */
    fun mapBag(
        len: ULong,
        prot: Int = PROT_READ or PROT_WRITE,
        flags: Int = MAP_SHARED or MAP_ANONYMOUS,
        offset: off_t = 0L,
    ): COpaquePointer? = mmap_base(fd = -1, __len = len, __prot = prot, __flags = flags, __offset = offset)

    /** mmap [manpage](https://www.man7.org/linux/man-pages/man2/mmap.2.html) */
    fun mmap(len: ULong, prot: Int = PROT_READ, flags: Int = MAP_SHARED, offset: off_t = 0L) = mmap_base(fd = fd,
        __len = len,
        __prot = prot,
        __flags = flags,
        __offset = offset).reinterpret<CArrayPointerVar<ByteVar>>()

    /** lseek [manpage](https://www.man7.org/linux/man-pages/man2/leek.2.html) */
    fun at(offset: __off_t, whence: Int) {
        lseek(fd, offset /* = kotlin.Long */, __whence = whence)

    }

    companion object {
        /**open [manpage](https://www.man7.org/linux/man-pages/man2/open.2.html) */

        fun open(path: String?, O_FLAGS: Int): Int {
            val fd = posix_open(path, O_FLAGS)
            posixRequires(fd > 0) { "File::open $path returned ${reportErr(fd)}" }
            return fd
        }

        val page_size by lazy { sysconf(_SC_PAGE_SIZE) }

        /**
         * [manpage](https://www.man7.org/linux/man-pages/man2/mmap.2.html)
         */
        fun mmap_base(
            __addr: kotlinx.cinterop.CValuesRef<*>? = 0L.toCPointer<ByteVar>(),
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
            /**
             * exactly one:
             *        MAP_SHARED MAP_SHARED_VALIDATE MAP_PRIVATE
             *
             *  |=MAP_ANON MAP_FIXED MAP_FIXED_NOREPLACE MAP_GROWSDOWN MAP_HUGETLB
             *   MAP_HUGE_2MB MAP_HUGE_1GB MAP_LOCKED MAP_NONBLOCK MAP_NORESERVE
             *   MAP_POPULATE MAP_STACK MAP_SYNC MAP_UNINITIALIZED
             */
            __flags: kotlin.Int,
            fd: Int,
            __offset: platform.posix.__off_t,
        ): kotlinx.cinterop.COpaquePointer {
            warning(__offset % page_size == 0L) { "$__offset requires blocksize of $page_size" }
            val cPointer = posix_mmap(__addr, __len, __prot, __flags, fd, __offset)
            posixRequires(cPointer.toLong() != -1L) {
                "mmap failed with result ${
                    reportErr(cPointer.toLong().toInt())
                }"
            }

            return cPointer!!


        }
        fun getDirFd(namedDirAndFile: List<String>): Int = if (namedDirAndFile.first().isNullOrEmpty()) {
            AT_FDCWD
        } else {
            posix_open(namedDirAndFile.first(), linux_uring.O_DIRECTORY).also {
                posixRequires(it > 0) { "opendir ${namedDirAndFile.first()}" }
            }
        }

        fun namedDirAndFile(file_path: String) = file_path.lastIndexOf('/').let { tail ->

            if (tail == -1) listOf("", file_path) else listOf(
                file_path.substring(0, tail),
                file_path.substring(tail.inc())
            )
        }
    }
}
