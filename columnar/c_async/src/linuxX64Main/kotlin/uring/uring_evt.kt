package uring

import kotlinx.cinterop.CPointer
import uring.io_uring_enter as linux_io_uring_enter1


// @formatter:off
sealed class uring_evt(val opConstant: UInt) {
    object OpNop : uring_evt(IORING_OP_NOP)
    object OpReadv : uring_evt(IORING_OP_READV)
    object OpWritev : uring_evt(IORING_OP_WRITEV)
    object OpFsync : uring_evt(IORING_OP_FSYNC)
    object OpReadFixed : uring_evt(IORING_OP_READ_FIXED)
    object OpWriteFixed : uring_evt(IORING_OP_WRITE_FIXED)
    object OpPollAdd : uring_evt(IORING_OP_POLL_ADD)
    object OpPollRemove : uring_evt(IORING_OP_POLL_REMOVE)
    object OpSyncFileRange : uring_evt(IORING_OP_SYNC_FILE_RANGE)
    object OpSendmsg : uring_evt(IORING_OP_SENDMSG)
    object OpRecvmsg : uring_evt(IORING_OP_RECVMSG)
    object OpTimeout : uring_evt(IORING_OP_TIMEOUT)
    object OpTimeoutRemove : uring_evt(IORING_OP_TIMEOUT_REMOVE)
    object OpAccept : uring_evt(IORING_OP_ACCEPT)
    object OpAsyncCancel : uring_evt(IORING_OP_ASYNC_CANCEL)
    object OpLinkTimeout : uring_evt(IORING_OP_LINK_TIMEOUT)
    object OpConnect : uring_evt(IORING_OP_CONNECT)
    object OpFallocate : uring_evt(IORING_OP_FALLOCATE)
    object OpOpenat : uring_evt(IORING_OP_OPENAT)
    object OpClose : uring_evt(IORING_OP_CLOSE)
    object OpFilesUpdate : uring_evt(IORING_OP_FILES_UPDATE)
    object OpStatx : uring_evt(IORING_OP_STATX)
    object OpRead : uring_evt(IORING_OP_READ)
    object OpWrite : uring_evt(IORING_OP_WRITE)
    object OpFadvise : uring_evt(IORING_OP_FADVISE)
    object OpMadvise : uring_evt(IORING_OP_MADVISE)
    object OpSend : uring_evt(IORING_OP_SEND)
    object OpRecv : uring_evt(IORING_OP_RECV)
    object OpOpenat2 : uring_evt(IORING_OP_OPENAT2)
    object OpEpollCtl : uring_evt(IORING_OP_EPOLL_CTL)
    object OpSplice : uring_evt(IORING_OP_SPLICE)
    object OpProvideBuffers : uring_evt(IORING_OP_PROVIDE_BUFFERS)
    object OpRemoveBuffers : uring_evt(IORING_OP_REMOVE_BUFFERS)
    object OpTee : uring_evt(IORING_OP_TEE)
    object OpShutdown : uring_evt(IORING_OP_SHUTDOWN)
    object OpRenameat : uring_evt(IORING_OP_RENAMEAT)
    object OpUnlinkat : uring_evt(IORING_OP_UNLINKAT)
    object OpLast : uring_evt(IORING_OP_LAST)
}

//@formatter:on
/**
 *  io_uring_enter(2) flags
 */
enum class uring_enter(val callConstant: UInt) {


    /**If this flag is set, then the system call will wait for the specificied number
     * of events in min_complete before returning. This flag can be set along with
     * to_submit to both submit and complete events in a single system call.
     */
    IoRingEnterGetEvents(IORING_ENTER_GETEVENTS),

    /**If the ring has been created with IORING_SETUP_SQPOLL, then this flag asks the
     * kernel to wakeup the SQ kernel thread to submit IO.
     */
    IoRingEnterSqWakeup(IORING_ENTER_SQ_WAKEUP),

    /**If the ring has been created with IORING_SETUP_SQPOLL, then the application has
     *  no real insight into when the SQ kernel thread has consumed entries from the
     *  SQ ring. This can lead to a situation where the application can no longer get a
     *  free SQE entry to submit, without knowing when it one becomes available as the
     *  SQ kernel thread consumes them. If the system call is used with this flag set,
     *  then it will wait until at least one entry is free in the SQ ring.*/
    IoRingEnterSqWait(IORING_ENTER_SQ_WAIT),

    IoRingEnterExtArg(IORING_ENTER_EXT_ARG),
    ;

    fun io_uring_enter(
        fd: UInt,
        to_submit: UInt,
        min_complete: UInt,

        sig: CPointer<sigset_t>
    ) = linux_io_uring_enter1(fd.toInt(), to_submit, min_complete, callConstant/*, sig*/)
}