package test.link


import kotlinx.cinterop.*
import linux_uring.*
import platform.posix.fprintf
import platform.posix.stderr
import simple.*
import simple.simple.CZero.z
import kotlin.native.internal.NativePtr.Companion.NULL

/* SPDX-License-Identifier: MIT */
/*
 * Description: run various linked sqe tests
 *
 */

var no_hardlink: Int = 0

/*
 * Timer with single nop
 */
fun test_single_hardlink(ring: CPointer<io_uring>): Int = memScoped {
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

    for (i in 0 until 2) {
        ret = io_uring_wait_cqe(ring, cqe1)
        val cqe = cqe1.getPointer(this)
        do {
            if (ret < 0) {
                fprintf(stderr, "wait completion %d\n", ret)
            }
            if (cqe.rawValue == NULL) {
                fprintf(stderr, "failed to get cqe\n")
            }
            if (no_hardlink.z) {
                break
            }
            if (cqe.pointed.pointed!!.user_data == 1UL && cqe.pointed.pointed!!.res == -EINVAL) {
                fprintf(stdout, "Hard links not supported, skipping\n")
                no_hardlink = 1
                break
            }
            HasPosixErr.posixFailOn(cqe.pointed.pointed!!.user_data == 1UL && cqe.pointed.pointed!!.res != -ETIME) {
                fprintf(stderr, "timeout failed with %d\n", cqe.pointed.pointed!!.res)

            }
            HasPosixErr.posixFailOn(cqe.pointed.pointed!!.user_data == 2UL && 0 != cqe.pointed.pointed!!.res) {
                fprintf(stderr, "nop failed with %d\n", cqe.pointed.pointed!!.res)
            }
        } while (false)
        io_uring_cqe_seen(ring, cqe.pointed.value)
    }

    return 0
}

/*
 * Timer.pointed.timer  -> nop
 */
fun test_double_hardlink(ring: CPointer<io_uring>): Int = memScoped {

    var ts1: __kernel_timespec = alloc()
    var ts2: __kernel_timespec = alloc()
    var ret: Int
    var i: Int

    if (0 != no_hardlink)
        return 0

    var sqe = io_uring_get_sqe(ring)!!

//	if (!sqe) {
//		fprintf(stderr, "get sqe failed\n");
//		goto err;
//	}
    ts1.tv_sec = 0.toLong() /* = kotlin.Long */
    ts1.tv_nsec = 10000000UL.toLong()
    io_uring_prep_timeout(sqe, ts1.ptr, 0, 0)
    sqe.pointed.flags = sqe.pointed.flags.plus(IOSQE_IO_LINK or IOSQE_IO_HARDLINK).toUByte()
    sqe.pointed.user_data = 1UL

    sqe = secondHardLink(ring)
    io_uring_prep_nop(sqe)
    sqe.pointed.user_data = 3UL

    ret = io_uring_submit(ring)
    HasPosixErr.posixFailOn(ret <= 0) {
        fprintf(stderr, "sqe submit failed: %d\n", ret)
    }
    val cqe1: CValuesRef<CPointerVar<io_uring_cqe>> = cValue()
    for (i in 0 until 3) {
        ret = io_uring_wait_cqe(ring, cqe1)
        val cqe2 = cqe1.getPointer(this)
        val cqe = cqe2.pointed.pointed!!
        HasPosixErr.posixFailOn(ret < 0) {
            fprintf(stderr, "wait completion %d\n", ret)
        }
        HasPosixErr.posixFailOn(cqe.user_data == 1UL && cqe.res != -ETIME) {
            fprintf(stderr, "timeout failed with %d\n", cqe.res)
        }
        HasPosixErr.posixFailOn(cqe.user_data == 2UL && cqe.res != -ETIME) {
            fprintf(stderr, "timeout failed with %d\n", cqe.res)
        }
        HasPosixErr.posixFailOn(cqe.user_data == 3UL && 0 != cqe.res) {
            fprintf(stderr, "nop failed with %d\n", cqe.res)
        }
        io_uring_cqe_seen(ring, cqe2.pointed.value)
    }
    return 0
}

fun secondHardLink(ring: CPointer<io_uring>): CPointer<io_uring_sqe> {
    return io_uring_get_sqe(ring)!!
}

/*
 * Test failing head of chain, and dependent getting -ECANCELED
 */
fun test_single_link_fail(ring: CPointer<io_uring>): Int = memScoped {
    var ret: Int

    var sqe = io_uring_get_sqe(ring)!!


    io_uring_prep_nop(sqe)
    sqe.pointed.flags = sqe.pointed.flags.or(IOSQE_IO_LINK.toUByte())

    sqe = io_uring_get_sqe(ring)!!

    io_uring_prep_nop(sqe)
    ret = io_uring_submit(ring)
    HasPosixErr.posixFailOn(ret <= 0) {
        printf("sqe submit failed: %d\n", ret)
    }
    val cqe1: CValuesRef<CPointerVar<io_uring_cqe>> = cValue()


    for (i in 0 until 2/*; i++*/) {
        ret = io_uring_peek_cqe(ring, cqe1)
        val cqe=cqe1.getPointer(this).pointed.value!!
        HasPosixErr.posixFailOn (ret < 0) {
            printf("wait completion %d\n", ret)
        }

        HasPosixErr.posixFailOn (i == 0 && cqe.pointed.res != -EINVAL) {
            printf("sqe0 failed with %d, wanted -EINVAL\n", cqe.pointed.res)
        }
        HasPosixErr.posixFailOn (i == 1 && cqe.pointed.res != -ECANCELED) {
            printf("sqe1 failed with %d, wanted -ECANCELED\n", cqe.pointed.res)
        }
        io_uring_cqe_seen(ring, cqe)
    }
    return 0
}

/*
 * Test two independent chains
 */
fun test_double_chain(ring:CPointer<io_uring>):Int=memScoped{
    var ret:Int

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
    HasPosixErr.posixFailOn (ret <= 0) {
        printf("sqe submit failed: %d\n", ret)
    }
    val cqe1:CValuesRef<CPointerVarOf<CPointer<io_uring_cqe>>> =cValue()
    for (i in 0 until 4 ) {
    ret = io_uring_wait_cqe(ring, cqe1)
    HasPosixErr.posixFailOn (ret < 0) {
        printf("wait completion %d\n", ret)
    }
    io_uring_cqe_seen(ring, cqe1.getPointer(this).pointed.value)
    }

    return 0
}

/*
 * Test multiple dependents
 */
fun test_double_link(ring:CPointer<io_uring>):Int=memScoped {


    var sqe = io_uring_get_sqe(ring)!!



    io_uring_prep_nop(sqe)
    sqe.pointed.flags = sqe.pointed.flags.plus(IOSQE_IO_LINK).toUByte()

    sqe = io_uring_get_sqe(ring)!!

    io_uring_prep_nop(sqe)
    sqe.pointed.flags = sqe.pointed.flags.plus(IOSQE_IO_LINK).toUByte()

    sqe = io_uring_get_sqe(ring)!!

    io_uring_prep_nop(sqe)

    var ret = io_uring_submit(ring)

    val cqe1: CValuesRef<CPointerVar<io_uring_cqe> > =cValue()
    for (i in 0 until 3 ) {
    ret = io_uring_wait_cqe(ring, cqe1)
    io_uring_cqe_seen(ring, cqe1.getPointer(this).pointed.value)
}

    return 0
}

/*
 * Test single dependency
 */
fun test_single_link(ring:CPointer<io_uring>):Int
= memScoped{

    var sqe = io_uring_get_sqe(ring)!!


    io_uring_prep_nop(sqe)
    sqe.pointed.flags = sqe.pointed.flags.plus(IOSQE_IO_LINK).toUByte()

    sqe = io_uring_get_sqe(ring)!!


    io_uring_prep_nop(sqe)

    var ret = io_uring_submit(ring)
    HasPosixErr.posixFailOn (ret <= 0) {
        printf("sqe submit failed: %d\n", ret)
    }

    repeat(2) {i->
    val cqe1: CValuesRef<CPointerVar<io_uring_cqe>  > = cValue()
        ret = io_uring_wait_cqe(ring, cqe1 )
    HasPosixErr.posixFailOn (ret < 0) {
        printf("wait completion %d\n", ret)
    }
    io_uring_cqe_seen(ring, cqe1.getPointer(this).pointed.value)
}

    return 0
}

fun test_early_fail_and_wait():Int  = memScoped {

    val invalid_fd = 42
    val iov:iovec =alloc { iov_base = platform.posix.NULL
        iov_len = 0.toULong() }
    val ring: io_uring = alloc<io_uring>()
    /* create a new ring as it leaves it dirty */
    var ret = io_uring_queue_init(8, ring.ptr, 0)
    HasPosixErr.posixFailOn(0 != ret) {
        printf("ring setup failed\n")
    }

    var sqe = io_uring_get_sqe(ring.ptr)!!

    io_uring_prep_readv(sqe, invalid_fd, iov.ptr, 1, 0)
    sqe.pointed.flags = (sqe.pointed.flags + IOSQE_IO_LINK).toUByte()
    sqe = io_uring_get_sqe(ring.ptr)!!

    io_uring_prep_nop(sqe)
    ret = io_uring_submit_and_wait(ring.ptr, 2)
    if (ret <= 0 && ret != -EAGAIN) {
        printf("sqe submit failed: %d\n", ret)
    }

    io_uring_queue_exit(ring.ptr)
    return 0
}

fun main():Unit = memScoped {

    val ring:io_uring=alloc()
    val poll_ring:io_uring=alloc()
    var ret = io_uring_queue_init(8, ring.ptr, 0)

    HasPosixErr.posixRequires(0 == ret) {
        ("ring setup failed\n")

    }

    ret = io_uring_queue_init(8, poll_ring.ptr as CValuesRef<io_uring>, IORING_SETUP_IOPOLL)
    HasPosixErr.posixRequires(0 == ret) {
        ("poll_ring setup failed\n")
    }

    ret = test_single_link(ring.ptr)
    HasPosixErr.posixRequires(0 == ret) {
        ("test_single_link failed\n")
    }

    ret = test_double_link(ring.ptr)
    HasPosixErr.posixRequires(0 == ret) {
        ("test_double_link failed\n")
    }

    ret = test_double_chain(ring.ptr)
    HasPosixErr.posixRequires(0 == ret) {
        ("test_double_chain failed\n")
    }

    ret = test_single_link_fail(poll_ring.ptr)
    HasPosixErr.posixRequires(0 == ret) {
        ("test_single_link_fail failed\n")
    }

    ret = test_single_hardlink(ring.ptr)
    HasPosixErr.posixRequires(0 == ret) {
        ("test_single_hardlink\n")
    }

    ret = test_double_hardlink(ring.ptr)
    HasPosixErr.posixRequires(0 == ret) {
        ("test_double_hardlink\n")
    }

    ret = test_early_fail_and_wait()
    HasPosixErr.posixRequires(0 == ret) {
        ("test_early_fail_and_wait\n")
    }

    return
}
