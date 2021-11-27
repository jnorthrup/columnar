package linux_uring.include

import kotlinx.cinterop.*
import linux_uring.*
import platform.posix.ENOMEM
import platform.posix.EPERM
import platform.posix.O_CREAT
import platform.posix.O_WRONLY
import platform.posix.calloc
import platform.posix.close
import platform.posix.fprintf
import platform.posix.free
import platform.posix.fsync
import platform.posix.geteuid
import platform.posix.iovec
import platform.posix.malloc
import platform.posix.memset
import platform.posix.open
import platform.posix.posix_memalign
import platform.posix.size_t
import platform.posix.stderr
import platform.posix.stdout
import platform.posix.strerror
import platform.posix.write
import simple.simple.CZero.nz

//import test.update.p

/* SPDX-License-Identifier: MIT */
/*
 * Description: Helpers for tests.
 */
//#include <stdlib.h>
//#include <assert.h>
//#include <string.h>
//#include <stdio.h>
//#include <fcntl.h>
//#include <unistd.h>
//#include <sys/types.h>
//
//#include "helpers.h"
//#include "liburing.h"

/*
 * Helper for allocating memory in tests.
 */
fun t_malloc(size: size_t): CPointer<ByteVar> {
    val ret = malloc(size)
    return ret!!.reinterpret()
}

/*
 * Helper for allocating size bytes aligned on a boundary.
 */
fun t_posix_memalign(memptr: CValuesRef<COpaquePointerVar>, alignment: size_t, size: size_t) {

    posix_memalign(memptr, alignment, size)
}

/*
 * Helper for allocating space for an array of nmemb elements
 * with size bytes for each element.
 */
fun t_calloc(nmemb: size_t, size: size_t): CPointer<ByteVar> {

    val ret = calloc(nmemb, size)
    return ret!!.reinterpret()
}

/*
 * Helper for creating file and write @size byte buf with 0xaa value in the file.
 */
fun t_create_file(file: String, size: size_t): Unit {


    var buf = t_malloc(size)
    memset(buf, 0xaa, size)

    var fd = open(file, O_WRONLY or O_CREAT, 644.fromOctal())
    assert(fd >= 0)

    var ret = write(fd, buf, size)
    fsync(fd)
    close(fd)
    free(buf)
}

/*
 * Helper for creating @buf_num number of iovec
 * with @buf_size bytes buffer of each iovec.
 */
fun t_create_buffers(buf_num: size_t, buf_size: size_t): CPointer<iovec> {

    val times = buf_num.toInt()
    val ar: CPointer<iovec> = nativeHeap.allocArray<iovec>(times)
    repeat(times){   i->
        val iovec = ar[i]
        val alloc = nativeHeap.alloc(buf_size.toLong(), buf_size.toInt())
        iovec.iov_base= alloc.reinterpret()!!
        iovec.iov_len=buf_size
    }
    return ar

}

/*
 * Helper for setting up an io_uring instance, skipping if the given user isn't
 * allowed to.
 */
fun t_create_ring_params(depth: Int, ring: CPointer<io_uring>, p: CPointer<io_uring_params>): t_setup_ret {


    val ret = io_uring_queue_init_params(depth.toUInt(), ring, p)
    if (!ret.nz)
        return T_SETUP_OK

    when {
        (p.pointed.flags and IORING_SETUP_SQPOLL).nz && (ret == -EPERM) && geteuid().nz -> {
            fprintf(stdout, "SQPOLL skipped for regular user\n")
            return T_SETUP_SKIP
        }
        /* = kotlin.UInt */
        else -> {
            fprintf(stderr, "queue_init: %s\n", strerror(-ret))
            return ret.toUInt()
        }
    }
}

fun t_create_ring(depth: Int, ring: CPointer<io_uring>, flags: UInt): t_setup_ret {
    val p: io_uring_params = nativeHeap.alloc()

    p.flags = flags
    return t_create_ring_params(depth, ring, p.ptr)
}

fun t_register_buffers(ring: CPointer<io_uring>, iovecs: CPointer<iovec>, nr_iovecs: UInt): t_setup_ret {

    val ret = io_uring_register_buffers(ring, iovecs.reinterpret<linux_uring.iovec>() as CValuesRef<linux_uring.iovec>, nr_iovecs)
    when {
        !ret.nz -> return T_SETUP_OK
        (ret == -EPERM || ret == -ENOMEM) && geteuid().nz -> {
            fprintf(stdout, "too large non-root buffer registration, skip\n")
            return T_SETUP_SKIP
        }
        else -> {
            fprintf(stderr, "buffer register failed: %s\n", strerror(-ret))
            return ret.toUInt()
        }
    }

}
