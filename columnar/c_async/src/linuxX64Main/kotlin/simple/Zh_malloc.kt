package simple

import kotlinx.cinterop.COpaquePointer
import platform.posix.*

/**
 * Helper function for cleaner looking code.
 * */

    fun zh_malloc( size: size_t): COpaquePointer? {
    val     buf: COpaquePointer? = malloc(size.toULong() as size_t /* = kotlin.ULong */)
        if (buf != NULL)
            return buf
    fprintf(stderr, "Fatal error: unable to allocate memory: %d\n", size)
        return NULL.also { platform.posix.exit(1) }
    }