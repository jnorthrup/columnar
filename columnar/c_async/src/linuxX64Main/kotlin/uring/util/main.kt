@file:Suppress("FunctionName")

package uring.util
import kotlinx.cinterop.*
import platform.posix.__S_IFBLK
import platform.posix.__S_IFCHR
import platform.posix.__S_IFDIR
import platform.posix.__S_IFIFO
import platform.posix.__S_IFLNK
import platform.posix.__S_IFMT
import platform.posix.__S_IFREG
import uring.file_info
import kotlin.reflect.KProperty1

fun __S_ISTYPE(mode: Int, mask: Int) = (((mode) and __S_IFMT) == (mask))
fun S_ISDIR(mode: Int) = __S_ISTYPE((mode), __S_IFDIR)
fun S_ISCHR(mode: Int) = __S_ISTYPE((mode), __S_IFCHR)
fun S_ISBLK(mode: Int) = __S_ISTYPE((mode), __S_IFBLK)
fun S_ISREG(mode: Int) = __S_ISTYPE((mode), __S_IFREG)
fun S_ISFIFO(mode: Int) = __S_ISTYPE((mode), __S_IFIFO)
fun S_ISLNK(mode: Int) = __S_ISTYPE((mode), __S_IFLNK)


inline fun <reified A : CStructVar, reified B : CVariable> NativePlacement.allocWithFlex(
    bProperty: KProperty1<A, CPointer<B>>,
    count: Int,
): A = alloc(sizeOf<A>() + sizeOf<B>() * count, alignOf<A>()).reinterpret()

fun fileInfo(iovec_count: Int): file_info = nativeHeap.allocWithFlex(file_info::iovecs, iovec_count)
