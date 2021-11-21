@file:Suppress("FunctionName")

package linux_uring.util
import kotlinx.cinterop.*
import simple.allocWithFlex
import linux_uring.file_info

fun fileInfo(iovec_count: Int): file_info = nativeHeap.allocWithFlex(file_info::iovecs, iovec_count)
