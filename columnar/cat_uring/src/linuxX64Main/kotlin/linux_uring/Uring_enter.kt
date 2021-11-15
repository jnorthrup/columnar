//package uring
//
//import kotlinx.cinterop.CPointer
//
///**
// *  io_uring_enter(2) flags
// */
//enum class uring_enter(val callConstant: UInt) {
//
//
//    /**If this flag is set, then the system call will wait for the specificied number
//     * of events in min_complete before returning. This flag can be set along with
//     * to_submit to both submit and complete events in a single system call.
//     */
//    IoRingEnterGetEvents(IORING_ENTER_GETEVENTS),
//
//    /**If the ring has been created with IORING_SETUP_SQPOLL, then this flag asks the
//     * kernel to wakeup the SQ kernel thread to submit IO.
//     */
//    IoRingEnterSqWakeup(IORING_ENTER_SQ_WAKEUP),
//
//    /**If the ring has been created with IORING_SETUP_SQPOLL, then the application has
//     *  no real insight into when the SQ kernel thread has consumed entries from the
//     *  SQ ring. This can lead to a situation where the application can no longer get a
//     *  free SQE entry to submit, without knowing when it one becomes available as the
//     *  SQ kernel thread consumes them. If the system call is used with this flag set,
//     *  then it will wait until at least one entry is free in the SQ ring.*/
//    IoRingEnterSqWait(IORING_ENTER_SQ_WAIT),
//
//    IoRingEnterExtArg(IORING_ENTER_EXT_ARG),
//    ;
//
//    fun io_uring_enter(
//        fd: UInt,
//        to_submit: UInt,
//        min_complete: UInt,
//
//        sig: CPointer<sigset_t>
//    ) = io_uring_enter(fd.toInt(), to_submit, min_complete, callConstant/*, sig*/)
//}