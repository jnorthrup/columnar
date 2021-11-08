
import interop.io_uring_cqe
import interop.request
import kotlinx.cinterop.*

fun selectorKeyAttachment(
completion_queue_entry_ptr:CPointer<io_uring_cqe>): CPointer<request>? =
    completion_queue_entry_ptr.pointed.user_data.toLong().toCPointer<request>()

