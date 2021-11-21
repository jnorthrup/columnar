@file:Suppress("EnumEntryName")
package linux_uring.include

import linux_uring.*

/**
 * The io_uring_setup() system call sets up a submission queue (SQ) and completion queue (CQ) with at least entries
 * entries, and returns a file descriptor which can be used to perform subsequent operations on the io_uring instance.
 * The submission and completion queues are shared between userspace and the kernel, which eliminates the need to copy
 * data when initiating and completing I/O.
 *  params is used by the application to pass options to the kernel, and by the kernel to convey information about the
 *  ring buffers.
 *
 *  ```
 *  struct io_uring_params {
 *  __u32 sq_entries;
 *  __u32 cq_entries;
 *  __u32 flags;
 *  __u32 sq_thread_cpu;
 *  __u32 sq_thread_idle;
 *  __u32 features;
 *  __u32 resv[4];
 *  struct io_sqring_offsets sq_off;
 *  struct io_cqring_offsets cq_off;
 *  };
 *  ```
 *  The flags, sq_thread_cpu, and sq_thread_idle fields are used to configure the io_uring instance. flags is a bit
 *  mask of 0 or more of the following values ORed together:
 */
enum class UringSetupFlags(flag_const: UInt) {

    /**    Perform busy-waiting for an I/O completion, as opposed to getting notifications via an asynchronous `IRQ`
     *  (Interrupt Request). The file system (if any) and block device must support polling in order for this to work.
     *  Busy-waiting provides lower latency, but may consume more `CPU` resources than interrupt driven I/O. Currently,
     *  this feature is usable only on a file descriptor opened using the `O_DIRECT` flag. When a read or write is
     *  submitted to a polled context, the application must poll for completions on the CQ ring by calling
     *  io_uring_enter(2). It is illegal to mix and match polled and non-polled I/O on an io_uring instance.
     */
    uringSetupIopoll(IORING_SETUP_IOPOLL),

    /**    When this flag is specified, a kernel thread is created to perform submission queue polling. An io_uring
     * instance configured in this way enables an application to issue I/O without ever context switching into the kernel.
     * By using the submission queue to fill in new submission queue entries and watching for completions on the completion
     * queue, the application can submit and reap I/Os without doing a single system call.
     * If the kernel thread is idle for more than sq_thread_idle milliseconds, it will set the `IORING_SQ_NEED_WAKEUP` bit
     * in the flags field of the struct io_sq_ring. When this happens, the application must call io_uring_enter(2) to
     * wake the kernel thread. If I/O is kept busy, the kernel thread will never sleep. An application making use of this
     * feature will need to guard the io_uring_enter(2) call with the following code sequence:
    ```
    /*
     * Ensure that the wakeup flag is read after the tail pointer
     * has been written. It's important to use memory load acquire
     * semantics for the flags read, as otherwise the application
     * and the kernel might not agree on the consistency of the
     * wakeup flag.
    */

    unsigned flags = atomic_load_relaxed(sq_ring->flags);
    if (flags & IORING_SQ_NEED_WAKEUP)
    io_uring_enter(fd, 0, 0, IORING_ENTER_SQ_WAKEUP);
    ```
    where sq_ring is a submission queue ring setup using the struct io_sqring_offsets described below.

    Before version 5.11 of the Linux kernel, to successfully use this feature, the application must register a set of
    files to be used for IO through io_uring_register(2) using the `IORING_REGISTER_FILES` opcode. Failure to do so will
    result in submitted IO being errored with EBADF. The presence of this feature can be detected by the
    `IORING_FEAT_SQPOLL_NONFIXED` feature flag. In version 5.11 and later, it is no longer necessary to register files
    to use this feature. 5.11 also allows using this as non-root, if the user has the `CAP_SYS_NICE` capability.
     */
    uringSetupSqpoll(IORING_SETUP_SQPOLL),

    /**     If this flag is specified, then the poll thread will be bound to the cpu set in the sq_thread_cpu field of the
     *  struct io_uring_params. This flag is only meaningful when `IORING_SETUP_SQPOLL` is specified. When cgroup setting
     *  cpuset.cpus changes (typically in container environment), the bounded cpu set may be changed as well.
     */
    uringSetupSq_aff(IORING_SETUP_SQ_AFF),

    /**  Create the completion queue with struct io_uring_params.cq_entries entries. The value must be greater than
     * entries, and may be rounded up to the next power-of-two.
     */
    uringSetupCqsize(IORING_SETUP_CQSIZE),

    /**  If this flag is specified, and if entries exceeds `IORING_MAX_ENTRIES` , then entries will be clamped at
     * `IORING_MAX_ENTRIES` . If the flag `IORING_SETUP_SQPOLL` is set, and if the value of struct
     * io_uring_params.cq_entries exceeds `IORING_MAX_CQ_ENTRIES` , then it will be clamped at `IORING_MAX_CQ_ENTRIES` .
     */
    uringSetupClamp(IORING_SETUP_CLAMP),

    /**    This flag should be set in conjunction with struct  io_uring_params.wq_fd being set to an existing io_uring
     * ring file descriptor. When set, the io_uring instance being created will share the asynchronous worker thread
     * backend of the specified io_uring ring, rather than create a new separate thread pool.
     */
    uringSetupAttach_wq(IORING_SETUP_ATTACH_WQ),

    /**   If this flag is specified, the io_uring ring starts in a disabled state. In this state, restrictions can
     *  be registered, but submissions are not allowed. See io_uring_register(2) for details on how to enable the
     *  ring. Available since 5.10.
     */
    uringSetupR_disabled(IORING_SETUP_R_DISABLED),
}
