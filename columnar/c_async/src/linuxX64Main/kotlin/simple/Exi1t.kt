package simple

import kotlinx.cinterop.*
import uring.io_uring_cqe
import uring.request

fun selectorKeyAttachment(
completion_queue_entry_ptr:CPointer<io_uring_cqe>): CPointer<request>? =
    completion_queue_entry_ptr.pointed.user_data.toLong().toCPointer<request>()

