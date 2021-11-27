package test.update

import kotlinx.cinterop.*
import linux_uring.*
import linux_uring.EAGAIN
import linux_uring.EINVAL
import linux_uring.ENOENT
import linux_uring.EPERM
import linux_uring.F_SETFL
import linux_uring.O_NONBLOCK
import linux_uring.errno
import linux_uring.exit
import linux_uring.fcntl
import linux_uring.fprintf
import linux_uring.perror
import linux_uring.pipe
import linux_uring.rand
import linux_uring.read
import linux_uring.stderr
import linux_uring.stdout
import linux_uring.write
import platform.posix.*
import platform.posix.__u64Var
import platform.posix.pthread_tVar
import simple.HasPosixErr
import simple.simple.CZero.nz
import kotlin.native.concurrent.AtomicReference
import kotlin.native.concurrent.ensureNeverFrozen
import platform.posix.pthread_create as posixPthread_create
import platform.posix.pthread_join as posixPthread_join

/* SPDX-License-Identifier: MIT */
/*
 * Description: test many files being polled for and updated
 *
 */
//#include <errno.h>
//#include <stdio.h>
//#include <unistd.h>
//#include <stdlib.h>
//#include <string.h>
//#include <signal.h>
//#include <sys/poll.h>
//#include <sys/resource.h>
//#include <fcntl.h>
//#include <pthread.h>
//
//#include "liburing.h"

const val NFILES: Int = 5000
const val BATCH: Int = 500
const val NLOOPS: Int = 1000
const val RING_SIZE: Int = 512

class p_clz {
    var fd: IntArray = IntArray(2)
    var triggered: Int = 0
}
val p =AtomicReference<Array<p_clz>>( Array(NFILES) { p_clz() }.also{it.ensureNeverFrozen()})


fun has_poll_update(): Int = memScoped {
    val ring: io_uring = alloc()
    var has_update = 0


    var ret = io_uring_queue_init(8, ring.ptr, 0)
    if (ret.nz)
        return -1

    val sqe = io_uring_get_sqe(ring.ptr)!!
    io_uring_prep_poll_update(sqe, NULL, NULL, POLLIN, IORING_TIMEOUT_UPDATE)

    ret = io_uring_submit(ring.ptr)
    if (ret != 1)
        return -1
    val cqe: CPointerVar<io_uring_cqe> = alloc()
    ret = io_uring_wait_cqe(ring.ptr, cqe.ptr.reinterpret())
    if (!ret.nz) {
        if (cqe.value!!.pointed.res == -ENOENT)
            has_update = 1
        else if (cqe.value!!.pointed.res != -EINVAL)
            return -1
        io_uring_cqe_seen(ring.ptr, cqe.value!!.pointed.ptr)
    }
    io_uring_queue_exit(ring.ptr)
    return has_update
}

fun arm_poll(ring: CPointer<io_uring>, off: Int): Int {


    val sqe = io_uring_get_sqe(ring)!!



    io_uring_prep_poll_multishot(sqe, p.value[off].fd[0], POLLIN)
    sqe.pointed.user_data = off.toULong()

    return 0
}

fun reap_polls(ring: CPointer<io_uring>): Int = memScoped {
    val cqe: CPointerVar<io_uring_cqe> = alloc()
    val c: ByteVar = alloc()


    for (i in 0 until BATCH) {

        val prev: __u64Var = alloc { value = i.toULong() }

        val sqe = io_uring_get_sqe(ring)!!
        /* update event */
        io_uring_prep_poll_update(
            sqe, (prev).ptr, NULL,
            POLLIN, IORING_POLL_UPDATE_EVENTS
        )
        sqe.pointed.user_data = 0x12345678u
    }

    var ret = io_uring_submit(ring)
    if (ret != BATCH) {
        fprintf(stderr, "submitted %d, %d\n", ret, BATCH)
        return 1
    }

    var seen = false
    var track = 0
    for (i in 0 until 2 * BATCH) {
        track = i
        ret = io_uring_wait_cqe(ring, cqe.ptr)
        if (ret.nz) {
            fprintf(stderr, "wait cqe %d\n", ret)
            return ret
        }
        val off = cqe.pointed!!.user_data

        if (off != 0x12345678UL) {
            ret = read(p.value[off.toInt()].fd[0], c.ptr, 1).toInt()
            if (ret != 1) {
                if (ret == -1 && errno == EAGAIN) {
                    //goto seen
                } else {
                    fprintf(stderr, "read got %d/%d\n", ret, errno)
                    break
                }
            }
        }
        io_uring_cqe_seen(ring, cqe.value)
    }

    if (track != 2 * BATCH) {
        fprintf(stderr, "gave up at %d\n", track)
        return 1
    }

    return 0
}

fun trigger_polls(): Int = memScoped {
    val c: ByteVar = alloc { value = 89 }
    for (i in 0 until BATCH) {

        var off: Int

        do {
            off = rand() % NFILES
            if (!p.value[off].triggered.nz)
                break
        } while (1.nz)

        p.value[off].triggered = 1
        val ret = write(p.value[off].fd[1], c.ptr, 1)
        if (ret != 1.toLong()) {
            fprintf(stderr, "write got %d/%d\n", ret, errno)
            return 1
        }
    }

    return 0
}

fun trigger_polls_fn(data: COpaquePointer?): COpaquePointer? {
    return null.also { trigger_polls() }
}

fun arm_polls(ring: CPointer<io_uring>): Int {
    var ret: Int
    var to_arm = NFILES

    var off = 0
    while (to_arm.nz) {
        var this_arm = to_arm
        if (this_arm > RING_SIZE)
            this_arm = RING_SIZE

        for (i in 0 until this_arm) {
            if (arm_poll(ring, off).nz) {
                fprintf(stderr, "arm failed at %d\n", off)
                return 1
            }
            off++
        }

        ret = io_uring_submit(ring)
        if (ret != this_arm) {
            fprintf(stderr, "submitted %d, %d\n", ret, this_arm)
            return 1
        }
        to_arm -= this_arm
    }

    return 0
}

fun main(): Unit = memScoped {
    val ring: io_uring = alloc()
    val params: io_uring_params = alloc()
    val rlim: rlimit = alloc()
    val thread: pthread_tVar = alloc()
    fun err(): Int {
        io_uring_queue_exit(ring.ptr)
        return -1
    }

    fun err_noring(): Int {
        fprintf(stderr, "poll-many failed\n")
        return 1
    }

    fun err_nofail(): Int { fprintf(stderr, "poll-many: not enough files available (and not root),  skipped\n"); return 0 }


    var ret = has_poll_update()
    HasPosixErr.posixFailOn(ret < 0) {
        fprintf(stderr, "poll update check failed %i\n", ret)

    }
    if (!ret.nz) {
        fprintf(stderr, "no poll update, skip\n")
        return
    }

    if (getrlimit(RLIMIT_NOFILE, rlim.ptr) < 0) {
        perror("getrlimit")
        goto(::err_noring)
    }

    if (rlim.rlim_cur < ((2 * NFILES + 5).toUInt())) {
        rlim.rlim_cur = ((2 * NFILES + 5).toULong())
        rlim.rlim_max = rlim.rlim_cur
        if (setrlimit(RLIMIT_NOFILE, rlim.ptr) < 0) {
            if (errno == EPERM)
                goto(::err_nofail)
            perror("setrlimit")
            goto(::err_noring)
        }
    }

    for (i in 0 until NFILES) {
        if (pipe(p.value[i].fd.toCValues()) < 0) {
            perror("pipe")
            goto(::err_noring)
        }
        fcntl(p.value[i].fd[0], F_SETFL, O_NONBLOCK)
    }

    params.flags = IORING_SETUP_CQSIZE
    params.cq_entries = 4096.toUInt()
    ret = io_uring_queue_init_params(RING_SIZE.toUInt(), ring.ptr, params.ptr)
    if (ret.nz) {
        if (ret == -EINVAL) {
            fprintf(stdout, "No CQSIZE, trying without\n")
            ret = io_uring_queue_init(RING_SIZE.toUInt(), ring.ptr, 0)
            if (ret.nz) {
                fprintf(stderr, "ring setup failed: %d\n", ret)
                goto(::err)
            }
        }
    }

    if (arm_polls(ring.ptr).nz)
        goto(::err)

    for (i in 0 until NLOOPS) {
        posixPthread_create(thread.ptr, null, staticCFunction(::trigger_polls_fn), NULL)
        ret = reap_polls(ring.ptr)
        if (ret.nz)
            goto(::err)
        posixPthread_join(thread.value, null)

        for (j in 0 until NFILES)
            p.value[j].triggered = 0
    }

    io_uring_queue_exit(ring.ptr)
    return

}

private infix fun NativePlacement.goto(f: () -> Int) = exit(f())
