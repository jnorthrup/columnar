package linux_uring

/**
 * #The opcode
 * describes the operation to be performed. It can be one of the enum
 *
 * #RETURNVALUE
io_uring_enter() returns the number of I/Os successfully consumed. This can be zero if to_submit was zero or if the
submission queue was empty. Note that if the ring was created with IORING_SETUP_SQPOLL specified, then the return value
will generally be the same asto_submit as submission happens outside the context of the system call.

The errors related to a submission queue entry will be returned through acompletion queue entry (see sectionCQE ERRORS),
rather than through the system call itself.

Errors that occur not on behalf of a submission queue entry are returned via thesystem call directly. On such an error,
-1 is returned anderrnois set appropriately.
 */

enum class uring_evt(val opConstant: UInt) {

    /**    Do not perform any I/O. This is useful for testing the performance of
     * the io_uring implementation itself. */
    Op_Nop(IORING_OP_NOP),

    /**    Vectored read and write operations, similar topreadv2(2)andpwritev2(2).If the file is not seekable,off
     * must be set to zero. */
    Op_Readv(IORING_OP_READV),
    Op_Writev(IORING_OP_WRITEV),

    /**    Read from or write to pre-mapped buffers. Seeio_uring_register(2)for details on how to setup a context for
     *  fixed reads and writes. */
    Op_Read_fixed(IORING_OP_READ_FIXED),
    Op_Write_fixed(IORING_OP_WRITE_FIXED),

    /**    File sync. See alsofsync(2).Note that, while I/O is initiated in the order in which it appears in
     * the submission queue, completions are unordered. For example, anapplication which places a write I/O followed by an fsync in thesubmission queue cannot expect the fsync to apply to the write. Thetwo operations execute in parallel, so the fsync may complete beforethe write is issued to the storage. The same is also true forpreviously issued writes that have not completed prior to the fsync. */
    Op_Fsync(IORING_OP_FSYNC),

    /** Poll thefdspecified in the submission queue entry for the eventsspecified in thepoll_eventsfield. Unlike poll
     * or epoll withoutEPOLLONESHOT,by default this interface always works in one shot mode. That is, once the polloperation is completed, it will have to be resubmitted.
    If IORING_POLL_ADD_MULTI is set in the SQElenfield, then the poll will work in multi shot mode instead. That
    means it'llrepatedly trigger when the requested event becomes true, and hence multipleCQEs can be generated from this single SQE. The CQEflagsfield will haveIORING_CQE_F_MOREset on completion if the application should expect further CQE entries fromthe original request. If this flag isn't set on completion, then the pollrequest has been terminated and no further events will be generated. This mode is available since 5.13.

    If IORING_POLL_UPDATE_EVENTSi s set in the SQElenfield, then the request will update an existing poll request with the mask ofevents passed in with this request. The lookup is based on theuser_datafield of the original SQE submitted, and this values is passed in theaddrfield of the SQE. This mode is available since 5.13.

    If IORING_POLL_UPDATE_USER_DATA is set in the SQElenfield, then the request will update theuser_dataof an existing poll request based on the value passed in theofffield. This mode is available since 5.13.

    This command works likean asyncpoll(2) and the completion event result is the returned mask of events. For thevariants that updateuser_dataorevents, the completion result will be similar toIORING_OP_POLL_REMOVE.
     */
    Op_Poll_add(IORING_OP_POLL_ADD),

    /**
    Remove an existing poll request. If found, theresfield of thestruct io_uring_cqewill contain 0. If not found,reswill contain-ENOENT,or-EALREADYif the poll request was in the process of completing already.
     */

    Op_Poll_remove(IORING_OP_POLL_REMOVE),

    /**
    Add, remove or modify entries in the interest list ofepoll(7).Seeepoll_ctl(2)for details of the system call.fdholds the file descriptor that represents the epoll instance,addrholds the file descriptor to add, remove or modify,lenholds the operation (EPOLL_CTL_ADD, EPOLL_CTL_DEL, EPOLL_CTL_MOD) to perform and,offholds a pointer to theepoll_eventsstructure. Available since 5.6.
     */
    Op_Epoll_ctl(IORING_OP_EPOLL_CTL),

    /**
    Issue the equivalent of a sync_file_range (2) on the file descriptor. The fdfield is the file descriptor to sync, theofffield holds the offset in bytes, thelenfield holds the length in bytes, and thesync_range_flagsfield holds the flags for the command. See alsosync_file_range(2)for the general description of the related system call. Available since 5.2.
     */
    Op_Sync_file_range(IORING_OP_SYNC_FILE_RANGE),

    /**        Issue the equivalent of asendmsg(2)system call.fdmust be set to the socket file descriptor,addrmust contain a pointer to the msghdr structure, andmsg_flagsholds the flags associated with the system call. See alsosendmsg(2)for the general description of the related system call. Available since 5.3.
     */
    Op_Sendmsg(IORING_OP_SENDMSG),

    /**        Works just like IORING_OP_SENDMSG, except forrecvmsg(2)instead. See the description of IORING_OP_SENDMSG. Available since 5.3.
     */
    Op_Recvmsg(IORING_OP_RECVMSG),

    /**        Issue the equivalent of asend(2)system call.fdmust be set to the socket file descriptor,addrmust contain a pointer to the buffer,lendenotes the length of the buffer to send, andmsg_flagsholds the flags associated with the system call. See alsosend(2)for the general description of the related system call. Available since 5.6.
     */
    Op_Send(IORING_OP_SEND),

    /**        Works just like IORING_OP_SEND, except forrecv(2)instead. See the description of IORING_OP_SEND. Available since 5.6.
     */
    Op_Recv(IORING_OP_RECV),

    /**        This command will register a timeout operation. Theaddrfield must contain a pointer to a struct timespec64 structure,lenmust contain 1 to signify one timespec64 structure,timeout_flagsmay contain IORING_TIMEOUT_ABSfor an absolute timeout value, or 0 for a relative timeout.offmay contain a completion event count. A timeoutwill trigger a wakeup event on the completion ring for anyone waiting forevents. A timeout condition is met when either the specified timeout expires,or the specified number of events have completed. Either condition willtrigger the event. If set to 0, completed events are not counted, whicheffectively acts like a timer. io_uring timeouts use theCLOCK_MONOTONICclock source. The request will complete with-ETIMEif the timeout got completed through expiration of the timer, or0if the timeout got completed through requests completing on their own. Ifthe timeout was cancelled before it expired, the request will complete with-ECANCELED.Available since 5.4.
    Since 5.15, this command also supports the following modifiers intimeout_flags:
     */
    Op_Timeout(IORING_OP_TIMEOUT),


    /*If set, then the clocksource used is CLOCK_BOOTTIME instead of CLOCK_MONOTONIC.This clocksource differs in that it includes time elapsed if the system was suspend while having a timeout request in-flight.*/
    TiMEout_boottime(IORING_TIMEOUT_BOOTTIME),

    /* If set, then the clocksource used is CLOCK_BOOTTIME instead of CLOCK_MONOTONIC.*/
    TiMEout_realtime(IORING_TIMEOUT_REALTIME),

    /*Iftimeout_flags are zero, then it attempts to remove an existing timeoutoperation.addrmust contain theuser_datafield of the previously issued timeout operation. If the specified timeoutrequest is found and cancelled successfully, this request will terminatewith a result value of0If the timeout request was found but expiration was already in progress,this request will terminate with a result value of-EBUSYIf the timeout request wasn't found, the request will terminate with a resultvalue of-ENOENTAvailable since 5.5.
    Iftimeout_flagscontainIORING_TIMEOUT_UPDATE,instead of removing an existing operation, it updates it.addrand return values are same as before.addr2field must contain a pointer to a struct timespec64 structure.timeout_flagsmay also contain IORING_TIMEOUT_ABS, in which case the value given is anabsolute one, not a relative one.Available since 5.11.
*/
    Op_Timeout_remove(IORING_OP_TIMEOUT_REMOVE),

    /**        Issue the equivalent of an accept4(2) system call. fd must be set to the socket file descriptor,addr must contain the pointer to the sockaddr structure, and addr2 must contain a pointer to the socklen_t addrlen field. Flags can be passed usingtheaccept_flagsfield. See alsoaccept4(2)for the general description of the related system call. Available since 5.5.
    If the file_indexfield is set to a positive number, the file won't be installed into the normal file table as usual but will be placed into the fixed file table at indexfile_index - 1.In this case, instead of returning a file descriptor, the result will containeither 0 on success or an error. If there is already a file registered at thisindex, the request will fail with-EBADF.Only io_uring has access to such files and no other syscall can use them. SeeIOSQE_FIXED_FILEandIORING_REGISTER_FILES.

    Available since 5.15.
     */
    Op_Accept(IORING_OP_ACCEPT),

    /**        Attempt to cancel an already issued request.addrmust contain theuser_datafield of the request that should be cancelled. The cancellation request willcomplete with one of the following results codes. If found, theresfield of the cqe will contain 0. If not found,reswill contain -ENOENT. If found and attempted cancelled, theresfield will contain -EALREADY. In this case, the request may or may notterminate. In general, requests that are interruptible (like socket IO) willget cancelled, while disk IO requests cannot be cancelled if already started.Available since 5.5.
     */
    Op_Async_cancel(IORING_OP_ASYNC_CANCEL),

    /**        This request must be linked with another request throughIOSQE_IO_LINKwhich is described below. UnlikeIORING_OP_TIMEOUT,IORING_OP_LINK_TIMEOUTacts on the linked request, not the completion queue. The format of the commandis otherwise likeIORING_OP_TIMEOUT,except there's no completion event count as it's tied to a specific request.If used, the timeout specified in the command will cancel the linked command,unless the linked command completes before the timeout. The timeout willcomplete with-ETIMEif the timer expired and the linked request was attempted cancelled, or-ECANCELEDif the timer got cancelled because of completion of the linked request. LikeIORING_OP_TIMEOUTthe clock source used isCLOCK_MONOTONICAvailable since 5.5.
     */
    Op_Link_timeout(IORING_OP_LINK_TIMEOUT),

    /**        Issue the equivalent of aconnect(2)system call.fdmust be set to the socket file descriptor,addrmust contain the const pointer to the sockaddr structure, andoffmust contain the socklen_t addrlen field. See alsoconnect(2)for the general description of the related system call. Available since 5.5.
     */
    Op_Connect(IORING_OP_CONNECT),

    /**        Issue the equivalent of afallocate(2)system call.fdmust be set to the file descriptor,lenmust contain the mode associated with the operation,offmust contain the offset on which to operate, andaddrmust contain the length. See alsofallocate(2)for the general description of the related system call. Available since 5.6.
     */
    Op_Fallocate(IORING_OP_FALLOCATE),

    /**        Issue the equivalent of aposix_fadvise(2)system call.fdmust be set to the file descriptor,offmust contain the offset on which to operate,lenmust contain the length, andfadvise_advicemust contain the advice associated with the operation. See alsoposix_fadvise(2)for the general description of the related system call. Available since 5.6.
     */
    Op_Fadvise(IORING_OP_FADVISE),

    /**        Issue the equivalent of amadvise(2)system call.addrmust contain the address to operate on,lenmust contain the length on which to operate,andfadvise_advicemust contain the advice associated with the operation. See alsomadvise(2)for the general description of the related system call. Available since 5.6.
     */
    Op_Madvise(IORING_OP_MADVISE),

    /**        Issue the equivalent of aopenat(2)system call.fdis thedirfdargument,addrmust contain a pointer to the
     * *pathnameargument,open_flagsshould contain any flags passed in, andlenis access mode of the file. See alsoopenat(2)for the general description of the related system call. Available since 5.6.
    If thefile_indexfield is set to a positive number, the file won't be installed into thenormal file table as usual
    but will be placed into the fixed file table at indexfile_index - 1.In this case, instead of returning a file descriptor, the result will containeither 0 on success or an error. If there is already a file registered at thisindex, the request will fail with-EBADF.Only io_uring has access to such files and no other syscall can use them. SeeIOSQE_FIXED_FILEandIORING_REGISTER_FILES.

    Available since 5.15.
     */
    Op_Openat(IORING_OP_OPENAT),

    /**        Issue the equivalent of aopenat2(2)system call.fdis thedirfdargument,addrmust contain a pointer to
     * the*pathnameargument,lenshould contain the size of the open_how structure, andoffshould be set to the address of the open_how structure. See alsoopenat2(2)for the general description of the related system call. Available since 5.6.
    If thefile_indexfield is set to a positive number, the file won't be installed into thenormal file table as usual
    but will be placed into the fixed file table at indexfile_index - 1.In this case, instead of returning a file descriptor, the result will containeither 0 on success or an error. If there is already a file registered at thisindex, the request will fail with-EBADF.Only io_uring has access to such files and no other syscall can use them. SeeIOSQE_FIXED_FILEandIORING_REGISTER_FILES.

    Available since 5.15.
     */
    Op_Openat2(IORING_OP_OPENAT2),

    /**        Issue the equivalent of aclose(2)system call.fdis the file descriptor to be closed. See alsoclose(2)for
     *  the general description of the related system call. Available since 5.6.
     */
    Op_Close(IORING_OP_CLOSE),

    /**        Issue the equivalent of astatx(2)system call.fdis thedirfdargument,addrmust contain a pointer to the
     * *pathnamestring,statx_flagsis theflagsargument,lenshould be themaskargument, andoffmust contain a pointer to thestatxbufto be filled in. See alsostatx(2)for the general description of the related system call. Available since 5.6.
     */
    Op_Statx(IORING_OP_STATX),

    /**        Issue the equivalent of apread(2)orpwrite(2)system call.fdis the file descriptor to be operated on,addr
     * contains the buffer in question,lencontains the length of the IO operation, andoffscontains the read or write offset. Iffddoes not refer to a seekable file,offmust be set to zero. Ifoffsis set to -1, the offset will use (and advance) the file position, like theread(2)andwrite(2)system calls. These are non-vectored versions of theIORING_OP_READVandIORING_OP_WRITEVopcodes. See alsoread(2)andwrite(2)for the general description of the related system call. Available since 5.6.
     */
    Op_Read(IORING_OP_READ),

    /**        Issue the equivalent of apread(2)orpwrite(2)system call.fdis the file descriptor to be operated on,addr
     * contains the buffer in question,lencontains the length of the IO operation, andoffscontains the read or write offset. Iffddoes not refer to a seekable file,offmust be set to zero. Ifoffsis set to -1, the offset will use (and advance) the file position, like theread(2)andwrite(2)system calls. These are non-vectored versions of theIORING_OP_READVandIORING_OP_WRITEVopcodes. See alsoread(2)andwrite(2)for the general description of the related system call. Available since 5.6.
     */
    Op_Write(IORING_OP_WRITE),

    /**        Issue the equivalent of asplice(2)system call.splice_fd_inis the file descriptor to read
     *  from,splice_off_inis an offset to read from,fdis the file descriptor to write to,offis an offset from which to start writing to. A sentinel value of -1 is usedto pass the equivalent of a NULL for the offsets tosplice(2).lencontains the number of bytes to copy.splice_flagscontains a bit mask for the flag field associated with the system call.Please note that one of the file descriptors must refer to a pipe.See alsosplice(2)for the general description of the related system call. Available since 5.7.
     */
    Op_Splice(IORING_OP_SPLICE),

    /**        Issue the equivalent of atee(2)system call.splice_fd_inis the file descriptor to read from,fdis the file
     *  descriptor to write to,lencontains the number of bytes to copy, andsplice_flagscontains a bit mask for the flag field associated with the system call.Please note that both of the file descriptors must refer to a pipe.See alsotee(2)for the general description of the related system call. Available since 5.8.
     */
    Op_Tee(IORING_OP_TEE),

    /**        This command is an alternative to usingIORING_REGISTER_FILES_UPDATEwhich then works in an async fashion,
     *  like the rest of the io_uring commands.The arguments passed in are the same.addrmust contain a pointer to the
     *  array of file descriptors,lenmust contain the length of the array, andoffmust contain the offset at which to
     *  operate. Note that the array of filedescriptors pointed to inaddrmust remain valid until
     *  this operation has completed. Available since 5.6.
     */
    Op_Files_update(IORING_OP_FILES_UPDATE),

    /**        This command allows an application to register a group of buffers to be usedby commands that read/receive
     * data. Using buffers in this manner can eliminatethe need to separate the poll + read, which provides a convenient
     * point intime to allocate a buffer for a given request. It's often infeasible to haveas many buffers available as
     * pending reads or receive. With this feature, theapplication can have its pool of buffers ready in the kernel,
     * and when thefile or socket is ready to read/receive data, a buffer can be selected for theoperation.fdmust
     * contain the number of buffers to provide,addrmust contain the starting address to add buffers from,lenmust
     * contain the length of each buffer to add from the range,buf_groupmust contain the group ID of this range of
     * buffers, andoffmust contain the starting buffer ID of this range of buffers. With that set,the kernel adds
     * buffers starting with the memory address inaddr,each with a length oflen.Hence the application should provide
     * len * fdworth of memory inaddr.Buffers are grouped by the group ID, and each buffer within this group will be
     * identical in size according to the above arguments. This allows the applicationto provide different groups of
     * buffers, and this is often used to havedifferently sized buffers available depending on what the expectations
     * are ofthe individual request. When submitting a request that should use a providedbuffer, theIOSQE_BUFFER_SELECT
     * flag must be set, andbuf_groupmust be set to the desired buffer group ID where the buffer should be selectedfrom.
     * Available since 5.7.
     */
    Op_Provide_buffers(IORING_OP_PROVIDE_BUFFERS),

    /**        Remove buffers previously registered withIORING_OP_PROVIDE_BUFFERS.fdmust contain the number of buffers
     *  to remove, andbuf_groupmust contain the buffer group ID from which to remove the buffers. Availablesince 5.7.
     */
    Op_Remove_buffers(IORING_OP_REMOVE_BUFFERS),

    /**        Issue the equivalent of ashutdown(2)system call.fdis the file descriptor to the socket being shutdown,
     * no other fields shouldbe set. Available since 5.11.
     */
    Op_Shutdown(IORING_OP_SHUTDOWN),

    /**        Issue the equivalent of arenameat2(2)system call.fdshould be set to theolddirfd,addrshould be set to
     * theoldpath,lenshould be set to thenewdirfd,addrshould be set to theoldpath,addr2should be set to thenewpath,and
     * finallyrename_flagsshould be set to theflagspassed in torenameat2(2).Available since 5.11.
     */
    Op_Renameat(IORING_OP_RENAMEAT),

    /**        Issue the equivalent of aunlinkat2(2)system call.fdshould be set to thedirfd,addrshould be set to
     *  thepathname,andunlink_flagsshould be set to theflagsbeing passed in tounlinkat(2).Available since 5.11.
     */
    Op_Unlinkat(IORING_OP_UNLINKAT),

    /**        Issue the equivalent of amkdirat2(2)system call.fdshould be set to thedirfd,addrshould be set to
     *  thepathname,andlenshould be set to themodebeing passed in tomkdirat(2).Available since 5.15.
     */
    Op_Mkdirat(IORING_OP_MKDIRAT),

    /**        Issue the equivalent of asymlinkat2(2)system call.fdshould be set to thenewdirfd,addrshould be set to
     *  thetargetandaddr2should be set to thelinkpathbeing passed in tosymlinkat(2).Available since 5.15.
     */
    Op_Symlinkat(IORING_OP_SYMLINKAT),

    /**        Issue the equivalent of alinkat2(2)system call.fdshould be set to theolddirfd,addrshould be set to
     *  theoldpath,lenshould be set to thenewdirfd,addr2should be set to thenewpath,andhardlink_flagsshould be set to theflagsbeing passed in tolinkat(2).Available since 5.15.
    Theflagsfield is a bit mask. The supported flags are:

     */
    Op_Linkat(IORING_OP_LINKAT),
}


enum class sqe_flags(val flagConstant: UInt) {
    /** When this flag is specified,fdis an index into the files array registered with the io_uring instance (see
     * theIORING_REGISTER_FILESsection of theio_uring_register(2)man page). Note that this isn't always available for all commands. If used ona command that doesn't support fixed files, the SQE will error with-EBADF.Available since 5.1. */
    Fixed_file(IOSQE_FIXED_FILE),

    /** When this flag is specified, the SQE will not be started before previouslysubmitted SQEs have completed, and new
     * SQEs will not be started before thisone completes. Available since 5.2. */
    Io_drain(IOSQE_IO_DRAIN),

    /** When this flag is specified, it forms a link with the next SQE in thesubmission ring. That next SQE will not be
     *  started before this one completes.This, in effect, forms a chain of SQEs, which can be arbitrarily long. The tailof the chain is denoted by the first SQE that does not have this flag set.This flag has no effect on previous SQE submissions, nor does it impact SQEsthat are outside of the chain tail. This means that multiple chains can beexecuting in parallel, or chains and individual SQEs. Only members inside thechain are serialized. A chain of SQEs will be broken, if any request in thatchain ends in error. io_uring considers any unexpected result an error. Thismeans that, eg, a short read will also terminate the remainder of the chain.If a chain of SQE links is broken, the remaining unstarted part of the chainwill be terminated and completed with-ECANCELEDas the error code. Available since 5.3. */
    Io_link(IOSQE_IO_LINK),

    /** Like IOSQE_IO_LINK, but it doesn't sever regardless of the completion result.Note that the link will still sever
     *  if we fail submitting the parent request,hard links are only resilient in the presence of completion results forrequests that did submit correctly. IOSQE_IO_HARDLINK implies IOSQE_IO_LINK.Available since 5.5. */
    Io_hardlink(IOSQE_IO_HARDLINK),

    /** Normal operation for io_uring is to try and issue an sqe as non-blocking first,and if that fails, execute it in
     * an async manner. To support more efficient overlapped operation of requests that the application knows/assumes willalways (or most of the time) block, the application can ask for an sqe to beissued async from the start. Available since 5.6. */
    Async(IOSQE_ASYNC),

    /** Used in conjunction with theIORING_OP_PROVIDE_BUFFERScommand, which registers a pool of buffers to be used by
     *  commands that reador receive data . When buffers are registered for this use case, and thisflag is set in the command, io_uring will grab a buffer from this pool whenthe request is ready to receive or read data . If successful, the resulting CQEwill haveIORING_CQE_F_BUFFERset in the flags part of the struct, and the upperIORING_CQE_BUFFER_SHIFTbits will contain the ID of the selected buffers. This allows the applicationto know exactly which buffer was selected for the operation. If no buffersare available and this flag is set, then the request will fail with-ENOBUFSas the error code. Once a buffer has been used, it is no longer available inthe kernel pool. The application must re-register the given buffer again whenit is ready to recycle it (eg has completed using it). Available since 5.7. */
    Buffer_select(IOSQE_BUFFER_SELECT),
}
//@formatter:on
