package linux_uring.include

import linux_uring.IORING_ENTER_GETEVENTS
import linux_uring.IORING_ENTER_SQ_WAIT
import linux_uring.IORING_ENTER_SQ_WAKEUP

/**
 * If the io_uring instance was configured for polling, by specifying IORING_SETUP_IOPOLL in the call to io_uring_setup(2),
 * then min_complete has a slightly different meaning.  Passing a value of 0 instructs the kernel to return any events
 * which are already complete, without blocking.  If min_complete is a non-zero value, the kernel will still return
 * immediately if any completion events are available.  If no event completions are available, then the call will poll
 * either until one or more completions become available, or until the process has exceeded its scheduler time slice.
 *
 * Note that, for interrupt driven I/O (where IORING_SETUP_IOPOLL was not specified in the call to io_uring_setup(2)),
 * an application may check the completion queue for event completions without entering the kernel at all.
 *
 * When the system call returns that a certain amount of SQEs have been consumed and submitted, it's safe to reuse SQE
 * entries in the ring. This is true even if the actual IO submission had to be punted to async context, which means
 * that the SQE may in fact not have been submitted yet. If the kernel requires later use of a particular SQE entry, it
 * will have made a private copy of it.
 */
enum class UringEnter(public val modeFlag: UInt) {/**If this flag is set, then the system call will wait for the specificied number of events in min_complete before
returning. This flag can be set along with to_submit to both submit and complete events in a single system call.
*/ GetEvents (IORING_ENTER_GETEVENTS),
/**If this flag is set, then the system call will wait for the specified number of events in min_complete before
returning. This flag can be set along with to_submit to both submit and complete events in a single system call.
*/ Sq_Wakeup (IORING_ENTER_SQ_WAKEUP),
/**If the ring has been created with IORING_SETUP_SQPOLL, then the application has no real insight into when the SQ kernel
thread has consumed entries from the SQ ring. This can lead to a situation where the application can no longer get a
free SQE entry to submit, without knowing when   one becomes available as the SQ kernel thread consumes them. If the
system call is used with this flag set, then it will wait until at least one entry is free in the SQ ring.
*/ Sq_Wait (IORING_ENTER_SQ_WAIT),
}
