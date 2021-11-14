package simple

import platform.posix.*
import kotlinx.cinterop.*
import platform.posix.__S_IFBLK
import platform.posix.__S_IFCHR
import platform.posix.__S_IFDIR
import platform.posix.__S_IFIFO
import platform.posix.__S_IFLNK
import platform.posix.__S_IFMT
import platform.posix.__S_IFREG

interface HasDescriptor : HasPosixErr {
    val fd: Int

    /**
     * [manpage](https://www.man7.org/linux/man-pages/man2/read.2.html)
     */
    fun read(buf: ByteArray) = read64(buf).toUInt()
    fun read64(buf: ByteArray): ULong

    /**
     * [manpage](https://www.man7.org/linux/man-pages/man2/write.2.html)
     */
    fun write(buf: ByteArray) = write64(buf).toUInt()
    fun write64(buf: ByteArray): ULong

    /**
     * [manpage](https://www.man7.org/linux/man-pages/man2/seek.2.html)
     */
    fun seek(offset: __off_t, whence: Int = SEEK_SET): ULong

    /**
     * [manpage](https://www.man7.org/linux/man-pages/man2/close.2.html)
     */
    fun close(): Int
    val isDir get()= memScoped { alloc<stat>().let{st ->fstat(fd,st.ptr) ;  S_ISDIR(st.st_mode)}}
    val isChr get()= memScoped { alloc<stat>().let{st ->fstat(fd,st.ptr) ;  S_ISCHR(st.st_mode)}}
    val isBlk get()= memScoped { alloc<stat>().let{st ->fstat(fd,st.ptr) ;  S_ISBLK(st.st_mode)}}
    val isReg get()= memScoped { alloc<stat>().let{st ->fstat(fd,st.ptr) ;  S_ISREG(st.st_mode)}}
    val isFifo get()= memScoped { alloc<stat>().let{st->fstat(fd,st.ptr)  ; S_ISFIFO(st.st_mode)}}
    val isLnk get()= memScoped { alloc<stat>().let{st ->fstat(fd,st.ptr) ;  S_ISLNK(st.st_mode)}}




companion object{

    fun __S_ISTYPE(mode: __mode_t, mask: Int) = mode.toInt() and __S_IFMT == mask
    fun S_ISDIR(mode: __mode_t) = __S_ISTYPE((mode), __S_IFDIR)
    fun S_ISCHR(mode: __mode_t) = __S_ISTYPE((mode), __S_IFCHR)
    fun S_ISBLK(mode: __mode_t) = __S_ISTYPE((mode), __S_IFBLK)
    fun S_ISREG(mode: __mode_t) = __S_ISTYPE((mode), __S_IFREG)
    fun S_ISFIFO(mode: __mode_t) = __S_ISTYPE((mode), __S_IFIFO)
    fun S_ISLNK(mode: __mode_t) = __S_ISTYPE((mode), __S_IFLNK)

}

}

