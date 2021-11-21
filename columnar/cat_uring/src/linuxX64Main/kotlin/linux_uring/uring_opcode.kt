package linux_uring

/**
 * #The opcode
 * describes the operation to be performed. It can be one of the enum
 *
 * #RETURNVALUE
 * io_uring_enter() returns the number of I/Os successfully consumed. This can be zero if to_submit was zero or if the
 * subm ission queue was empty. Note that if the ring was created with `IORING_SETUP_SQPOLL` specified, the n the return
 * value will generally be the same asto_submit as subm ission happens outside the context of the system call. The errors
 * related to a subm ission queue entry will be returned through acompletion queue entry (see sectionCQE ERRORS), rather
 * than through the system call itself. Errors that occur not on behalf of a subm ission queue entry are returned via the
 * system call directly. On such an error, -1 is returned and errno is set appropriately.
 */

enum class uring_opcode(val opConstant: UInt) {

    /**    Do not perform any I/O. This is useful for testing the performance of
     * the io_uring implementation itself. */
    Op_Nop(IORING_OP_NOP),

    /**    Vectored read and write operations, similar topreadv2(2)and pwritev2(2).If the file is not seekable,off
     * must be set to zero. */
    Op_Readv(IORING_OP_READV),
    Op_Writev(IORING_OP_WRITEV),

    /**    Read from or write to pre-mapped buffers. Seeio_uring_reg ister(2)for details on how to setup a context for
     *  fixed reads and writes. */
    Op_Read_fixed(IORING_OP_READ_FIXED),
    Op_Write_fixed(IORING_OP_WRITE_FIXED),

    /**    File sync. See alsofsync(2).Note that, while I/O is initiated in the order in which it appears in
     * the subm ission queue, completions are unordered. For example, anapplication which places a write I/O followed
     * by an fsync in the subm ission queue cannot expect the fsync to apply to the write. the two operations execute
     * in parallel, so the fsync may complete beforethe write is issued to the storage. The same is also true for
     * previously issued writes that have not completed prior to the fsync. */
    Op_Fsync(IORING_OP_FSYNC),

    /** Poll the fdspecified in the subm ission queue entry for the eventsspecified in thepoll_eventsfield. Unlike
     * poll
     * or epoll withoutEPOLLONESHOT,by default this interface always works in one shot mode. That is, once the
     * polloperation is completed, it will have to be resubmitted.
     * If `IORING_POLL_ADD_MULTI` is set in the SQElenfield, the n the poll will work in multi shot mode instead. That
     * means it'llrepatedly trigger when the requested event becomes true, and hence multipleCQEs can be generated from
     * this single SQE. The CQEflagsfield will haveIORING_CQE_F_MOREset on completion if the application should expect
     * further `CQE` entries fromthe original request. If this flag isn't set on completion, the n the pollrequest has
     * been terminated and no furthe r events will be generated. This mode is available since 5.13.
     *
     *  If `IORING_POLL_UPDATE_EVENTS` is set in the SQElenfield, the n the request will update an ex isting poll request
     *  with the mask ofevents passed in with this request. The lookup is based on the user_datafield of the original `SQE`
     *  submitted, and this values is passed in the addrfield of the SQE. This mode is available since 5.13.
     *  If `IORING_POLL_UPDATE_USER_DATA` is set in the SQElenfield, the n the request will update the user_dataof an
     *  existing poll request based on the value passed in theofffield. This mode is available since 5.13.
     *
     *  This command works like an async poll(2) and the completion event result is the returned mask of events. For the
     *  variants that update user_data or events, the completion result will be similar to `IORING_OP_POLL_REMOVE` .
     */
    Op_Poll_add(IORING_OP_POLL_ADD),

    /**
    Remove an ex isting poll request. If found, the resfield of the struct io_uring_cqewill contain 0. If not found,res
    will contain `-ENOENT` ,or `-EALREADY` if the poll request was in the process of completing already.
     */

    Op_Poll_remove(IORING_OP_POLL_REMOVE),

    /** Add, remove or modify entries in the interest l ist ofepoll(7).Seeepoll_ctl(2)for details of the system call.fd
     * holds the file descriptor that represents the epoll instance,addrholds the file descriptor to add, remove or
     * modify,len holds the operation ( `EPOLL_CTL_ADD` , `EPOLL_CTL_DEL` , `EPOLL_CTL_MOD` ) to perform and,off holds
     * a pointer to the epoll_events structure. Available since 5.6. */
    Op_Epoll_ctl(IORING_OP_EPOLL_CTL),

    /**
    Issue the equivalent of a sync_file_range (2) on the file descriptor. The fdfield is the file descriptor to sync,
    the offfield holds the offset in bytes, the lenfield holds the length in bytes, and the sync_range_flagsfield holds
    the flags for the command. See alsosync_file_range(2)for the general description of the related system call.
    Available since 5.2.
     */
    Op_Sync_file_range(IORING_OP_SYNC_FILE_RANGE),

    /**        Issue the equivalent of asendmsg(2)system call.fd mustbe set to the socket file descriptor,addr must
     *  contain a pointer to the msghdr structure, and msg_flags holds the flags associated with the system call. See
     *  also sendmsg(2) for the general description of the related system call. Available since 5.3.
     */
    Op_Sendmsg(IORING_OP_SENDMSG),

    /**        Works just like IORING_OP_SENDMSG, except forrecvmsg(2)instead. See the description of `IORING_OP_SENDMSG` .
     *  Available since 5.3.
     */
    Op_Recvmsg(IORING_OP_RECVMSG),

    /**        Issue the equivalent of asend(2)system call.fd mustbe set to the socket file descriptor,addr must
     * contain a pointer to the buffer,len denotes the length of the buffer to send, and msg_flagsholds the flags
     * associated with the system call. See also send(2) for the general description of the related system call.
     * Available since 5.6.
     */
    Op_Send(IORING_OP_SEND),

    /**        Works just like `IORING_OP_SEND` , except forrecv(2)instead. See the description of `IORING_OP_SEND` .
     * Available since 5.6.
     */
    Op_Recv(IORING_OP_RECV),

    /**        This command will reg ister a timeout operation. the addrfield must contain a pointer to a struct
     * timespec64 structure,len must contain 1 to signify one timespec64 structure,timeout_flagsmay contain
     * `IORING_TIMEOUT_ABS` for an absolute timeout value, or 0 for a relative timeout.offmay contain a completion event
     * count. A timeoutwill trigger a wakeup event on the completion ring for anyone waiting forevents. A timeout
     * condition is met when eithe r the specified timeout expires,or the specified number of events have completed.
     * Either condition willtrigger the event. If set to 0, completed events are not counted, whicheffectively acts
     * like a timer. io_uring timeouts use the CLOCK_MONOTONICclock source. The request will complete with `-ETIME` if
     * the timeout got completed through expiration of the timer, or0if the timeout got completed through requests
     * completing on the ir own. Ifthe timeout was cancelled before it expired, the request will complete with
     * `-ECANCELED` .Available since 5.4.
    Since 5.15, this command also supports the following modifiers intimeout_flags:
     */
    Op_Timeout(IORING_OP_TIMEOUT),


    /**If set, the n the clocksource used is `CLOCK_BOOTTIME` instead of CLOCK_MONOTONIC.This clocksource differs in
     * that it includes time elapsed if the system was suspend while having a timeout request in-flight.*/
    TiMEout_boottime(IORING_TIMEOUT_BOOTTIME),

    /** If set, the n the clocksource used is `CLOCK_BOOTTIME` instead of CLOCK_MONOTONIC.*/
    TiMEout_realtime(IORING_TIMEOUT_REALTIME),

    /**If timeout_flags are zero, the n it attempts to remove an existing timeout operation. addr must contain the
     * user_datafield of the previously issued timeout operation. If the specified timeoutrequest is found and cancelled
     * successfully, this request will terminatewith a result value of 0 If the timeout request was found but expiration
     * was already in progress,this request will terminate with a result value of `-EBUSY`
     *
     * If the timeout request wasn't found, the request will terminate with a resultvalue of `-ENOENT`
     * Available since 5.5.
     *
     * If timeout_flags contain `IORING_TIMEOUT_UPDATE` , instead of removing an existing operation,
     * it updates it.addr and return values are same as before.addr2field must contain a pointer to a struct timespec64
     * structure.timeout_flags may also contain IORING_TIMEOUT_ABS, in which case the value given is anabsolute one,
     * not a relative one.
     * Available since 5.11.
     */
    Op_Timeout_remove(IORING_OP_TIMEOUT_REMOVE),

    /**        Issue the equivalent of an accept4(2) system call. fd must be set to the socket file descriptor,addr must
     *  contain the pointer to the sockaddr structure, and addr2 must contain a pointer to the socklen_t addrlen field.
     *  Flags can be passed using  the accept_flagsfield. See also accept4(2) for the general description of the related
     *  system call. Available since 5.5.
    If the file_indexfield is set to a positive number, the file won't be installed into the normal file table as usual
    but will be placed into the fixed file table at indexfile_index - 1.In this case, instead of returning a file
    descriptor, the result will containeithe r 0 on success or an error. If the re is already a file reg istered at
    thisindex, the request will fail with `-EBADF` .Only io_uring has access to such files and no othe r syscall can use
    them. See `IOSQE_FIXED_FILE` and `IORING_REGISTER_FILES` .

    Available since 5.15.
     */
    Op_Accept(IORING_OP_ACCEPT),

    /**        Attempt to cancel an already issued request.addr must contain the user_datafield of the request that
     *  should be cancelled. The cancellation request willcomplete with one of the following results codes. If found,
     *  the resfield of the cqe will contain 0. If not found,reswill contain -ENOENT. If found and attempted cancelled,
     *  the resfield will contain -EALREADY. In this case, the request may or may notterminate. In general, requests
     *  that are interruptible (like socket IO) willget cancelled, while d isk IO requests cannot be cancelled if
     *  already started.Available since 5.5.
     */
    Op_Async_cancel(IORING_OP_ASYNC_CANCEL),

    /**        This request must be linked with anothe r request throughIOSQE_IO_LINKwhich is described below. Unlike
     * IORING_OP_TIMEOUT,IORING_OP_LINK_TIMEOUTacts on the linked request, not the completion queue. The format of the
     * command is othe rw ise likeIORING_OP_TIMEOUT,except there's no completion event count as it's tied to a specific
     * request.If used, the timeout specified in the command will cancel the linked command,unless the linked command
     * completes before the timeout. The timeout willcomplete with-ETIMEif the timer expired and the linked request was
     * attempted cancelled, or `-ECANCELED` if the timer got cancelled because of completion of the linked request. Like
     * `IORING_OP_TIMEOUT` the clock source used isCLOCK_MONOTONICAvailable since 5.5.
     */
    Op_Link_timeout(IORING_OP_LINK_TIMEOUT),

    /**        Issue the equivalent of aconnect(2) system call.fd mustbe set to the socket file descriptor,addr must
     * contain the const pointer to the sockaddr structure, and off must contain the socklen_t addrlen field. See also
     * connect(2) for the general description of the related system call.
     * Available since 5.5.
     */
    Op_Connect(IORING_OP_CONNECT),

    /**        Issue the equivalent of a fallocate(2) system call.fd mustbe set to the file descriptor,len must contain
     * the mode associated with the operation,off must contain the offset on which to operate, and addr must contain
     * the length. See also fallocate(2) for the general description of the related system call.
     * Available since 5.6.
     */
    Op_Fallocate(IORING_OP_FALLOCATE),

    /** Issue the equivalent of aposix_fadv ise(2)system call.fd mustbe set to the file descriptor,off
     * must contain the offset on which to operate,len must contain the length, and fadv ise_advice must contain the
     * advice associated with the operation. See alsoposix_fadv ise(2) for the general description of the related system
     * call. Available since 5.6.
     */
    Op_Fadvise(IORING_OP_FADVISE),

    /** Issue the equivalent of a madvise(2) system call.addr must contain the address to operate on,len
     * must contain the length on which to operate,and fadvise_advice must contain the advice associated with
     * the operation. See also madvise(2) for the general description of the related system call.
     * Available since 5.6.
     */
    Op_Madvise(IORING_OP_MADVISE),

    /** Issue the equivalent of a openat(2) system call.fd is the dirfd argument, addr must contain a pointer to the
     *  pathname argument,open_flags should contain any flags passed in, and len is access mode of the file. See also
     * openat(2) for the general description of the related system call.
     * Available since 5.6.
     *
     * If the file_index field is set to a positive number, the file won't be installed into the normal file table as
     * usual but will be placed into the fixed file table at index `file_index-1`.
     *
     * In this case, instead of returning a file descriptor, the result will contain either 0 on success or an error.
     * If there is already a file registered at this index, the request will fail with-EBADF. Only io_uring has access
     * to such files and no other syscall can use them. See `IOSQE_FIXED_FILE` and IORING_REGISTER_FILES.
     * Available since 5.15.
     */
    Op_Openat(IORING_OP_OPENAT),

    /**        Issue the equivalent of aopenat2(2)system call.fd is the dirfd argument,addr must contain a pointer to
     * the *pathname argument,len should contain the size of the open_how structure, and off should be set to the
     * address of the open_how structure. See alsoopenat2(2)for the general description of the related system call.
     * Available since 5.6.
     *
     * If the file_indexfield is set to a positive number, the file won't be installed into the normal file table as usual
     * but will be placed into the fixed file table at indexfile_index - 1.In this case, instead of returning a file
     * descriptor, the result will containeithe r 0 on success or an error. If the re is already a file reg istered at
     * thisindex, the request will fail with-EBADF.Only io_uring has access to such files and no othe r syscall can use
     * them. See `IOSQE_FIXED_FILE` and `IORING_REGISTER_FILES`      * Available since 5.15.
     */
    Op_Openat2(IORING_OP_OPENAT2),

    /** Issue the equivalent of a close(2) system call.fd is the file descriptor to be closed. See also close(2) for
     *  the general description of the related system call.
     *  Available since 5.6.
     */
    Op_Close(IORING_OP_CLOSE),

    /**Issue the equivalent of as tatx(2) system call.fd is the dirfd argument, addr must contain a pointer to the
     * pathname string,statx_flags is the flags argument,len should be the mask argument, and off must contain a pointer to
     * the statxbufto be filled in. See alsostatx(2) for the general description of the related system call.
     * Available since 5.6.
     */
    Op_Statx(IORING_OP_STATX),

    /**        Issue the equivalent of apread(2)orpwrite(2)system call.fd is the file descriptor to be operated on,addr
     * contains the buffer in question,lencontains the length of the IO operation, and offscontains the read or write
     * offset. Iffddoes not refer to a seekable file,off mustbe set to zero. Ifoffs is set to -1, the offset will use
     * (and advance) the file position, like theread(2)and write(2)system calls. the se are non-vectored versions of
     * the IORING_OP_READVand IORING_OP_WRITEVopcodes. See alsoread(2)and write(2)for the general description of the
     * related system call.
     * Available since 5.6.
     */
    Op_Read(IORING_OP_READ),

    /**        Issue the equivalent of a pread(2) or pwrite(2) system call. fd is the file descriptor to be operated on,
     * addr contains the buffer in question,len contains the length of the IO operation, and offs contains the
     * read or write offset. If fd does not refer to a seekable file,off mustbe set to zero. If offs is set to -1,
     * the offset will use (and advance) the file position, like the read(2) and write(2) system calls. these are
     * non-vectored versions of the `IORING_OP_READV` and `IORING_OP_WRITEV` opcodes. See also read(2) and write(2)
     * for the general description of the related system call.
     * Available since 5.6.
     */
    Op_Write(IORING_OP_WRITE),

    /** Issue the equivalent of asplice(2)system call.splice_fd_in is the file descriptor to read
     *  from, splice_off_in is an offset to read from,fd is the file descriptor to write to,off is an offset from which
     *  to start writing to. A sentinel value of -1 is usedto pass the equivalent of a `NULL` for the offsets tosplice(2).
     *  lencontains the number of bytes to copy.splice_flagscontains a bit mask for the flag field associated with the
     *  system call.Please note that one of the file descriptors must refer to a pipe.See alsosplice(2)for the general
     *  description of the related system call.
     *  Available since 5.7.
     */
    Op_Splice(IORING_OP_SPLICE),

    /** Issue the equivalent of atee(2)system call.splice_fd_in is the file descriptor to read from,fd is the file
     *  descriptor to write to,lencontains the number of bytes to copy, and splice_flagscontains a bit mask for the
     *  flag field associated with the system call.Please note that both of the file descriptors must refer to a pipe.
     *  See alsotee(2)for the general description of the related system call.
     *  Available since 5.8.
     */
    Op_Tee(IORING_OP_TEE),

    /**This command is an alternative to usingIORING_REGISTER_FILES_UPDATEwhich the n works in an async fashion,
     *  like the rest of the io_uring command s.The arguments passed in are the same.addr must contain a pointer to the
     *  array of file descriptors,len must contain the length of the array, and off must contain the offset at which to
     *  operate. Note that the array of filedescriptors pointed to inaddr mustremain valid until
     *  this operation has completed. Available since 5.6.
     */
    Op_Files_update(IORING_OP_FILES_UPDATE),

    /**This command allows an application to register a group of buffers to be used by commands that read/receive
     * data. Using buffers in this manner can eliminatethe need to separate the poll + read, which provides a convenient
     * point in time to allocate a buffer for a given request. It's often infeasible to haveas many buffers available as
     * pending reads or receive. With this feature, the application can have its pool of buffers ready in the kernel,
     * and when the file or socket is ready to read/receive data, a buffer can be selected for the operation.fd must
     * contain the number of buffers to provide,addr must contain the starting address to add buffers from,len must
     * contain the length of each buffer to add from the range,buf_group must contain the group ID of this range of
     * buffers, and off must contain the starting buffer ID of this range of buffers. With that set,the kernel adds
     * buffers starting with the memory address inaddr,each with a length of len.Hence the application should provide
     * len * fd worth of memory inaddr.
     *
     * Buffers are grouped by the group ID, and each buffer within this group will be
     * identical in size according to the above arguments. This allows the applicationto provide different groups of
     * buffers, and this is often used to havedifferently sized buffers available depending on what the expectations
     * are ofthe individual request. When submitting a request that should use a providedbuffer, the
     * `IOSQE_BUFFER_SELECT` flag must be set, and buf_group must be set to the desired buffer group ID where
     * the buffer should be selected from.
     * Available since 5.7.
     */
    Op_Provide_buffers(IORING_OP_PROVIDE_BUFFERS),

    /**        Remove buffers previously registered withIORING_OP_PROVIDE_BUFFERS.fd must contain the number of buffers
     *  to remove, and buf_group must contain the buffer group ID from which to remove the buffers. Availablesince 5.7.
     */
    Op_Remove_buffers(IORING_OP_REMOVE_BUFFERS),

    /**        Issue the equivalent of ashutdown(2)system call.fd is the file descriptor to the socket being shutdown,
     * no othe r fields shouldbe set. Available since 5.11.
     */
    Op_Shutdown(IORING_OP_SHUTDOWN),

    /**        Issue the equivalent of arenameat2(2)system call.fdshould be set to the old dirfd,addrshould be set to
     * theoldpath,lenshould be set to thenewdirfd,addrshould be set to theoldpath,addr2 should be set to thenewpath,and
     * finallyrename_flagsshould be set to the flagspassed in torenameat2(2).Available since 5.11.
     */
    Op_Renameat(IORING_OP_RENAMEAT),

    /**        Issue the equivalent of aunlinkat2(2)system call.fdshould be set to thedirfd,addrshould be set to
     *  thepathname,and unlink_flagsshould be set to the flagsbeing passed in tounlinkat(2).Available since 5.11.
     */
    Op_Unlinkat(IORING_OP_UNLINKAT),

    /**        Issue the equivalent of a mkdirat2(2) system call.fd should be set to the dir fd,addr should be set to
     *  the pathname,and len should be set to the mode being passed in to mkdirat(2).
     *  Available since 5.15.
     */
    Op_Mkdirat(IORING_OP_MKDIRAT),

    /** Issue the equivalent of a symlinkat2(2) system call.  fd should be set to the new dir fd,addr should be set to
     *  the target and addr2 should be set to the link path being passed in to symlinkat(2).
     *  Available since 5.15.
     */
    Op_Symlinkat(IORING_OP_SYMLINKAT),

    /**        Issue the equivalent of alinkat2(2) system call.fd should be set to the old dirfd,addr should be set to
     *  theoldpath,lenshould be set to thenewdirfd,addr2should be set to thenewpath,and hardlink_flagsshould be set to
     *  the flagsbeing passed in tolinkat(2).
     *  Available since 5.15.

     */
    Op_Linkat(IORING_OP_LINKAT),
}

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
enum class uring_setup_flags(flag_const: UInt) {

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

/**
If no flags are specified, the io_uring instance is setup for interrupt driven I/O. I/O may be submitted using
io_uring_enter(2) and can be reaped by polling the completion queue.

The resv array must be initialized to zero.

features is filled in by the kernel, which specifies various features supported by current kernel version.*/
enum class uring_setup_features(feat_const: UInt) {
    /**    If this flag is set, the two SQ and CQ rings can be mapped with a single mmap(2) call. The SQEs must still
     *  be allocated separately. This brings the necessary mmap(2) calls down from three to two. Available since
     *  kernel 5.4.
     */
    featSingle_mmap(IORING_FEAT_SINGLE_MMAP),

    /**     If this flag is set, io_uring supports never dropping completion events. If a completion event occurs and
     * the CQ ring is full, the kernel stores the event internally until such a time that the CQ ring has room for
     * more entries. If this overflow condition is entered, attempting to submit more IO will fail with the `-EBUSY`
     * error value, if it can't flush the overflown events to the CQ ring. If this happens, the application must
     * reap events from the CQ ring and attempt the submit again. Available since kernel 5.5.
     */
    featNodrop(IORING_FEAT_NODROP),

    /**      If this flag is set, applications can be certain that any data for async offload has been consumed when
     * the kernel has consumed the SQE. Available since kernel 5.5.
     */
    featSubmit_stable(IORING_FEAT_SUBMIT_STABLE),

    /**    If this flag is set, applications can specify offset == -1 with IORING_OP_{READV,WRITEV} ,
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

/**
The flagsfield is a bit mask. The supported flags are:
 */

enum class sqe_flags(val flagConstant: UInt) {
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
