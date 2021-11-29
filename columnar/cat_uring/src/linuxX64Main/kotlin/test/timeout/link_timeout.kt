package test.timeout

import kotlinx.cinterop.*
import linux_uring.*
import linux_uring.include.UringSqeFlags.sqeIo_link
import platform.posix.POLLIN
import simple.simple.CZero.nz
import test.timeout.TimeoutAppState.Companion.end.err

/* SPDX-License-Identifier: MIT */
/*
 * Description: run various linked timeout cases
 *
 */
//#include <errno.h>
//#include <stdio.h>
//#include <unistd.h>
//#include <stdlib.h>
//#include <string.h>
//#include <fcntl.h>
//#include <sys/poll.h>
//
//#include "liburing.h"

class TimeoutAppState : NativePlacement by nativeHeap {
    private fun test_fail_lone_link_timeouts(ring: CPointer<io_uring>): Int {
        val ts: __kernel_timespec = alloc()


        var ret: Int
        val sqe = io_uring_get_sqe(ring)!!
        var goto: end? = null; do {

            io_uring_prep_link_timeout(sqe, ts.ptr, 0)
            ts.tv_sec = 1
            ts.tv_nsec = 0
            val sqeref = sqe.pointed
            sqeref.user_data = 1.toULong()
            sqeref.flags = (sqeref.flags + sqeIo_link.ub).toUByte()

            ret = io_uring_submit(ring)
            if (ret != 1) {
                printf("sqe submit failed: %d\n", ret)
                goto = err;break
            }
            val cqe: CPointerVar<io_uring_cqe> = alloc()

            ret = io_uring_wait_cqe(ring, cqe.ptr)
            if (ret < 0) {
                printf("wait completion %d\n", ret)
                goto = err;break
            }

            val pointed = cqe.pointed!!
            if (pointed.user_data != 1.toULong()) {
                fprintf(stderr, "invalid user data %d\n", pointed.res)
                goto = err;break
            }

            if (pointed.res != -EINVAL) {
                fprintf(stderr, "got %d, wanted -EINVAL\n", pointed.res)
                goto = err;break
            }

            io_uring_cqe_seen(ring, cqe.value)
        } while (false)

        return goto?.let { 1 } ?: 0
    }

    private fun test_fail_two_link_timeouts(ring: CPointer<io_uring>): Int {
        val ts: __kernel_timespec = alloc {
            tv_sec = 1
            tv_nsec = 0
        }

        val cqe: CPointerVar<io_uring_cqe> = alloc()
        val sqe: CPointerVar<io_uring_sqe> = alloc()


        /*
         * sqe_1: write destined to fail
         * use buf=NULL, to do that during the issuing stage
         */
        sqe.value = io_uring_get_sqe(ring)!!

        io_uring_prep_writevFail(sqe.value, 0, 0L.toCPointer()   , 1, 0)
        sqe.pointed!!.flags = sqe.pointed!!.flags.or(IOSQE_IO_LINK.toUByte())
        sqe.pointed!!.user_data = 1uL


        /* sqe_2: valid linked timeout */
        sqe.value = io_uring_get_sqe(ring)

        io_uring_prep_link_timeout(sqe.value,   ts.ptr, 0)
        sqe.pointed!!.flags = sqe.pointed!!.flags.or(IOSQE_IO_LINK.toUByte())
        sqe.pointed!!.user_data = 2uL


        /* sqe_3: invalid linked timeout */
        sqe.value = io_uring_get_sqe(ring)
        io_uring_prep_link_timeout(sqe.value,   ts.ptr, 0)
        sqe.pointed!!.flags = sqe.pointed!!.flags.or(IOSQE_IO_LINK.toUByte())
        sqe.pointed!!.user_data = 3uL

        /* sqe_4: invalid linked timeout */
        sqe.value = io_uring_get_sqe(ring)
        io_uring_prep_link_timeout(sqe.value,  ts.ptr, 0)
        sqe.pointed!!.flags = sqe.pointed!!.flags.or(IOSQE_IO_LINK.toUByte())
        sqe.pointed!!.user_data = 4uL

        var ret = io_uring_submit(ring)
        if (ret < 3) {
            printf("sqe submit failed: %d\n", ret)
            return 1
        }
        val nr_wait = ret

        for (i in 0 until  nr_wait ) {
            ret = io_uring_wait_cqe(ring,   cqe.ptr)
            if (ret < 0) {
                printf("wait completion %d\n", ret)
                return 1
            }

            when (cqe.pointed!!.user_data.toInt()) {
                1->            if (cqe.pointed!!.res != -EFAULT && cqe.pointed!!.res != -ECANCELED) {
                    fprintf(
                        stderr, "write got %d, wanted -EFAULT or -ECANCELED\n", cqe.pointed!!.res)
                    return 1
                }
                2-> if (cqe.pointed!!.res != -ECANCELED) { fprintf(stderr, "Link timeout got %d, wanted -ECACNCELED\n", cqe.pointed!!.res)
                    return 1
                }
                3,/* fall through */   4->            if (cqe.pointed!!.res != -ECANCELED && cqe.pointed!!.res != -EINVAL) {
                    fprintf(
                        stderr, "Invalid link timeout got %d, wanted -ECACNCELED || -EINVAL\n",cqe.pointed!!.res )
                    return 1        }
            }; io_uring_cqe_seen(ring, cqe.value) }

        return 0
    }

    /*
     * Test linked timeout with timeout (timeoutception)
     */
    private fun test_single_link_timeout_ception(ring: CPointer<io_uring>): Int {
        val ts1: __kernel_timespec = alloc()
        val ts2: __kernel_timespec = alloc()
        val cqe: CPointerVar<io_uring_cqe> = alloc()

        var goto: end? = null;do {
            var sqe = io_uring_get_sqe(ring)
            if (null == sqe) {
                printf("get sqe failed\n")
                goto = err;break
            }

            ts1.tv_sec = 1
            ts1.tv_nsec = 0
            val count = (-1).toUInt()
            io_uring_prep_timeout(sqe, ts1.ptr, count, 0)
            sqe.pointed.flags = sqe.pointed.flags.or(sqeIo_link.ub)
            sqe.pointed.user_data = 1u

            sqe = io_uring_get_sqe(ring)
            if (null == sqe) {
                printf("get sqe failed\n")
                goto = err;break
            }

            ts2.tv_sec = 2
            ts2.tv_nsec = 0
            io_uring_prep_link_timeout(sqe, ts2.ptr, 0)
            sqe.pointed.user_data = 2u

            var ret = io_uring_submit(ring)
            if (ret != 2) {
                printf("sqe submit failed: %d\n", ret)
                goto = err;break
            }

            for (i in 0 until 2) {
                ret = io_uring_wait_cqe(ring, cqe.ptr)
                if (ret < 0) {
                    printf("wait completion %d\n", ret)
                    goto = err;break
                }
                val pointed = cqe.pointed!!
                when (pointed.user_data.toInt()) {
                    1 -> {
                        /* newer kernels allow timeout links */
                        if (pointed.res != -EINVAL && pointed.res != -ETIME) {
                            fprintf(
                                stderr, "Timeout got %d, wanted -EINVAL or -ETIME\n", pointed.res
                            )
                            goto = err;break
                        }
                    }
                    2 ->

                        if (pointed.res != -ECANCELED) {
                            fprintf(stderr, "Link timeout got %d, wanted -ECANCELED\n", pointed.res)
                            goto = err;break
                        }
                }; io_uring_cqe_seen(ring, cqe.value)
            }
        } while (false)



        return goto?.let { 1 } ?: 0
    }

    /*
     * Test linked timeout with NOP
     */
    private fun test_single_link_timeout_nop(ring: CPointer<io_uring>): Int {
        val ts: __kernel_timespec = alloc()
        val cqe: CPointerVar<io_uring_cqe> = alloc()
        var ret: Int
        var goto: end? = null;do {
            var sqe = io_uring_get_sqe(ring)!!

            io_uring_prep_nop(sqe)
            sqe.pointed.flags = sqe.pointed.flags.or(sqeIo_link.ub)
            sqe.pointed.user_data = 1u

            sqe = io_uring_get_sqe(ring)!!

            ts.tv_sec = 1
            ts.tv_nsec = 0
            io_uring_prep_link_timeout(sqe, ts.ptr, 0)
            sqe.pointed.user_data = 2u

            ret = io_uring_submit(ring)
            if (ret != 2) {
                printf("sqe submit failed: %d\n", ret)
                goto = err;break
            }

            for (i in 0 until 2) {
                ret = io_uring_wait_cqe(ring, cqe.ptr)
                if (ret < 0) {
                    printf("wait completion %d\n", ret)
                    goto = err;break
                }
                val pointed1 = cqe.pointed!!
                when (pointed1.user_data.toInt()) {
                    1 ->
                        if (pointed1.res.nz) {
                            fprintf(stderr, "NOP got %d, wanted 0\n", pointed1.res)
                            goto = err;break
                        }
                    2 -> if (pointed1.res != -ECANCELED) {
                        fprintf(stderr, "Link timeout got %d, wanted -ECACNCELED\n", pointed1.res)
                        goto = err;break
                    }
                }; io_uring_cqe_seen(ring, cqe.value)
            }
        } while (false)
        return goto?.let { 1 } ?: 0
    }

    /*
     * Test read that will not complete, with a linked timeout behind it that
     * has errors in the SQE
     */
    private fun test_single_link_timeout_error(ring: CPointer<io_uring>): Int = memScoped {
        val ts: __kernel_timespec = alloc()
        val cqe: CPointerVar<io_uring_cqe> = alloc()
        val iov: iovec = alloc()
        val fds = IntArray(2)
        val buffer = ByteArray(256)


        if (pipe(fds.refTo(0)).nz) {
            perror("pipe")
            return 1
        }
        var goto: end? = null; do {
        var sqe = io_uring_get_sqe(ring)!!
        iov.iov_base = buffer.refTo(0).getPointer(this)
        iov.iov_len = buffer.size.toULong()
        io_uring_prep_readv(sqe, fds[0], iov.ptr, 1, 0)
        sqe.pointed.flags = sqe.pointed.flags.or(sqeIo_link.ub)
        sqe.pointed.user_data = 1u

        sqe = io_uring_get_sqe(ring)!!
        ts.tv_sec = 1
        ts.tv_nsec = 0
        io_uring_prep_link_timeout(sqe, ts.ptr, 0)
        /* set invalid field, it'll get failed */
        sqe.pointed.ioprio = 89u
        sqe.pointed.user_data = 2u

        var ret = io_uring_submit(ring)
        if (ret != 2) {
            printf("sqe submit failed: %d\n", ret)
            goto = err;break
        }

        for (i in 0 until 2) {
            ret = io_uring_wait_cqe(ring, cqe.ptr)
            if (ret < 0) {
                printf("wait completion %d\n", ret)
                goto = err;break
            }
            val pointed = cqe.pointed!!
            when (pointed.user_data.toInt()) {
                1 ->
                    if (pointed.res != -ECANCELED) {
                        fprintf(
                            stderr, "Read got %d, wanted -ECANCELED\n",
                            pointed.res
                        )
                        goto = err;break
                    }
                2 ->
                    if (pointed.res != -EINVAL) {
                        fprintf(stderr, "Link timeout got %d, wanted -EINVAL\n", pointed.res)
                        goto = err;break
                    }
            }; io_uring_cqe_seen(ring, cqe.value)
        }
    } while (false);return goto?.let { 1 } ?: 0
    }

    /*
     * Test read that will complete, with a linked timeout behind it
     */
    private fun test_single_link_no_timeout(ring: CPointer<io_uring>): Int = memScoped {
        val ts: __kernel_timespec = alloc()
        val cqe: CPointerVar<io_uring_cqe> = alloc()
        val fds = IntArray(2)
        val iov: iovec = alloc()
        val buffer = ByteArray(256)

        if (pipe(fds.refTo(0)).nz) {
            perror("pipe")
            return 1
        }
        var goto: end? = null;do {

        var sqe = io_uring_get_sqe(ring)!!


        iov.iov_base = buffer.toCValues().getPointer(this)
        iov.iov_len = buffer.size.toULong()
        io_uring_prep_readv(sqe, fds[0], iov.ptr, 1, 0)
        sqe.pointed.flags = sqe.pointed.flags.or(sqeIo_link.ub)
        sqe.pointed.user_data = 1u

        sqe = io_uring_get_sqe(ring)!!


        ts.tv_sec = 1
        ts.tv_nsec = 0
        io_uring_prep_link_timeout(sqe, ts.ptr, 0)
        sqe.pointed.user_data = 2u

        sqe = io_uring_get_sqe(ring)!!

        iov.iov_base = buffer.refTo(0).getPointer(this)
        iov.iov_len = (buffer).size.toULong()
        io_uring_prep_writev(sqe, fds[1], iov.ptr, 1, 0)
        sqe.pointed.user_data = 3u

        var ret = io_uring_submit(ring)
        if (ret != 3) {
            printf("sqe submit failed: %d\n", ret)
            goto = err;break
        }

        for (i in 0 until 3) {
            ret = io_uring_wait_cqe(ring, cqe.ptr)
            if (ret < 0) {
                printf("wait completion %d\n", ret)
                goto = err;break
            }
            val pointed = cqe.pointed!!
            when (pointed.user_data.toInt()) {
                1, 3 -> if (pointed.res != (buffer.size)) {
                    fprintf(
                        stderr, "R/W got %d, wanted %d\n", pointed.res,
                        (buffer).size
                    )
                    goto = err;break
                }
                2 -> if (pointed.res != -ECANCELED) {
                    fprintf(
                        stderr, "Link timeout %d, wanted -ECANCELED ${-ECANCELED}\n",
                        pointed.res
                    )
                    goto = err;break
                }
            }; io_uring_cqe_seen(ring, cqe.value)
        }

    } while (false)
        close(fds[0])
        close(fds[1])
        return goto?.let { 1 } ?: 0
    }

    /*
     * Test read that will not complete, with a linked timeout behind it
     */
    private fun test_single_link_timeout(ring: CPointer<io_uring>, nsec: UInt): Int = memScoped {
        val ts: __kernel_timespec = alloc()
        val cqe: CPointerVar<io_uring_cqe> = alloc()
        val fds = IntArray(2)
        val iov: iovec = alloc()

        if (pipe(fds.refTo(0)).nz) {
            perror("pipe")
            return 1
        }
        var goto: end? = null;do {

        io_uring_get_sqe(ring)!!.let { sqe ->

            val buffer = ByteArray(256)
            iov.iov_base = buffer.refTo(0).getPointer(this)
            iov.iov_len = (buffer).size.toULong()
            io_uring_prep_readv(sqe, fds[0], iov.ptr, 1, 0)
            val pointed: io_uring_sqe = sqe.pointed
            pointed.flags = pointed.flags.or(sqeIo_link.ub)
            pointed.user_data = 1u
        }
        io_uring_get_sqe(ring)!!.let { sqe ->


            ts.tv_sec = 0
            ts.tv_nsec = nsec.toLong()
            io_uring_prep_link_timeout(sqe, ts.ptr, 0)
            sqe.pointed.user_data = 2u
        }
        var ret = io_uring_submit(ring)
        if (ret != 2) {
            printf("sqe submit failed: %d\n", ret)
            goto = err;break
        }

        for (i in 0 until 2) {
            ret = io_uring_wait_cqe(ring, cqe.ptr)
            if (ret < 0) {
                printf("wait completion %d\n", ret)
                goto = err;break
            }
            val pointed = cqe.pointed!!
            when (pointed.user_data.toInt()) {
                1 -> if (pointed.res != -EINTR && pointed.res != -ECANCELED) {
                    fprintf(stderr, "Read got %d\n", pointed.res)
                    goto = err;break
                }
                2 -> if (pointed.res != -EALREADY && pointed.res != -ETIME && pointed.res != 0) {
                    fprintf(stderr, "Link timeout got %d\n", pointed.res)
                    goto = err;break
                }
            }; io_uring_cqe_seen(ring, cqe.value)
        }
    } while (false)
        close(fds[0])
        close(fds[1])
        return goto?.let { 1 } ?: 0
    }

    private fun test_timeout_link_chain1(ring: CPointer<io_uring>): Int = memScoped {
        val ts: __kernel_timespec = alloc()
        val cqe: CPointerVar<io_uring_cqe> = alloc()
        val fds = IntArray(2)
        val iov: iovec = alloc()


        if (pipe(fds.refTo(0)).nz) {
            perror("pipe")
            return 1
        }
        println(" test_timeout_link_chain1 pipe using fds ${fds.toList()}")
        var goto: end? = null;do {

        var sqe = io_uring_get_sqe(ring)!!
        val buffer = ByteArray(256)
        iov.iov_base = buffer.refTo(0).getPointer(this)
        iov.iov_len = (buffer).size.toULong()
        io_uring_prep_readv(sqe, fds[0], iov.ptr, 1, 0)
        sqe.pointed.flags = sqe.pointed.flags.or(sqeIo_link.ub)
        sqe.pointed.user_data = 1u

        sqe = io_uring_get_sqe(ring)!!
        ts.tv_sec = 0
        ts.tv_nsec = 1000000
        io_uring_prep_link_timeout(sqe, ts.ptr, 0)
        sqe.pointed.flags = sqe.pointed.flags.or(sqeIo_link.ub)
        sqe.pointed.user_data = 2U

        sqe = io_uring_get_sqe(ring)!!

        io_uring_prep_nop(sqe)
        sqe.pointed.user_data = 3U

        var ret = io_uring_submit(ring)
        if (ret != 3) {
            printf("sqe submit failed: %d\n", ret)
            goto = err;break
        }

        for (i in 0 until 3) {
            ret = io_uring_wait_cqe(ring, cqe.ptr)
            if (ret < 0) {
                printf("wait completion %d\n", ret)
                goto = err;break
            }
            val ioUringCqe = cqe.pointed!!
            when (ioUringCqe.user_data.toInt()) {
                1 -> if (ioUringCqe.res != -EINTR && ioUringCqe.res != -ECANCELED) {
                    fprintf(
                        stderr, "Req  -EINTR ${-EINTR} or -ECANCELED ${-ECANCELED}? got %d\n", ioUringCqe.res
                    )
                    goto = err;break
                }
                2 -> if (ioUringCqe.res != -EALREADY && ioUringCqe.res != -ETIME) {
                    fprintf(
                        stderr, "Req -EALREADY  ${-EALREADY} or -ETIME ${-ETIME} got %d\n", ioUringCqe.user_data,
                        ioUringCqe.res
                    )
                    goto = err;break
                }
                3 -> if (ioUringCqe.res != -ECANCELED) {
                    fprintf(
                        stderr, "Req  ECANCELED $ECANCELED ? got %d\n", ioUringCqe.user_data,
                        ioUringCqe.res
                    )
                    goto = err;break
                }
            }
            io_uring_cqe_seen(ring, cqe.value)
        }
    } while (false)
        close(fds[0])
        close(fds[1])
        return goto?.let { 1 } ?: 0
    }

    private fun test_timeout_link_chain2(ring: CPointer<io_uring>): Int = memScoped {
        val ts: __kernel_timespec = alloc()
        val cqe: CPointerVar<io_uring_cqe> = alloc()
        val fds = IntArray(2)

        if (pipe(fds.refTo(0)).nz) {
            perror("pipe")
            return 1
        }
        var goto: end? = null;do {

        var sqe = io_uring_get_sqe(ring)!!

        io_uring_prep_poll_add(sqe, fds[0], POLLIN)
        sqe.pointed.flags = sqe.pointed.flags.or(sqeIo_link.ub)
        sqe.pointed.user_data = 1u
        sqe = io_uring_get_sqe(ring)!!

        ts.tv_sec = 0
        ts.tv_nsec = 1000000
        io_uring_prep_link_timeout(sqe, ts.ptr, 0)
        sqe.pointed.flags = sqe.pointed.flags.or(sqeIo_link.ub)
        sqe.pointed.user_data = 2u

        sqe = io_uring_get_sqe(ring)!!
        io_uring_prep_nop(sqe)
        sqe.pointed.flags = sqe.pointed.flags.or(sqeIo_link.ub)
        sqe.pointed.user_data = 3u

        sqe = io_uring_get_sqe(ring)!!
        io_uring_prep_nop(sqe)
        sqe.pointed.user_data = 4u

        var ret = io_uring_submit(ring)
        if (ret != 4) {
            printf("sqe submit failed: %d\n", ret)
            goto = err;break
        }

        for (i in 0 until 4) {
            ret = io_uring_wait_cqe(ring, cqe.ptr)
            if (ret < 0) {
                printf("wait completion %d\n", ret)
                goto = err;break
            }
            val pointed = cqe.pointed!!
            when (pointed.user_data.toInt()) {
                /* poll cancel really should return -ECANCEL... */
                1 -> if (pointed.res != -ECANCELED) {
                    fprintf(
                        stderr, "Req ECANCELED ${-ECANCELED} ? got %d\n", pointed.user_data,
                        pointed.res
                    )
                    goto = err;break
                }
                2 -> if (pointed.res != -ETIME) {
                    fprintf(
                        stderr, "Req  ETIME ${-ETIME} ? got %d\n", pointed.user_data,
                        pointed.res
                    )
                    goto = err;break
                }
                3, 4 ->
                    if (pointed.res != -ECANCELED) {
                        fprintf(
                            stderr, "Req   ECANCELED $ECANCELED ? got %d\n", pointed.user_data,
                            pointed.res
                        )
                        goto = err;break
                    }
            }; io_uring_cqe_seen(ring, cqe.value)
        }
    } while (false)
        close(fds[0])
        close(fds[1])
        return goto?.let { 1 } ?: 0
    }

    private fun test_timeout_link_chain3(ring: CPointer<io_uring>): Int = memScoped {
        val ts: __kernel_timespec = alloc()
        val cqe: CPointerVar<io_uring_cqe> = alloc()
        val fds = IntArray(2)

        if (pipe(fds.refTo(0)).nz) {
            perror("pipe")
            return 1
        }
        var goto: end? = null;do {

        var sqe = io_uring_get_sqe(ring)
        if (null == sqe) {
            printf("get sqe failed\n")
            goto = err;break
        }
        io_uring_prep_poll_add(sqe, fds[0], POLLIN)
        sqe.pointed.flags = sqe.pointed.flags.or(sqeIo_link.ub)
        sqe.pointed.user_data = 1u

        sqe = io_uring_get_sqe(ring)
        if (null == sqe) {
            printf("get sqe failed\n")
            goto = err;break
        }
        ts.tv_sec = 0
        ts.tv_nsec = 1000000
        io_uring_prep_link_timeout(sqe, ts.ptr, 0)
        sqe.pointed.flags = sqe.pointed.flags.or(sqeIo_link.ub)
        sqe.pointed.user_data = 2u

        sqe = io_uring_get_sqe(ring)
        if (null == sqe) {
            printf("get sqe failed\n")
            goto = err;break
        }
        io_uring_prep_nop(sqe)
        sqe.pointed.flags = sqe.pointed.flags.or(sqeIo_link.ub)
        sqe.pointed.user_data = 3u

        /* POLL.pointed.TIMEOUT  -> NOP */

        sqe = io_uring_get_sqe(ring)
        if (null == sqe) {
            printf("get sqe failed\n")
            goto = err;break
        }
        io_uring_prep_poll_add(sqe, fds[0], POLLIN)
        sqe.pointed.flags = sqe.pointed.flags.or(sqeIo_link.ub)
        sqe.pointed.user_data = 4u

        sqe = io_uring_get_sqe(ring)
        if (null == sqe) {
            printf("get sqe failed\n")
            goto = err;break
        }
        ts.tv_sec = 0
        ts.tv_nsec = 1000000
        io_uring_prep_link_timeout(sqe, ts.ptr, 0)
        sqe.pointed.user_data = 5u

        /* poll on pipe + timeout */

        sqe = io_uring_get_sqe(ring)
        if (null == sqe) {
            printf("get sqe failed\n")
            goto = err;break
        }
        io_uring_prep_nop(sqe)
        sqe.pointed.user_data = 6u

        /* nop */

        var ret = io_uring_submit(ring)
        if (ret != 6) {
            printf("sqe submit failed: %d\n", ret)
            goto = err;break
        }

        for (i in 0 until 6) {
            ret = io_uring_wait_cqe(ring, cqe.ptr)
            if (ret < 0) {
                printf("wait completion %d\n", ret)
                goto = err;break
            }
            val pointed = cqe.pointed!!
            when (pointed.user_data.toInt()) {
                2 -> if (pointed.res != -ETIME) {
                    fprintf(
                        stderr, "Req   ETIME $ETIME ? got %d\n", pointed.user_data,
                        pointed.res
                    )
                    goto = err;break
                }
                1, 3, 4, 5 ->
                    if (pointed.res != -ECANCELED) {
                        fprintf(
                            stderr, "Req   ECANCELED $ECANCELED ? got %d\n", pointed.user_data,
                            pointed.res
                        )
                        goto = err;break
                    }
                6 ->
                    if (pointed.res.nz) {
                        fprintf(
                            stderr, "Req   0 ? got %d\n", pointed.user_data,
                            pointed.res
                        )
                        goto = err;break
                    }
            }
            io_uring_cqe_seen(ring, cqe.value)
        }

    } while (false)
        close(fds[0])
        close(fds[1])
        return goto?.let { 1 } ?: 0
    }

    private fun test_timeout_link_chain4(ring: CPointer<io_uring>): Int {
        val ts: __kernel_timespec = alloc()
        val cqe: CPointerVar<io_uring_cqe> = alloc()
        val fds = IntArray(2)

        if (pipe(fds.refTo(0)).nz) {
            perror("pipe")
            return 1
        }
        var goto: end? = null;do {

            var sqe = io_uring_get_sqe(ring)!!
            io_uring_prep_nop(sqe)
            sqe.pointed.flags = sqe.pointed.flags.or(sqeIo_link.ub)
            sqe.pointed.user_data = 1u

            sqe = io_uring_get_sqe(ring)!!
            io_uring_prep_poll_add(sqe, fds[0], POLLIN)
            sqe.pointed.flags = sqe.pointed.flags.or(sqeIo_link.ub)
            sqe.pointed.user_data = 2u

            sqe = io_uring_get_sqe(ring)!!
            ts.tv_sec = 0
            ts.tv_nsec = 1000000
            io_uring_prep_link_timeout(sqe, ts.ptr, 0)
            sqe.pointed.user_data = 3u

            var ret = io_uring_submit(ring)
            if (ret != 3) {
                printf("sqe submit failed: %d\n", ret)
                goto = err;break
            }

            for (i in 0 until 3) {
                ret = io_uring_wait_cqe(ring, cqe.ptr)
                if (ret < 0) {
                    printf("wait completion %d\n", ret)
                    goto = err;break
                }
                val pointed = cqe.pointed!!
                when (pointed.user_data.toInt()) {
                    /* poll cancel really should return -ECANCEL... */
                    1 -> if (pointed.res.nz) {
                        fprintf(
                            stderr, "Req   ${-0}   got %d\n", pointed.user_data,
                            pointed.res
                        )
                        goto = err;break
                    }
                    2 -> if (pointed.res != -ECANCELED) {
                        fprintf(
                            stderr, "Req ECANCELED  ${-ECANCELED} ? got %d\n", pointed.user_data,
                            pointed.res
                        )
                        goto = err;break
                    }
                    3 -> if (pointed.res != -ETIME) {
                        fprintf(
                            stderr, "Req ETIME  ${-ETIME} ? got %d\n", pointed.user_data,
                            pointed.res
                        )
                        goto = err;break
                    }
                }
                io_uring_cqe_seen(ring, cqe.value)
            }


        } while (false); close(fds[0])
        close(fds[1]);return goto?.let { 1 } ?: 0
    }

    private fun test_timeout_link_chain5(ring: CPointer<io_uring>): Int {
        val ts1: __kernel_timespec = alloc()
        val ts2: __kernel_timespec = alloc()
        val cqe: CPointerVar<io_uring_cqe> = alloc()

        var goto: end? = null;do {

            var sqe = io_uring_get_sqe(ring)!!
            if (null == sqe) {
                printf("get sqe failed\n")
                goto = err;break
            }
            io_uring_prep_nop(sqe)
            sqe.pointed.flags = sqe.pointed.flags.or(sqeIo_link.ub)
            sqe.pointed.user_data = 1u

            sqe = io_uring_get_sqe(ring)!!
            if (null == sqe) {
                printf("get sqe failed\n")
                goto = err;break
            }
            ts1.tv_sec = 1
            ts1.tv_nsec = 0
            io_uring_prep_link_timeout(sqe, ts1.ptr, 0)
            sqe.pointed.flags = sqe.pointed.flags.or(sqeIo_link.ub)
            sqe.pointed.user_data = 2u

            sqe = io_uring_get_sqe(ring)!!
            if (null == sqe) {
                printf("get sqe failed\n")
                goto = err;break
            }
            ts2.tv_sec = 2
            ts2.tv_nsec = 0
            io_uring_prep_link_timeout(sqe, ts2.ptr, 0)
            sqe.pointed.user_data = 3u

            var ret = io_uring_submit(ring)
            if (ret != 3) {
                printf("sqe submit failed: %d\n", ret)
                goto = err;break
            }

            for (i in 0 until 3) {
                if (goto != null) break
                ret = io_uring_wait_cqe(ring, cqe.ptr)
                if (ret < 0) {
                    printf("wait completion %d\n", ret)
                    goto = err;break
                }
                val pointed = cqe.pointed!!
                when (pointed.user_data.toInt()) {
                    1, 2 ->
                        if (pointed.res.nz && pointed.res != -ECANCELED) {
                            fprintf(
                                stderr, "Request got %d, wanted -EINVAL or -ECANCELED\n",
                                pointed.res
                            )
                            goto = err;break
                        }
                    3 ->
                        if (pointed.res != -ECANCELED && pointed.res != -EINVAL) {
                            fprintf(stderr, "Link timeout got %d, wanted -ECANCELED\n", pointed.res)
                            goto = err;break
                        }
                }
                io_uring_cqe_seen(ring, cqe.value)
            }

        } while (false);return goto?.let { 1 } ?: 0
    }

    fun main(): Int {
        val ring: io_uring = alloc()

        var ret = io_uring_queue_init(8, ring.ptr, 0)
        if (ret.nz) {
            printf("ring setup failed\n")
            return 1
        }

        ret = test_timeout_link_chain1(ring.ptr)
        if (ret.nz) {
            printf("test_single_link_chain1 failed\n")
            return ret
        }

        ret = test_timeout_link_chain2(ring.ptr)
        if (ret.nz) {
            printf("test_single_link_chain2 failed\n")
            return ret
        }

        ret = test_timeout_link_chain3(ring.ptr)
        if (ret.nz) {
            printf("test_single_link_chain3 failed\n")
            return ret
        }

        ret = test_timeout_link_chain4(ring.ptr)
        if (ret.nz) {
            printf("test_single_link_chain4 failed\n")
            return ret
        }

        ret = test_timeout_link_chain5(ring.ptr)
        if (ret.nz) {
            printf("test_single_link_chain5 failed\n")
            return ret
        }

        ret = test_single_link_timeout(ring.ptr, 10.toUInt())
        if (ret.nz) {
            printf("test_single_link_timeout 10 failed\n")
            return ret
        }

        ret = test_single_link_timeout(ring.ptr, 100000UL.toUInt())
        if (ret.nz) {
            printf("test_single_link_timeout 100000 failed\n")
            return ret
        }

        ret = test_single_link_timeout(ring.ptr, 500000000UL.toUInt())
        if (ret.nz) {
            printf("test_single_link_timeout 500000000 failed\n")
            return ret
        }

        ret = test_single_link_no_timeout(ring.ptr)
        if (ret.nz) {
            printf("test_single_link_no_timeout failed\n")
            return ret
        }

        ret = test_single_link_timeout_error(ring.ptr)
        if (ret.nz) {
            printf("test_single_link_timeout_error failed\n")
            return ret
        }

        ret = test_single_link_timeout_nop(ring.ptr)
        if (ret.nz) {
            printf("test_single_link_timeout_nop failed\n")
            return ret
        }

        ret = test_single_link_timeout_ception(ring.ptr)
        if (ret.nz) {
            printf("test_single_link_timeout_ception failed\n")
            return ret
        }

        ret = test_fail_lone_link_timeouts(ring.ptr)
        if (ret.nz) {
            printf("test_fail_lone_link_timeouts failed\n")
            return ret
        }

        ret = test_fail_two_link_timeouts(ring.ptr)
        if (ret.nz) {
            printf("test_fail_two_link_timeouts failed\n")
            return ret
        }

        return 0
    }

    companion object {
        enum class end {
            err, ok
        }
    }
}

fun main() {
    exit(TimeoutAppState().main())
}
