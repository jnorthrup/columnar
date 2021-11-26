package test.link


import kotlinx.cinterop.*
import linux_uring.*
import platform.posix.NULL
import platform.posix.__s32
import platform.posix.fprintf
import platform.posix.iovec
import platform.posix.stderr
import simple.HasPosixErr
import simple.simple.CZero.z
import kotlin.native.internal.NativePtr
import linux_uring.printf as linux_uringPrintf

/* SPDX-License-Identifier: MIT */
/*
 * Description: run various linked sqe tests
 *
 */

var no_hardlink: Int = 0

/*
 * Timer with single nop
 */
fun test_single_hardlink(ring: CPointer<io_uring>): Int = nativeHeap.run {
    val ts: __kernel_timespec = alloc()
    val cqe1: CValuesRef<CPointerVar<io_uring_cqe>> = cValue()
    var ret: Int
    var i: Int

    var sqe = io_uring_get_sqe(ring)
    HasPosixErr.posixFailOn(sqe?.let { false } ?: true) {
        fprintf(stderr, "get sqe failed\n")
    }
    ts.tv_sec = 0
    ts.tv_nsec = 10000000UL.toLong()
    io_uring_prep_timeout(sqe, ts.ptr, 0, 0)
    sqe!!.pointed.flags = sqe.pointed.flags.plus(IOSQE_IO_LINK or IOSQE_IO_HARDLINK).toUByte()
    sqe.pointed.user_data = 1u

    sqe = io_uring_get_sqe(ring)
    HasPosixErr.posixFailOn(sqe?.let { false } ?: true) {
        fprintf(stderr, "get sqe failed\n")
    }
    io_uring_prep_nop(sqe)
    sqe!!.pointed.user_data = 2.toULong()

    ret = io_uring_submit(ring)
    HasPosixErr.posixFailOn(ret <= 0) {
        fprintf(stderr, "sqe submit failed: %d\n", ret)
    }

    memScoped {
        val cqe = cqe1.getPointer(this)
        repeat(2) { i ->
            ret = io_uring_wait_cqe(ring, cqe1)
            do {
                if (ret < 0) {
                    fprintf(stderr, "wait completion %d\n", ret)
                }
                if (cqe.rawValue == NativePtr.NULL) {
                    fprintf(stderr, "failed to get cqe\n")
                }
                if (no_hardlink.z) {
                    break
                }
                if (cqe.pointed.pointed!!.user_data == 1UL && cqe.pointed.pointed!!.res == -platform.posix.EINVAL) {
                    fprintf(platform.posix.stdout, "Hard links not supported, skipping\n")
                    no_hardlink = 1
                    break
                }
                HasPosixErr.posixFailOn(cqe.pointed.pointed!!.user_data == 1UL && cqe.pointed.pointed!!.res != -platform.posix.ETIME) {
                    fprintf(stderr, "timeout failed with %d\n", cqe.pointed.pointed!!.res)

                }
                HasPosixErr.posixFailOn(cqe.pointed.pointed!!.user_data == 2UL && 0 != cqe.pointed.pointed!!.res) {
                    fprintf(stderr, "nop failed with %d\n", cqe.pointed.pointed!!.res)
                }
            } while (false)
            io_uring_cqe_seen(ring, cqe.pointed.value)
        }
    }

    return 0
}

/*
 * Timer.pointed.timer  -> nop
 */
fun test_double_hardlink(ring: CPointer<io_uring>): Int = nativeHeap.run {

    var ts1 = alloc<__kernel_timespec>()
    var ts2 = alloc<__kernel_timespec>()
    var ret: Int
    var i: Int

    if (0 != no_hardlink)
        return 0

    var sqe = io_uring_get_sqe(ring)!!

    ts1.tv_sec = 0.toLong() /* = kotlin.Long */
    ts1.tv_nsec = 10000000UL.toLong()
    io_uring_prep_timeout(sqe, ts1.ptr, 0, 0)
    sqe.pointed.flags = (sqe.pointed.flags.toUInt() or IOSQE_IO_LINK or IOSQE_IO_HARDLINK).toUByte()
    sqe.pointed.user_data = 1UL

    sqe = secondHardLink(ring)
    io_uring_prep_nop(sqe)
    sqe.pointed.user_data = 3UL

    ret = io_uring_submit(ring)
    HasPosixErr.posixFailOn(ret <= 0) {
        fprintf(stderr, "sqe submit failed: %d\n", ret)
    }
    val cqe: CPointerVar<io_uring_cqe> = alloc()

    for (i in 0 until 3) {
        ret = io_uring_wait_cqe(ring, cqe.ptr)
        HasPosixErr.posixFailOn(ret < 0) {
            fprintf(stderr, "wait completion %d\n", ret)
        }
        HasPosixErr.posixFailOn(cqe.pointed?.user_data == 1UL && cqe.pointed?.res != -platform.posix.ETIME) {
            fprintf(stderr, "timeout failed with %d\n", cqe.pointed?.res as __s32)
        }
        HasPosixErr.posixFailOn(cqe.pointed?.user_data == 2UL && (cqe.pointed?.res as __s32) != -platform.posix.ETIME) {
            fprintf(stderr, "timeout failed with %d\n", cqe.pointed?.res as __s32)
        }
        HasPosixErr.posixFailOn(cqe.pointed?.user_data == 3UL && 0 != cqe.pointed?.res as __s32) {
            fprintf(stderr, "nop failed with %d\n", cqe.pointed?.res as __s32)
        }
        io_uring_cqe_seen(ring, cqe.value)

    }
    return 0
}

fun secondHardLink(ring: CPointer<io_uring>): CPointer<io_uring_sqe> {
    return io_uring_get_sqe(ring)!!
}

/*
 * Test failing head of chain, and dependent getting -ECANCELED
 */
fun test_single_link_fail(ring: CPointer<io_uring>): Int = nativeHeap.run {
    var ret: Int

    var sqe = io_uring_get_sqe(ring)!!
    io_uring_prep_nop(sqe)
    sqe.pointed.flags = sqe.pointed.flags.or(IOSQE_IO_LINK.toUByte())

    sqe = io_uring_get_sqe(ring)!!

    io_uring_prep_nop(sqe)
    ret = io_uring_submit(ring)
    HasPosixErr.posixFailOn(ret <= 0) { "sqe submit failed: " + ret }
    val cqe: CPointerVar<io_uring_cqe> = alloc()


    for (i in 0 until 2/*; i++*/) {
        ret = io_uring_peek_cqe(ring, cqe.ptr)



        HasPosixErr.posixFailOn(ret < 0) {
            linux_uringPrintf("wait completion %d\n", ret)
        }

        HasPosixErr.posixFailOn(i == 0 && cqe.pointed?.res != -platform.posix.EINVAL) {
            linux_uringPrintf("sqe0 failed with %d, wanted -EINVAL\n", cqe.value!!.pointed.res)
        }
        HasPosixErr.posixFailOn(i == 1 && cqe.pointed?.res != -platform.posix.ECANCELED) {
            linux_uringPrintf("sqe1 failed with %d, wanted -ECANCELED\n", cqe.pointed?.res as __s32)
        }
        io_uring_cqe_seen(ring, cqe.value)
    }
    return 0
}

/*
 * Test two independent chains
 */
fun test_double_chain(ring: CPointer<io_uring>): Int = nativeHeap.run {
    var ret: Int

    var sqe = io_uring_get_sqe(ring)!!


    io_uring_prep_nop(sqe)
    sqe.pointed.flags = sqe.pointed.flags.plus(IOSQE_IO_LINK).toUByte()

    sqe = io_uring_get_sqe(ring)!!

    io_uring_prep_nop(sqe)

    sqe = io_uring_get_sqe(ring)!!

    io_uring_prep_nop(sqe)
    sqe.pointed.flags = sqe.pointed.flags.plus(IOSQE_IO_LINK).toUByte()

    sqe = io_uring_get_sqe(ring)!!

    io_uring_prep_nop(sqe)

    ret = io_uring_submit(ring)
    HasPosixErr.posixFailOn(ret <= 0) {
        linux_uringPrintf("sqe submit failed: %d\n", ret)
    }
    val cqe1: CValuesRef<CPointerVarOf<CPointer<io_uring_cqe>>> = cValue()
    memScoped {
        for (i in 0 until 4) {
            ret = io_uring_wait_cqe(ring, cqe1)
            HasPosixErr.posixFailOn(ret < 0) {
                linux_uringPrintf("wait completion %d\n", ret)
            }
            io_uring_cqe_seen(ring, cqe1.getPointer(this).pointed.value)
        }
    }
    return 0
}

/*
 * Test multiple dependents
 */
fun test_double_link(ring: CPointer<io_uring>): Int = nativeHeap.run {


    var sqe = io_uring_get_sqe(ring)!!



    io_uring_prep_nop(sqe)
    sqe.pointed.flags = sqe.pointed.flags.plus(IOSQE_IO_LINK).toUByte()

    sqe = io_uring_get_sqe(ring)!!

    io_uring_prep_nop(sqe)
    sqe.pointed.flags = sqe.pointed.flags.plus(IOSQE_IO_LINK).toUByte()

    sqe = io_uring_get_sqe(ring)!!

    io_uring_prep_nop(sqe)

    var ret = io_uring_submit(ring)

    val cqe1: CValuesRef<CPointerVar<io_uring_cqe>> = cValue()
    memScoped {
        for (i in 0 until 3) {
            ret = io_uring_wait_cqe(ring, cqe1)
            io_uring_cqe_seen(ring, cqe1.getPointer(this).pointed.value)
        }
    }
    return 0
}

/*
 * Test single dependency
 */
fun test_single_link(ring: CPointer<io_uring>): Int = nativeHeap.run {

    var sqe = io_uring_get_sqe(ring)!!


    io_uring_prep_nop(sqe)
    sqe.pointed.flags = sqe.pointed.flags.plus(IOSQE_IO_LINK).toUByte()

    sqe = io_uring_get_sqe(ring)!!


    io_uring_prep_nop(sqe)

    var ret = io_uring_submit(ring)
    HasPosixErr.posixFailOn(ret <= 0) {
        linux_uringPrintf("sqe submit failed: %d\n", ret)
    }
    memScoped {
        repeat(2) { i ->
            val cqe1: CValuesRef<CPointerVar<io_uring_cqe>> = cValue()
            ret = io_uring_wait_cqe(ring, cqe1)
            HasPosixErr.posixFailOn(ret < 0) {
                linux_uringPrintf("wait completion %d\n", ret)
            }
            io_uring_cqe_seen(ring, cqe1.getPointer(this).pointed.value)
        }
    }
    return 0
}

fun test_early_fail_and_wait(): Int = nativeHeap.run {

    val invalid_fd = 42
//    val iov: iovec = alloc {
//        iov_base = NULL
//        iov_len = 0.toULong()
//    }
    val iov = (alloc<iovec> {
        iov_base = NULL
        iov_len = 0.toULong()
    }).ptr
    val ring: io_uring = alloc<io_uring>()
    /* create a new ring as it leaves it dirty */
    var ret = io_uring_queue_init(8, ring.ptr, 0)
    HasPosixErr.posixFailOn(0 != ret) {
        linux_uringPrintf("ring setup failed\n")
    }

    var sqe = io_uring_get_sqe(ring.ptr)!!

    io_uring_prep_readv(sqe, invalid_fd, iov.reinterpret(), 1, 0)
    sqe.pointed.flags = (sqe.pointed.flags + IOSQE_IO_LINK).toUByte()
    sqe = io_uring_get_sqe(ring.ptr)!!

    io_uring_prep_nop(sqe)
    ret = io_uring_submit_and_wait(ring.ptr, 2)
    if (ret <= 0 && ret != -platform.posix.EAGAIN) {
        linux_uringPrintf("sqe submit failed: %d\n", ret)
    }

    io_uring_queue_exit(ring.ptr)
    return 0
}

fun main(): Unit = nativeHeap.run {

    val ring: io_uring = alloc()
    var ret = io_uring_queue_init(8, ring.ptr, 0)
    HasPosixErr.posixRequires(ret.z) {
        ("ring setup failed\n")
    }

    val poll_ring = alloc<io_uring>()
    read_barrier()
    ret = io_uring_queue_init(8, poll_ring.ptr as CValuesRef<io_uring>, IORING_SETUP_IOPOLL)
    HasPosixErr.posixRequires(ret.z) {
        ("poll_ring setup failed\n")
    }
    read_barrier()

    ret = test_single_link(ring.ptr)
    HasPosixErr.posixRequires(ret.z) {
        ("test_single_link failed\n")
    }
    read_barrier()

    ret = test_double_link(ring.ptr)
    HasPosixErr.posixRequires(ret.z) {
        ("test_double_link failed\n")
    }
    read_barrier()

    ret = test_double_chain(ring.ptr)
    HasPosixErr.posixRequires(ret.z) {
        ("test_double_chain failed\n")
    }
    read_barrier()
    if (true) {//we see some NPE's from io_uring so ... defer
        ret = test_single_link_fail(poll_ring.ptr)
        HasPosixErr.posixRequires(ret.z) {
            ("test_single_link_fail failed\n")
        }
    }
    ret = test_single_hardlink(ring.ptr)
    HasPosixErr.posixRequires(ret.z) {
        ("test_single_hardlink\n")
    }

    ret = test_double_hardlink(ring.ptr)
    HasPosixErr.posixRequires(ret.z) {
        ("test_double_hardlink\n")
    }

    ret = test_early_fail_and_wait()
    HasPosixErr.posixRequires(ret.z) {
        ("test_early_fail_and_wait\n")
    }

    return
}