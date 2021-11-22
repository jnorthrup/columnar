@file:Suppress("EnumEntryName")

package linux_uring.include

import linux_uring.*

/**
io_uring_enter() is used to initiate and complete I/O using the shared submission and completion queues setup by a call to io_uring_setup(2). A single call can both submit new I/O and wait for completions of I/O initiated by this call or previous calls to io_uring_enter().

fd is the file descriptor returned by io_uring_setup(2). to_submit specifies the number of I/Os to submit from the submission queue. flags is a bitmask of the following values:

IORING_ENTER_GETEVENTS
If this flag is set, then the system call will wait for the specificied number of events in min_complete before returning. This flag can be set along with to_submit to both submit and complete events in a single system call.

IORING_ENTER_SQ_WAKEUP
If the ring has been created with IORING_SETUP_SQPOLL, then this flag asks the kernel to wakeup the SQ kernel thread to submit IO.

IORING_ENTER_SQ_WAIT
If the ring has been created with IORING_SETUP_SQPOLL, then the application has no real insight into when the SQ kernel thread has consumed entries from the SQ ring. This can lead to a situation where the application can no longer get a free SQE entry to submit, without knowing when it one becomes available as the SQ kernel thread consumes them. If the system call is used with this flag set, then it will wait until at least one entry is free in the SQ ring.

If the io_uring instance was configured for polling, by specifying IORING_SETUP_IOPOLL in the call to io_uring_setup(2), then min_complete has a slightly different meaning.  Passing a value of 0 instructs the kernel to return any events which are already complete, without blocking.  If min_complete is a non-zero value, the kernel will still return immediately if any completion events are available.  If no event completions are available, then the call will poll either until one or more completions become available, or until the process has exceeded its scheduler time slice.

Note that, for interrupt driven I/O (where IORING_SETUP_IOPOLL was not specified in the call to io_uring_setup(2)), an application may check the completion queue for event completions without entering the kernel at all.

When the system call returns that a certain amount of SQEs have been consumed and submitted, it's safe to reuse SQE entries in the ring. This is true even if the actual IO submission had to be punted to async context, which means that the SQE may in fact not have been submitted yet. If the kernel requires later use of a particular SQE entry, it will have made a private copy of it.

sig is a pointer to a signal mask (see sigprocmask(2)); if sig is not NULL, io_uring_enter() first replaces the current signal mask by the one pointed to by sig, then waits for events to become available in the completion queue, and then restores the original signal mask.  The following io_uring_enter() call:

ret = io_uring_enter(fd, 0, 1, IORING_ENTER_GETEVENTS, &sig);
is equivalent to atomically executing the following calls:
```C
pthread_sigmask(SIG_SETMASK, &sig, &orig);
ret = io_uring_enter(fd, 0, 1, IORING_ENTER_GETEVENTS, NULL);
pthread_sigmask(SIG_SETMASK, &orig, NULL);
```
See the description of pselect(2) for an explanation of why the sig parameter is necessary.

Submission queue entries are represented using the following data structure:

```C
/*
 * IO submission data structure (Submission Queue Entry)
*/
struct io_uring_sqe {
__u8    opcode;         /* type of operation for this sqe */
__u8    flags;          /* IOSQE_ flags */
__u16   ioprio;         /* ioprio for the request */
__s32   fd;             /* file descriptor to do IO on */
union {
__u64   off;            /* offset into file */
__u64   addr2;
};
union {
__u64   addr;       /* pointer to buffer or iovecs */
__u64   splice_off_in;
}
__u32   len;            /* buffer size or number of iovecs */
union {
__kernel_rwf_t  rw_flags;
__u32    fsync_flags;
__u16    poll_events;   /* compatibility */
__u32    poll32_events; /* word-reversed for BE */
__u32    sync_range_flags;
__u32    msg_flags;
__u32    timeout_flags;
__u32    accept_flags;
__u32    cancel_flags;
__u32    open_flags;
__u32    statx_flags;
__u32    fadvise_advice;
__u32    splice_flags;
};
__u64    user_data;     /* data to be passed back at completion time */
union {
struct {
/* index into fixed buffers, if used */
union {
/* index into fixed buffers, if used */
__u16    buf_index;
/* for grouped buffer selection */
__u16    buf_group;
}
/* personality to use, if used */
__u16    personality;
__s32    splice_fd_in;
};
__u64    __pad2[3];
};
};
```
The opcode describes the operation to be performed.  It can be one of:*/

enum class UringOpcode(val opConstant: UInt) {

    /**    Do not perform any I/O. This is useful for testing the performance of
     * the io_uring implementation itself. */
    Op_Nop(IORING_OP_NOP),

    /**
     * Vectored read similar to preadv2(2) and pwritev2(2).  If the file is not seekable,off must be set to zero. */
    Op_Readv(IORING_OP_READV),

    /**
     * Write operations, similar to preadv2(2) and pwritev2(2).   If the file is not seekable,off must be set to zero. */
    Op_Writev(IORING_OP_WRITEV),

    /**
     * Read from pre-mapped buffers. See io_uring_register(2) for details on how to setup a context for fixed reads and writes. */
    Op_Read_fixed(IORING_OP_READ_FIXED),

    /**
     * write to pre-mapped buffers. See io_uring_register(2) for details on how to setup a context for fixed reads and writes. */
    Op_Write_fixed(IORING_OP_WRITE_FIXED),

    /**
     * File sync. See also fsync(2).  Note that, while I/O is initiated in the order in which it appears in
     * the submission queue, completions are unordered. For example, an application which places a write I/O followed
     * by an fsync in the submission queue cannot expect the fsync to apply to the write. the two operations execute
     * in parallel, so the fsync may complete before the write is issued to the storage. The same is also true for
     * previously issued writes that have not completed prior to the fsync. */
    Op_Fsync(IORING_OP_FSYNC),

    /** Poll the fd specified in the submission queue entry for the events specified in the poll_events field. Unlike
     * poll or epoll without `EPOLLONESHOT`,by default this interface always works in one shot mode. That is, once the
     * poll operation is completed, it will have to be resubmitted.
     * If `IORING_POLL_ADD_MULTI` is set in the SQElen field, the n the poll will work in multi shot mode instead. That
     * means it'll repeatedly trigger when the requested event becomes true, and hence multiple CQEs can be generated from
     * this single SQE. The CQE flags field will have `IORING_CQE_F_MORE` set on completion if the application should expect
     * further `CQE` entries from the original request. If this flag isn't set on completion, the n the poll request has
     * been terminated and no further events will be generated
     * @since 5.13.
     *
     *  If `IORING_POLL_UPDATE_EVENTS` is set in the SQE len field, the n the request will update an existing poll request
     *  with the mask of events passed in with this request. The lookup is based on the user_data field of the original `SQE`
     *  submitted, and this values is passed in the addr field of the SQE
     * @since 5.13.
     *  If `IORING_POLL_UPDATE_USER_DATA` is set in the SQE len field, the n the request will update the user_data of an
     *  existing poll request based on the value passed in the off field
     * @since 5.13.
     *
     *  This command works like an async poll(2) and the completion event result is the returned mask of events. For the
     *  variants that update user_data or events, the completion result will be similar to `IORING_OP_POLL_REMOVE`.
     */
    Op_Poll_add(IORING_OP_POLL_ADD),

    /** Remove an existing poll request. If found, the res field of the struct io_uring_cqe will contain 0. If not
     * found,res will contain `-ENOENT` ,or `-EALREADY` if the poll request was in the process of completing already. */
    Op_Poll_remove(IORING_OP_POLL_REMOVE),

    /** Add, remove or modify entries in the interest list of epoll(7). See epoll_ctl(2) for details of the system
     * call
     * @param fd holds the file descriptor that represents the epoll instance
     * @param addr holds the file descriptor to add, remove or modify
     * @param len holds the operation ( `EPOLL_CTL_ADD` , `EPOLL_CTL_DEL` , `EPOLL_CTL_MOD` ) to perform
     * @param off holds a pointer to the epoll_events structure.
     * @since 5.6. */
    Op_Epoll_ctl(IORING_OP_EPOLL_CTL),

    /** Issue the equivalent of a sync_file_range (2) on the file descriptor.
     * @param  fd field is the file descriptor to sync,
     * @param off field holds the offset in bytes
     * @param len field holds the length in bytes
     * @param sync_range_flags field holds the flags for the command.
     * @see sync_file_range(2) for the general description of the related system call.
     * @since 5.2.
     */
    Op_Sync_file_range(IORING_OP_SYNC_FILE_RANGE),

    /**Issue the equivalent of a sendmsg(2)system call
     * @param fd mustbe set to the socket file descriptor
     * @param addr must contain a pointer to the msghdr structure
     * @param  msg_flags holds the flags associated with the system call.
     * @see  sendmsg(2) for the general description of the related system call.
     * @since 5.3.
     */
    Op_Sendmsg(IORING_OP_SENDMSG),

    /**        Works just like IORING_OP_SENDMSG, except for recvmsg(2)instead. See the description of `IORING_OP_SENDMSG` .
     *
     * @since 5.3.
     */
    Op_Recvmsg(IORING_OP_RECVMSG),

    /** Issue the equivalent of a send(2)system call
     * @param fd mustbe set to the socket file descriptor
     * @param addr must contain a pointer to the buffer
     * @param len denotes the length of the buffer to send
     * @param msg_flagsholds the flags associated with the system call.
     * @see send(2) for the general description of the related system call.
     *
     * @since 5.6.
     */
    Op_Send(IORING_OP_SEND),

    /**        Works just like `IORING_OP_SEND` , except forrecv(2)instead. See the description of `IORING_OP_SEND` .
     *
     * @since 5.6.
     */
    Op_Recv(IORING_OP_RECV),

    /**        This command will register a timeout operation.
     * Since 5.15, this command also supports the following modifiers intimeout_flags
     * @param addr field must contain a pointer to a struct timespec64 structure
     * @param len must contain 1 to signify one timespec64 structure
     * @param timeout_flags may contain `IORING_TIMEOUT_ABS` for an absolute timeout value, or 0 for a relative timeout
     * @param off may contain a completion event count. A timeout will trigger a wakeup event on the completion ring
     * for anyone waiting for events. A timeout condition is met when either the specified timeout expires, or the
     * specified number of events have completed. Either condition will trigger the event. If set to 0, completed events
     * are not counted, which effectively acts like a timer. io_uring timeouts use the CLOCK_MONOTONIC clock source.
     * @return 0 if the timeout got completed through requests
     * completing on the ir own.
     * @throws ETIME (*-1) if the timeout got completed through expiration of the timer,
     * @throws ECANCELED (*-1) If the timeout was cancelled before it expired, the request will complete
     * @since 5.4.

     */
    Op_Timeout(IORING_OP_TIMEOUT),


    /**If set, the n the clocksource used is `CLOCK_BOOTTIME` instead of CLOCK_MONOTONIC. This clocksource differs in
     * that it includes time elapsed if the system was suspend while having a timeout request in-flight.*/
    TiMEout_boottime(IORING_TIMEOUT_BOOTTIME),

    /** If set, the n the clocksource used is `CLOCK_BOOTTIME` instead of CLOCK_MONOTONIC.*/
    TiMEout_realtime(IORING_TIMEOUT_REALTIME),

    /**
    If the specified
     * timeoutrequest is found and cancelled
     * successfully, this request will terminatewith a result value of 0 If the timeout request was found but expiration
     * was already in progress,this request will terminate with a result value of `-EBUSY`
     *
     * If the timeout request wasn't found, the request will terminate with a resultvalue of `-ENOENT`
     *
     *
     *
     * If timeout_flags contain `IORING_TIMEOUT_UPDATE` , instead of removing an existing operation,
     * it updates it.addr and return values are same as before.addr2field must contain a pointer to a struct timespec64
     * structure.timeout_flags may also contain IORING_TIMEOUT_ABS, in which case the value given is anabsolute one,
     * not a relative one.
     *
     * @since 5.11.
     * @param   timeout_flags if  zero, then it attempts to remove an existing timeout operation
     * @param addr must contain the  user_datafield of the previously issued timeout operation. */
    Op_Timeout_remove(IORING_OP_TIMEOUT_REMOVE),

    /**        Issue the equivalent of an accept4(2) system call
     *  If the file_indexfield is set to a positive number, the file won't be installed into the normal file table as
     *  usual but will be placed into the fixed file table at indexfile_index - 1.In this case, instead of returning a
     *  file descriptor, the result will containeithe r 0 on success or an error. If the re is already a file registered
     *  at thisindex, the request will fail with `-EBADF` .Only io_uring has access to such files and no other syscall
     *  can use them. See `IOSQE_FIXED_FILE` and `IORING_REGISTER_FILES`
     *
     * @param  fd must be set to the socket file descriptor
     * @param addr must contain the pointer to the sockaddr structure
     * @param addr2 must contain a pointer to the socklen_t addrlen field.
     * @param accept_flagsfield Flags can be passed
     * @see  accept4(2) for the general description of the related system call.
     * @since 5.15.
     */
    Op_Accept(IORING_OP_ACCEPT),

    /**        Attempt to cancel an already issued request.addr must contain the user_datafield of the request that
     *  should be cancelled. The cancellation request willcomplete with one of the following results codes. If found,
     *  the resfield of the cqe will contain 0. If not found,reswill contain -ENOENT. If found and attempted cancelled,
     *  the resfield will contain -EALREADY. In this case, the request may or may notterminate. In general, requests
     *  that are interruptible (like socket IO) willget cancelled, while d isk IO requests cannot be cancelled if
     *  already started.
     * @since 5.5.
     */
    Op_Async_cancel(IORING_OP_ASYNC_CANCEL),

    /**        This request must be linked with anothe r request throughIOSQE_IO_LINKwhich is described below. Unlike
     * IORING_OP_TIMEOUT,IORING_OP_LINK_TIMEOUTacts on the linked request, not the completion queue. The format of the
     * command is othe rw ise likeIORING_OP_TIMEOUT,except there's no completion event count as it's tied to a specific
     * request.If used, the timeout specified in the command will cancel the linked command,unless the linked command
     * completes before the timeout. The timeout willcomplete with-ETIMEif the timer expired and the linked request was
     * attempted cancelled, or `-ECANCELED` if the timer got cancelled because of completion of the linked request. Like
     * `IORING_OP_TIMEOUT` the clock source used isCLOCK_MONOTONIC
     * @since 5.5.
     */
    Op_Link_timeout(IORING_OP_LINK_TIMEOUT),

    /**        Issue the equivalent of aconnect(2) system call.fd mustbe set to the socket file descriptor,addr must
     * contain the const pointer to the sockaddr structure, and off must contain the socklen_t addrlen field. See also
     * connect(2) for the general description of the related system call.
     *
     * @since 5.5.
     */
    Op_Connect(IORING_OP_CONNECT),

    /**        Issue the equivalent of a fallocate(2) system call.fd mustbe set to the file descriptor,len must contain
     * the mode associated with the operation,off must contain the offset on which to operate, and addr must contain
     * the length. See also fallocate(2) for the general description of the related system call.
     *
     * @since 5.6.
     */
    Op_Fallocate(IORING_OP_FALLOCATE),

    /** Issue the equivalent of aposix_fadv ise(2)system call.fd mustbe set to the file descriptor,off
     * must contain the offset on which to operate,len must contain the length, and fadv ise_advice must contain the
     * advice associated with the operation. See alsoposix_fadv ise(2) for the general description of the related system
     * call.
     * @since 5.6.
     */
    Op_Fadvise(IORING_OP_FADVISE),

    /** Issue the equivalent of a madvise(2) system call.addr must contain the address to operate on,len
     * must contain the length on which to operate,and fadvise_advice must contain the advice associated with
     * the operation. See also madvise(2) for the general description of the related system call.
     *
     * @since 5.6.
     */
    Op_Madvise(IORING_OP_MADVISE),

    /** Issue the equivalent of a openat(2) system call.
     *
     * ```
     * int openat(int dirfd, const char *pathname, int flags);
     * ```
     *
     *

     * @param fd is the dirfd argument,
     * @param addr must contain a pointer to the pathname argument
     * @param open_flags should contain any flags passed in,
     * @param len is access mode of the file
     * @see openat  for the general description of the related system call.
     * @return returning a file descriptor, the result will contain either 0 on success or an error.
     * Only io_uring has access to such files and no other syscall can use them.
     *
     * @since 5.6.
     * @param file_index field if set to a positive number, the file won't be installed into the normal file table as usual but will be placed into the fixed file table at
     * index `file_index-1`.
     *
     * @exception  EBADF*-1  If there is already a file registered at this index, the request will fail
     * @see IOSQE_FIXED_FILE and
     * @see IORING_REGISTER_FILES
     * @since 5.15
     */
    Op_Openat(IORING_OP_OPENAT),

    /**        Issue the equivalent of a openat2(2)system call.
     * ```
     * long syscall(SYS_openat2, int dirfd, const char *pathname, struct open_how *how, size_t size);
     * ```
     *
     * @param fd is the dirfd argument,
     * @param addr must contain a pointer to the  pathname argument,
     * @param len should contain the size of the open_how structure
     * @param off should be set to the address of the open_how structure.
     * @see  openat2  for the general description of the related system call.
     * @since 5.6.
     * @param file_index if set to a positive number, the file won't be installed into the normal
     * file table as usual but will be placed into the fixed file table at index `file_index - 1`. In this case,
     * instead of returning a file descriptor, the result will contain either 0 on success or an error.
     * Only io_uring has access to such files and no other syscall can use them.
     * @return an fd opened
     * @exception EBADF (minus) If there is already a file registered at this index, the request will fail.
     * @see`IOSQE_FIXED_FILE`
     * @see `IORING_REGISTER_FILES`
     * @since 5.15.
     */
    Op_Openat2(IORING_OP_OPENAT2),

    /** Issue the equivalent of a close(2) system call.fd is the file descriptor to be closed. See also close(2) for
     *  the general description of the related system call.
     *
     * @since 5.6.
     */
    Op_Close(IORING_OP_CLOSE),

    /**Issue the equivalent of as tatx(2) system call.fd is the dirfd argument, addr must contain a pointer to the
     * pathname string,statx_flags is the flags argument,len should be the mask argument, and off must contain a pointer to
     * the statxbufto be filled in. See alsostatx(2) for the general description of the related system call.
     *
     * @since 5.6.
     */
    Op_Statx(IORING_OP_STATX),

    /**        Issue the equivalent of apread(2)orpwrite(2)system call.fd is the file descriptor to be operated on,addr
     * contains the buffer in question,lencontains the length of the IO operation, and offscontains the read or write
     * offset. Iffddoes not refer to a seekable file,off mustbe set to zero. Ifoffs is set to -1, the offset will use
     * (and advance) the file position, like theread(2)and write(2)system calls. the se are non-vectored versions of
     * the IORING_OP_READVand IORING_OP_WRITEVopcodes. See alsoread(2)and write(2)for the general description of the
     * related system call.
     *
     * @since 5.6.
     */
    Op_Read(IORING_OP_READ),

    /**        Issue the equivalent of a pread(2) or pwrite(2) system call. fd is the file descriptor to be operated on,
     * addr contains the buffer in question,len contains the length of the IO operation, and offs contains the
     * read or write offset. If fd does not refer to a seekable file,off mustbe set to zero. If offs is set to -1,
     * the offset will use (and advance) the file position, like the read(2) and write(2) system calls. these are
     * non-vectored versions of the `IORING_OP_READV` and `IORING_OP_WRITEV` opcodes. See also read(2) and write(2)
     * for the general description of the related system call.
     *
     * @since 5.6.
     */
    Op_Write(IORING_OP_WRITE),

    /** Issue the equivalent of asplice(2)system call.splice_fd_in is the file descriptor to read
     *  from, splice_off_in is an offset to read from,fd is the file descriptor to write to,off is an offset from which
     *  to start writing to. A sentinel value of -1 is usedto pass the equivalent of a `NULL` for the offsets tosplice(2).
     *  lencontains the number of bytes to copy.splice_flagscontains a bit mask for the flag field associated with the
     *  system call.Please note that one of the file descriptors must refer to a pipe.See alsosplice(2)for the general
     *  description of the related system call.
     *
     * @since 5.7.
     */
    Op_Splice(IORING_OP_SPLICE),

    /** Issue the equivalent of atee(2)system call.splice_fd_in is the file descriptor to read from,fd is the file
     *  descriptor to write to,lencontains the number of bytes to copy, and splice_flagscontains a bit mask for the
     *  flag field associated with the system call.Please note that both of the file descriptors must refer to a pipe.
     *  See alsotee(2)for the general description of the related system call.
     *
     * @since 5.8.
     */
    Op_Tee(IORING_OP_TEE),

    /**This command is an alternative to usingIORING_REGISTER_FILES_UPDATEwhich the n works in an async fashion,
     *  like the rest of the io_uring command s.The arguments passed in are the same.addr must contain a pointer to the
     *  array of file descriptors,len must contain the length of the array, and off must contain the offset at which to
     *  operate. Note that the array of filedescriptors pointed to inaddr mustremain valid until
     *  this operation has completed.
     * @since 5.6.
     */
    Op_Files_update(IORING_OP_FILES_UPDATE),

    /**This command allows an application to register a group of buffers to be used by commands that read/receive
     * data. Using buffers in this manner can eliminate the need to separate the poll + read, which provides a convenient
     * point in time to allocate a buffer for a given request. It's often infeasible to have as many buffers available as
     * pending reads or receive. With this feature, the application can have its pool of buffers ready in the kernel,
     * and when the file or socket is ready to read/receive data, a buffer can be selected for the operation.
     *
     * @param fd must contain the number of buffers to provide
     * @param addr must contain the starting address to add buffers from
     * @param len must contain the length of each buffer to add from the range
     * @param buf_group must contain the group ID of this range of buffers
     * @param off must contain the starting buffer ID of this range of buffers.
     *
     * With that set,the kernel adds
     * buffers starting with the memory address inaddr,each with a length of len.  Hence the application should
     * provide len * fd worth of memory inaddr.
     *
     * Buffers are grouped by the group ID, and each buffer within this group will be
     * identical in size according to the above arguments. This allows the application to provide different groups of
     * buffers, and this is often used to have differently sized buffers available depending on what the expectations
     * are of the individual request. When submitting a request that should use a provided buffer, the
     * `IOSQE_BUFFER_SELECT` flag must be set, and buf_group must be set to the desired buffer group ID where
     * the buffer should be selected from.
     *
     *
     * @since 5.7.
     */
    Op_Provide_buffers(IORING_OP_PROVIDE_BUFFERS),

    /**Remove buffers previously registered with `IORING_OP_PROVIDE_BUFFERS`
     * @param fd must contain the number of buffers to remove
     *  @param buf_group must contain the buffer group ID from which to remove the buffers.
     *
     *  @since  5.7.
     */
    Op_Remove_buffers(IORING_OP_REMOVE_BUFFERS),

    /**        Issue the equivalent of ashutdown(2)system call.fd is the file descriptor to the socket being shutdown,
     * no othe r fields shouldbe set.
     * @since 5.11.
     */
    Op_Shutdown(IORING_OP_SHUTDOWN),

    /**
     * Issue the equivalent of a renameat2(2) system call
     * @param fdshould be set to the old dirfd
     * @param addr should be set to
     * the oldpath
     * @param lenshould be set to thenewdirfd
     * @param  addrshould be set to theoldpath
     * @param addr2 should be set to thenewpath
     * @param rename_flags should be set to the flagspassed in to renameat2(2).
     * @since 5.11.
     */
    Op_Renameat(IORING_OP_RENAMEAT),

    /**Issue the equivalent of a unlinkat2(2)system call
     * @param fdshould be set to the dirfd
     * @param addr should be set to the pathname
     * @param  unlink_flags should be set to the flags being passed in to unlinkat(2).
     * @since 5.11.
     */
    Op_Unlinkat(IORING_OP_UNLINKAT),

    /**        Issue the equivalent of a mkdirat2(2) system call
     * @param fd should be set to the dir fd
     * @param addr should be set to
     *  the pathname
     *  @param len should be set to the mode being passed in to mkdirat(2).
     *
     * @since 5.15.
     */
    Op_Mkdirat(IORING_OP_MKDIRAT),

    /** Issue the equivalent of a symlinkat2(2) system call.
     * @param  fd should be set to the new dir fd
     * @param addr should be set to
     *  the target
     *  @param  addr2 should be set to the link path being passed in to symlinkat(2).
     *
     * @since 5.15.
     */
    Op_Symlinkat(IORING_OP_SYMLINKAT),

    /**        Issue the equivalent of a linkat2(2) system call
     * @param fd should be set to the old dirfd
     * @param addr should be set to
     *  the oldpath
     *  @param lenshould be set to the new dirfd
     *  @param addr2 should be set to the newpath
     *  @param hardlink_flagsshould be set to
     *  the flagsbeing passed in to linkat(2).
     *
     * @since 5.15.

     */
    Op_Linkat(IORING_OP_LINKAT),
}
