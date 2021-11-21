@file:Suppress("EnumEntryName")

package linux_uring.include

import linux_uring.*

/**
If no flags are specified, the io_uring instance is setup for interrupt driven I/O. I/O may be submitted using
io_uring_enter(2) and can be reaped by polling the completion queue.

The resv array must be initialized to zero.

features is filled in by the kernel, which specifies various features supported by current kernel version.*/
enum class UringSetupFeatures(val feat_const: UInt) {
    /**
     * If this flag is set, the two SQ and CQ rings can be mapped with a single mmap(2) call. The SQEs must still
     * be allocated separately. This brings the necessary mmap(2) calls down from three to two. Available since
     * kernel 5.4.
     */
    featSingle_mmap(IORING_FEAT_SINGLE_MMAP),

    /**If this flag is set, io_uring supports never dropping completion events. If a completion event occurs and
     * the CQ ring is full, the kernel stores the event internally until such a time that the CQ ring has room for
     * more entries. If this overflow condition is entered, attempting to submit more IO will fail with the `-EBUSY`
     * error value, if it can't flush the overflown events to the CQ ring. If this happens, the application must
     * reap events from the CQ ring and attempt the submit again. Available since kernel 5.5.
     */
    featNodrop(IORING_FEAT_NODROP),

    /**If this flag is set, applications can be certain that any data for async offload has been consumed when
     * the kernel has consumed the SQE. Available since kernel 5.5.
     */
    featSubmit_stable(IORING_FEAT_SUBMIT_STABLE),

    /**If this flag is set, applications can specify offset == -1 with IORING_OP_{READV,WRITEV} ,
     * IORING_OP_{READ,WRITE}_FIXED , and IORING_OP_{READ,WRITE} to mean current file position, which behaves like
     * preadv2(2) and pwritev2(2) with offset == -1. It'll use (and update) the current file position. This obviously
     * comes with the caveat that if the application has multiple reads or writes in flight, then the end result will
     * not be as expected. This is similar to threads sharing a file descriptor and doing IO using the current file
     * position. Available since kernel 5.6.
     */
    featRw_cur_pos(IORING_FEAT_RW_CUR_POS),

    /**   If this flag is set, then io_uring guarantees that both sync and async execution of a request assumes the
     *  credentials of the task that called io_uring_enter(2) to queue the requests. If this flag isn't set, then
     *  requests are issued with the credentials of the task that originally registered the io_uring. If only one task
     *  is using a ring, then this flag doesn't matter as the credentials will always be the same. Note that this is
     *  the default behavior, tasks can still register different personalities through io_uring_register(2) with
     *  `IORING_REGISTER_PERSONALITY` and specify the personality to use in the sqe. Available since kernel 5.6.
     */
    featCur_personality(IORING_FEAT_CUR_PERSONALITY),

    /** If this flag is set, then io_uring supports using an internal poll mechanism to drive data/space readiness. This
     *  means that requests that cannot read or write data to a file no longer need to be punted to an async thread for
     *  handling, instead they will begin operation when the file is ready. This is similar to doing poll + read/write
     *  in userspace, but eliminates the need to do so. If this flag is set, requests waiting on space/data consume a
     *  lot less resources doing so as they are not blocking a thread. Available since kernel 5.7.
     */
    featFast_poll(IORING_FEAT_FAST_POLL),

    /**    If this flag is set, the `IORING_OP_POLL_ADD` command accepts the full 32-bit range of epoll based flags. Most
     * notably `EPOLLEXCLUSIVE` which allows exclusive (waking single waiters) behavior. Available since kernel 5.9.
     */
    featPoll_32bits(IORING_FEAT_POLL_32BITS),

    /** If this flag is set, the `IORING_SETUP_SQPOLL` feature no longer requires the use of fixed files. Any normal file
     *  descriptor can be used for IO commands without needing registration. Available since kernel 5.11.
     */
    featSqpoll_nonfixed(IORING_FEAT_SQPOLL_NONFIXED),

    /** If this flag is set, then the io_uring_enter(2) system call supports passing in an extended argument instead of
     * just the sigset_t of earlier kernels. This. extended argument is of type struct io_uring_getevents_arg and
     * allows the caller to pass in both a sigset_t and a timeout argument for waiting on events. The struct layout
     * is as follows:
    ```
    struct io_uring_getevents_arg {
    __u64 sigmask;
    __u32 sigmask_sz;
    __u32 pad;
    __u64 ts;
    };
    ```
    and a pointer to this struct must be passed in if `IORING_ENTER_EXT_ARG` is set in the flags for the enter system call.
    Available since kernel 5.11.            */
    featEnter_ext_arg(IORING_FEAT_EXT_ARG),

    /** If this flag is set, io_uring is using native workers for its async helpers. Previous kernels used kernel
     *  threads that assumed the identity of the original io_uring owning task, but later kernels will actively create
     *  what looks more like regular process threads instead. Available since kernel 5.12.             */
    featNative_workers(IORING_FEAT_NATIVE_WORKERS),

    /**
     * If this flag is set, then io_uring supports a variety of features related to fixed files and buffers. In
     * particular, it indicates that registered buffers can be updated in -place, whereas before the full set would
     * have to be unregistered first. Available since kernel 5.13. */
    featRsrc_tags(IORING_FEAT_RSRC_TAGS),

}
