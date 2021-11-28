@file:Suppress("EnumEntryName")

package linux_uring.include

import linux_uring.IOSQE_FIXED_FILE
import linux_uring.*

/**
The flagsfield is a bit mask. The supported flags are:
 */

enum class UringSqeFlags(src:UInt,val flagConstant: UByte=src.toUByte()) {
    /** When this flag is specified, fd is an index into the files array registered with the io_uring instance (see
     * the `IORING_REGISTER_FILES` section of the io_uring_register(2) man page). Note that this isn't always available
     * for all command s. If used on a command that doesn't support fixed files, the `SQE` will error with-EBADF.
     * Available since 5.1. */
    sqeFixed_file(IOSQE_FIXED_FILE),

    /** When this flag is specified, the `SQE` will not be started before previously submitted SQEs have completed, and new
     * SQEs will not be started before this one completes.
     * Available since 5.2. */
    sqeIo_drain(IOSQE_IO_DRAIN),

    /** When this flag is specified, it forms a link with the next `SQE` in the submission ring. That next `SQE` will not be
     *  started before this one completes.This, in effect, forms a chain of SQEs, which can be arbitrarily long. The
     *  tail of the chain is denoted by the first `SQE` that does not have this flag set.This flag has no effect on
     *  previous `SQE` subm issions, nor does it impact SQEsthat are outside of the chain tail. This means that multiple
     *  chains can beexecuting in parallel, or chains and individual SQEs. Only members inside the chain are serialized.
     *  A chain of SQEs will be broken, if any request in thatchain ends in error. io_uring considers any unexpected
     *  result an error. Thismeans that, eg, a short read will also terminate the remainder of the chain.If a chain of
     *  `SQE` links is broken, the remaining unstarted part of the chainwill be terminated and completed with-ECANCELED
     *  as the error code.
     *  Available since 5.3. */
    sqeIo_link(IOSQE_IO_LINK),

    /** Like IOSQE_IO_LINK, but it doesn't sever regardless of the completion result.Note that the link will still sever
     *  if we fail submitting the parent request,hard links are only resilient in the presence of completion results for
     *  requests that did submit correctly. `IOSQE_IO_HARDLINK` implies IOSQE_IO_LINK.Available since 5.5. */
    sqeIo_hardlink(IOSQE_IO_HARDLINK),

    /** Normal operation for io_uring is to try and issue an sqe as non-blocking first,and if that fails, execute it in
     * an async manner. To support more efficient overlapped operation of requests that the application knows/assumes
     * will always (or most of the time) block, the application can ask for an sqe to be issued async from the start.
     * Available since 5.6. */
    sqeAsync(IOSQE_ASYNC),

    /** Used in conjunction with the `IORING_OP_PROVIDE_BUFFERS` command, which reg isters a pool of buffers to be used by
     *  commands that reador receive data. When buffers are registered for this use case, and this flag is set in the
     *  command, io_uring will grab a buffer from this pool whenthe request is ready to receive or read data. If
     *  successful, the resulting `CQE` will have `IORING_CQE_F_BUFFER` set in the flags part of the struct, and the upper
     *  `IORING_CQE_BUFFER_SHIFT` bits will contain the ID of the selected buffers. This allows the applicationto know
     *  exactly which buffer was selected for the operation. If no buffers are available and this flag is set, then the
     *  request will fail with `-ENOBUFS` as the error code. Once a buffer has been used, it is no longer available in
     *  the kernel pool. The application must re-register the given buffer again when it is ready to recycle it (eg has
     *  completed using it).
     *  Available since 5.7. */
    sqeBuffer_select(IOSQE_BUFFER_SELECT),
}
