package test.lfsopenat

import kotlinx.cinterop.*
import linux_uring.*
import platform.posix.*
import platform.posix.O_RDWR
import platform.posix.S_IRUSR
import platform.posix.iovec
import simple.HasPosixErr
import simple.simple.CZero.nz
import simple.simple.CZero.z
import linux_uring.calloc as linux_uringCalloc
import linux_uring.strerror as posix_strerror1
import platform.posix.calloc as posixCalloc
import platform.posix.close as posixClose
import platform.posix.dup as posixDup
import platform.posix.malloc as posixMalloc
import platform.posix.open as posixOpen
import platform.posix.pipe as posixPipe
import platform.posix.pwrite as posixPwrite
import platform.posix.strndup as posixStrndup
import platform.posix.unlink as posixUnlink

//
//#include <string.h>
//#include <stdio.h>
//#include <stdlib.h>
//#include <sys/types.h>
//#include <sys/stat.h>
//#include <fcntl.h>
//#include <errno.h>
//#include <sys/resource.h>
//#include <unistd.h>
//
//#include "liburing.h"

//#define DIE(...) do {\
//		fprintf(stderr, __VA_ARGS__);\
//		abort();\
//	} while(0);

const val _LARGEFILE_SOURCE = true
const val _FILE_OFFSET_BITS = 64

const val RSIZE: Int = 2
const val OPEN_FLAGS: Int = O_RDWR or platform.posix.O_CREAT
val OPEN_MODE: platform.posix.mode_t = (S_IRUSR or platform.posix.S_IWUSR).toUInt()
public fun lfsopenat( ):Unit = memScoped{//val argc=argv.size
    var   fn:String = "io_uring_openat_test"
    var   ring:io_uring=alloc()
    var   ret:Int
    var dfd:Int

//    HasPosixErr.posixRequires(! (argc > 0)){"no args accepted"}
    dfd = posixOpen("/tmp",  platform.posix.__O_PATH   )
    HasPosixErr.posixRequires(! (dfd < 0))
    { "open /tmp: "+ posix_strerror1(platform.posix.errno) }

    ret = io_uring_queue_init(RSIZE.toUInt(), ring.ptr.reinterpret(), 0)
    HasPosixErr.posixRequires(! (ret < 0))
    { "failed to init io_uring: "+ posix_strerror1(-ret) }

    HasPosixErr.posixRequires(prepare_file(dfd, fn).z){"prepare_file"}


    ret = open_io_uring(ring.ptr, dfd, fn)
    HasPosixErr.posixRequires((ret. z)) {
        "open_io_uring() failed"
    }

    ret = test_linked_files(dfd, fn, false)
    HasPosixErr.posixRequires((ret. z)) {
        "test_linked_files() !async failed"
    }

    ret = test_linked_files(dfd, fn, true)
    HasPosixErr.posixRequires((ret.z)) {
        "test_linked_files() async failed"
    }

    ret = test_drained_files(dfd, fn, false, false)
    HasPosixErr.posixRequires((ret. z)) {
        "test_drained_files() failed"
    }

    ret = test_drained_files(dfd, fn, false, true)
    HasPosixErr.posixRequires((ret. z)) {
        "test_drained_files() middle failed"
    }

    ret = test_drained_files(dfd, fn, true, false)
    HasPosixErr.posixRequires((ret. z)) {
        "test_drained_files() linked failed"
    }

    io_uring_queue_exit(ring.ptr)
    posixClose(dfd)
    posixUnlink("/tmp/io_uring_openat_test")
    return
}

fun open_io_uring(ring: CPointer<io_uring>, dfd: Int, fn: String): Int = nativeHeap.run {

    val cqe: CPointerVar<io_uring_cqe> = alloc<CPointerVar<io_uring_cqe>>()
    var ret: Int
    val fd: Int

    val sqe: CPointer<io_uring_sqe> = io_uring_get_sqe(ring)!!

    io_uring_prep_openat(sqe, dfd, fn, OPEN_FLAGS, OPEN_MODE)

    ret = io_uring_submit(ring)
    HasPosixErr.posixRequires(!(ret < 0)) {
        "failed to submit openat: ${posix_strerror1(-ret)}"
    }

    ret = io_uring_wait_cqe(
        ring,
        cqe.ptr as CValuesRef<CPointerVar<io_uring_cqe> /* = kotlinx.cinterop.CPointerVarOf<kotlinx.cinterop.CPointer<linux_uring.io_uring_cqe>> */>
    )
    fd = cqe.pointed!!.res
    io_uring_cqe_seen(ring, cqe.value)
    HasPosixErr.posixRequires(ret >= 0) {
        "wait_cqe failed: " + posix_strerror1(-ret)

    }

    HasPosixErr.posixRequires(fd >= 0) {
        "io_uring openat failed: " + posix_strerror1(-fd)
    }

    posixClose(fd)
    return 0
}

fun prepare_file(dfd: Int, fn: String): Int {
    val buf = posixStrndup("foo", 4)
    var fd: Int
    var res: Int

    fd = openat(dfd, fn, OPEN_FLAGS, OPEN_MODE)
    HasPosixErr.posixRequires(!(fd < 0)) {
        "prepare/open: " + posix_strerror1(platform.posix.errno)

    }

    res = posixPwrite(fd, buf, 3, (1UL shl 32).toLong()).toInt()
    HasPosixErr.posixRequires(!(res < 0))
    { "prepare/pwrite: " + posix_strerror1(platform.posix.errno) }

    posixClose(fd)
    return res.takeIf { it < 0 } ?: 0
}

fun test_linked_files(dfd: Int, fn: String, async: Boolean): Int = nativeHeap.run {
    var ring: io_uring = alloc()
    var sqe: CPointer<io_uring_sqe>
    var buffer = posixMalloc(128.toULong())
    var iov: iovec = alloc {
        iov_base = buffer
        iov_len = 128.toULong()
    }
    var ret: Int
    val fd: Int
    var fds:CPointer<IntVar> = posixCalloc (Int.SIZE_BYTES.toULong(), 2.toULong())!!.reinterpret()

    ret = io_uring_queue_init(10, ring.ptr, 0)
    HasPosixErr.posixRequires(ret >= 0)
    { "failed to init io_uring: " + posix_strerror1(-ret) }
fprintf(platform.posix.stderr,"success with io_uring_queue_init")
    doPipe(fds)

    sqe = io_uring_get_sqe(ring.ptr) !!
    io_uring_prep_readv(sqe, fds [0]   , iov.ptr.reinterpret(), 1, 0)
    sqe.pointed.flags   = (sqe.pointed.flags + IOSQE_IO_LINK).toUByte()
    if (async)
        sqe.pointed.flags  = (sqe.pointed.flags+ IOSQE_ASYNC).toUByte()

    sqe = io_uring_get_sqe(ring.ptr)!!
    io_uring_prep_openat(sqe, dfd, fn, OPEN_FLAGS, OPEN_MODE)

    ret = io_uring_submit(ring.ptr)
    HasPosixErr.posixRequires(! (ret != 2)) {
        "failed to submit openat: "+ posix_strerror1(-ret)
    }
    fprintf(platform.posix.stderr,"success with io_uring_submit")

    fd = posixDup(ring.ring_fd)
   HasPosixErr.posixRequires( ! (fd < 0) ){
        "dup() failed: "+ posix_strerror1(-fd)
    }
    fprintf(platform.posix.stderr,"success with dup")

    /* io_uring.pointed.flush () */
    posixClose(fd)

    io_uring_queue_exit(ring.ptr)
    return 0
}

private fun doPipe(fds: CPointer<IntVar>) {
    HasPosixErr.posixRequires(posixPipe(fds).z) {
        ( ("pipe") )
    }
    fprintf(platform.posix.stderr, "success with pipe")
}

fun test_drained_files(dfd:Int, fn:String, linked:Boolean, prepend:Boolean):Int = nativeHeap.run{
	val    ring:io_uring =alloc()
   var sqe:CPointer<io_uring_sqe>
   val buffer= ByteArray(128)
    val iov:iovec = alloc{  iov_base =  buffer.pin().objcPtr().toLong().toCPointer()
		iov_len =128.toULong()
	}
    var ret:Int
	val fd:Int
	var to_cancel:Int = 0
	val fds=IntArray(2)

    ret = io_uring_queue_init(10, ring.ptr, 0)!!
    HasPosixErr.posixRequires(!(ret < 0))
	{ "failed to init io_uring: " + posix_strerror1(-ret )}

   	HasPosixErr.posixRequires(   (posixPipe(fds.refTo(0)).z) ){
        ("pipe")
    }

    sqe = io_uring_get_sqe(ring.ptr)!!
    io_uring_prep_readv(sqe, fds[0], iov.ptr.reinterpret(), 1, 0)
    sqe.pointed.user_data = 0.toULong()

    if (prepend) {
        sqe = io_uring_get_sqe(ring.ptr)!!
        io_uring_prep_nop(sqe)
		sqe.pointed.flags = sqe.pointed.flags.plus(IOSQE_IO_DRAIN).toUByte()
        to_cancel++
        sqe.pointed.user_data = to_cancel.toULong()
    }

    if (linked) {
        sqe = io_uring_get_sqe(ring.ptr)!!
        io_uring_prep_nop(sqe)
		sqe.pointed.flags = sqe.pointed.flags.plus(IOSQE_IO_DRAIN or IOSQE_IO_LINK).toUByte()
        to_cancel++
        sqe.pointed.user_data = to_cancel.toULong()
    }

    sqe = io_uring_get_sqe(ring.ptr)!!
    io_uring_prep_openat(sqe, dfd, fn, OPEN_FLAGS, OPEN_MODE)
	sqe.pointed.flags = sqe.pointed.flags.plus(IOSQE_IO_DRAIN).toUByte()
    to_cancel++
    sqe.pointed.user_data = to_cancel.toULong()


    ret = io_uring_submit(ring.ptr)
    HasPosixErr.posixRequires(! (ret != 1 + to_cancel) )
		{ "failed to submit openat: " + posix_strerror1(-ret) }

    fd = posixDup(ring.ring_fd)
HasPosixErr.posixRequires(! (fd < 0))
		{ "dup() failed: " + posix_strerror1(-fd) }

    /*
     * close(), which triggers.pointed.flush (), and io_uring_queue_exit()
     * should successfully return and not hang.
     */
    posixClose(fd)
    io_uring_queue_exit(ring.ptr)
    return 0
}
