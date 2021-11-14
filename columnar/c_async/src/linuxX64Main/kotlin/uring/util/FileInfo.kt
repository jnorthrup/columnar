@file:Suppress("FunctionName")

package uring.util
import kotlinx.cinterop.*
import simple.allocWithFlex
import uring.file_info

fun fileInfo(iovec_count: Int): file_info = nativeHeap.allocWithFlex(file_info::iovecs, iovec_count)
