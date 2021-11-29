package test.register

import kotlinx.cinterop.*
import linux_uring.*
import linux_uring.include.fromOctal
import platform.linux.getrlimit
import platform.linux.setrlimit
import simple.HasPosixErr
import simple.simple.CZero.nz
import simple.simple.CZero.z
import linux_uring.pipe as linux_uringPipe

/* SPDX-License-Identifier: MIT */
/*
 * Description: run various file registration tests
 *
 */
//#include <errno.h>
//#include <stdio.h>
//#include <unistd.h>
//#include <stdlib.h>
//#include <string.h>
//#include <fcntl.h>
//#include <sys/resource.h>

//#include "helpers.h"
//#include "liburing.h"

var no_update: Int = 0

fun close_files(files: IntArray?, nr_files: Int, add: Int) = nativeHeap.run {
     repeat(nr_files) { i ->

        files?.let {
            val __fd = files[i]
            if(__fd >2) {
                close(__fd)
            }
        }
        val fname= if (add.z)
            ".reg.$i"
        else
            ".add.${i + add}"
        unlink(fname )
    }
}

fun open_files(nr_files: Int, extra: Int, add: Int): IntArray {

    val files = IntArray(nr_files+extra)

    for (i in 0 until nr_files) {
      val __file: String =    if (add.z)
             (  ".reg.$i" )
        else
             (  ".add.${ i + add}")
        files[i] = open(__file, O_RDWR or O_CREAT  , 644.fromOctal())
        if (files[i] < 0) {
            perror("open $__file")
            break
        }
    }
    if (extra.nz) {
        for (i in nr_files until nr_files + extra)
            files[i] = -1
    }

    return files
}

fun test_shrink(ring: CPointer<io_uring>): Int = nativeHeap.run {
//    ret:Int, off, fd;
//    int *files;
    val files = open_files(50, 0, 0)
    var ret = io_uring_register_files(ring, files.toCValues(), 50)
    HasPosixErr.posixFailOn(ret.nz) {
        fprintf(stderr, "%s: register ret=%d\n", "test_shrink", ret)

    }

    var off = 0
    do {
        val fd: IntVar = alloc { value = -1 }
        ret = io_uring_register_files_update(ring, off.toUInt(), fd.ptr, 1)
        if (ret != 1) {
            if (off == 50 && ret == -EINVAL)
                break
            fprintf(stderr, "%s: update ret=%d\n", "test_shrink", ret)
            break
        }
        off++
    } while (true)

    ret = io_uring_unregister_files(ring)
    HasPosixErr.posixFailOn(ret.nz) {
        fprintf(stderr, "%s: unregister ret=%d\n", "test_shrink", ret)
    }

    close_files(files, 50, 0)
    return 0

}


fun test_grow(ring: CPointer<io_uring>): Int {
//    ret:Int, off;
//    int *files, *fds = NULL;

    val files = open_files(50, 250, 0)
    var ret = io_uring_register_files(ring, files.toCValues(), 300)
    HasPosixErr.posixFailOn(ret.nz) {
        fprintf(stderr, "%s: register ret=%d\n", "test_grow", ret)
    }

    var off = 50
    do {
        val fds = open_files(1, 0, off)
        ret = io_uring_register_files_update(ring, off.toUInt(), fds.refTo(0), 1)
        if (ret != 1) {
            if (off == 300 && ret == -EINVAL)
                break
            fprintf(stderr, "%s: update ret=%d\n", "test_grow", ret)
            break
        }
        HasPosixErr.posixFailOn(off >= 300) {
            fprintf(stderr, "%s: Succeeded beyond end-of-list?\n", "test_grow")
        }
        off++
    } while (true)

    ret = io_uring_unregister_files(ring)
    HasPosixErr.posixFailOn(ret.nz) {
        fprintf(stderr, "%s: unregister ret=%d\n", "test_grow", ret)
    }

    close_files(files, 100, 0)
    close_files(null, 251, 50)
    return 0
}

fun test_replace_all(ring: CPointer<io_uring>): Int {
//    int *files, *fds = NULL;
//    ret:Int, i;
    val __FUNCTION__ = "test_replace_all"
    val files = open_files(100, 0, 0)
    var ret = io_uring_register_files(ring, files.toCValues(), 100)
    HasPosixErr.posixFailOn(ret.nz) {
        fprintf(stderr, "%s: register ret=%d\n", __FUNCTION__, ret)
    }

    val fds = IntArray(100) { -1 }

    ret = io_uring_register_files_update(ring, 0, fds.refTo(0), 100)
    HasPosixErr.posixFailOn(ret != 100) {
        fprintf(stderr, "%s: update ret=%d\n", __FUNCTION__, ret)
    }
    ret = io_uring_unregister_files(ring)
    HasPosixErr.posixFailOn(ret.nz) {
        fprintf(stderr, "%s: unregister ret=%d\n", __FUNCTION__, ret)
    }
    close_files(files, 100, 0)
    return 0
}

fun test_replace(ring: CPointer<io_uring>): Int {
//    int *files, *fds = NULL;
//    ret:Int;
    val __FUNCTION__ = "test_replace"
    val files = open_files(100, 0, 0)
    var ret = io_uring_register_files(ring, files.toCValues(), 100)
    HasPosixErr.posixFailOn(ret.nz) {
        fprintf(stderr, "%s: register ret=%d\n", __FUNCTION__, ret)
    }

    val fds = open_files(10, 0, 1)
    ret = io_uring_register_files_update(ring, 90, fds.refTo(0), 10)
    HasPosixErr.posixFailOn(ret != 10) {
        fprintf(stderr, "%s: update ret=%d\n", __FUNCTION__, ret)
    }

    ret = io_uring_unregister_files(ring)
    if (ret.nz) {
        fprintf(stderr, "%s: unregister ret=%d\n", __FUNCTION__, ret)
    }

    close_files(files, 100, 0)
    close_files(fds, 10, 1)
    return 0
}

fun test_removals(ring: CPointer<io_uring>): Int {
//    int * files, *fds = NULL
//    ret:Int, i
    val __FUNCTION__ = "test_removals"
    val files = open_files(100, 0, 0)
    var ret = io_uring_register_files(ring, files.toCValues(), 100)
    HasPosixErr.posixFailOn(ret.nz) {
        fprintf(stderr, "%s: register ret=%d\n", __FUNCTION__, ret)
    }

    val fds = IntArray(10) { -1 }


    ret = io_uring_register_files_update(ring, 50, fds.refTo(0), 10)
    HasPosixErr.posixFailOn(ret != 10) {
        fprintf(stderr, "%s: update ret=%d\n", __FUNCTION__, ret)
    }

    ret = io_uring_unregister_files(ring)
    HasPosixErr.posixFailOn(ret.nz) {
        fprintf(stderr, "%s: unregister ret=%d\n", __FUNCTION__, ret)
    }

    close_files(files, 100, 0)

    return 0
}

fun test_additions(ring: CPointer<io_uring>): Int {
//    int * files, *fds = NULL
//    ret:Int
    val __FUNCTION__ = "test_additions"
    val files = open_files(100, 100, 0)
    var ret = io_uring_register_files(ring, files.toCValues(), 200)
    HasPosixErr.posixFailOn(ret.nz) {
        fprintf(stderr, "%s: register ret=%d\n", __FUNCTION__, ret)
    }

    val fds = open_files(2, 0, 1)
    ret = io_uring_register_files_update(ring, 100, fds.refTo(0), 2)
    HasPosixErr.posixFailOn(ret != 2) {
        fprintf(stderr, "%s: update ret=%d\n", __FUNCTION__, ret)
    }

    ret = io_uring_unregister_files(ring)
    HasPosixErr.posixFailOn(ret.nz) {
        fprintf(stderr, "%s: unregister ret=%d\n", __FUNCTION__, ret)
    }

    close_files(files, 100, 0)
    close_files(fds, 2, 1)
    return 0
}

fun test_sparse(ring: CPointer<io_uring>): Int {
//    int * files
//    ret:Int
    val __FUNCTION__ = "test_sparse"
    val files = open_files(100, 100, 0)
    var ret = io_uring_register_files(ring, files.toCValues(), 200)
    do {
        if (ret.nz) {
            if (ret == -EBADF) {
                fprintf(stdout, "Sparse files not supported\n")
                no_update = 1
                break;
            }
            HasPosixErr.posixFailOn(ret.nz)
            {  "$__FUNCTION__: register ret=$ret " }
        }
        ret = io_uring_unregister_files(ring)
        HasPosixErr.posixFailOn(ret.nz) {
            fprintf(stderr, "%s: unregister ret=%d\n", __FUNCTION__, ret)
        }
    } while (false)
    close_files(files, 100, 0)
    return 0
}

fun test_basic_many(ring: CPointer<io_uring>): Int {
//    int * files
//    ret:Int
    val __FUNCTION__ = "test_basic_many"
    val files = open_files(768, 0, 0)
    var ret = io_uring_register_files(ring, files.toCValues(), 768)
    HasPosixErr.posixFailOn(ret.nz) {
         "$__FUNCTION__: register $ret"
    }
    ret = io_uring_unregister_files(ring)
    HasPosixErr.posixFailOn(ret.nz) {
        fprintf(stderr, "%s: unregister %d\n", __FUNCTION__, ret)
    }
    close_files(files, 768, 0)
    return 0
}

fun test_basic(ring: CPointer<io_uring>, fail: Int): Int {
//    int * files
//    ret:Int
    val nr_files: Int = if (fail.nz) 10 else 100
    val __FUNCTION__ = "test_basic"
    val files = open_files(nr_files, 0, 0)
    var ret = io_uring_register_files(ring, files.toCValues(), 100)
    if (ret.nz) {
        if (fail.nz) {
            if (ret == -EBADF || ret == -EFAULT)
                return 0
        }

        HasPosixErr.posixFailOn(true) { fprintf(stderr, "%s: register %d\n", __FUNCTION__, ret) }

    }
    HasPosixErr.posixFailOn(fail.nz) {
        fprintf(stderr, "Registration succeeded, but expected fail\n")
    }
    ret = io_uring_unregister_files(ring)
    HasPosixErr.posixFailOn(ret.nz) {
        fprintf(stderr, "%s: unregister %d\n", __FUNCTION__, ret)
    }
    close_files(files, nr_files, 0)
    return 0
}

/*
 * Register 0 files, but reserve space for 10.  Then add one file.
 */
fun test_zero(ring: CPointer<io_uring>): Int {
//    int * files, *fds = NULL
//    ret:Int
    val __FUNCTION__ = "test_zero"
    val files = open_files(0, 10, 0)
    var ret = io_uring_register_files(ring, files.toCValues(), 10)
    HasPosixErr.posixFailOn(ret.nz) {
        fprintf(stderr, "%s: register ret=%d\n", __FUNCTION__, ret)
    }

    val fds = open_files(1, 0, 1)
    ret = io_uring_register_files_update(ring, 0, fds.refTo(0), 1)
    HasPosixErr.posixFailOn(ret != 1) {
        fprintf(stderr, "%s: update ret=%d\n", __FUNCTION__, ret)
    }

    ret = io_uring_unregister_files(ring)
    HasPosixErr.posixFailOn(ret.nz) {
        fprintf(stderr, "%s: unregister ret=%d\n", __FUNCTION__, ret)
    }


    close_files(fds, 1, 1)
    return 0
}

fun test_fixed_read_write(ring: CPointer<io_uring>, index: Int): Int = nativeHeap.run {
    var iov: CPointer<iovec> = allocArray<iovec>(2)
    var ret: Int
    val __FUNCTION__ = "test_fixed_read_write"
    println("$__FUNCTION__ on index $index")
    iov[0].iov_base = malloc(4096)
    iov[0].iov_len = 4096u
    memset(iov[0].iov_base, 0x5a, 4096)

    iov[1].iov_base = malloc(4096)
    iov[1].iov_len = 4096u

    var sqe = io_uring_get_sqe(ring)!!
    println("sqe succeeded")
    io_uring_prep_writev(sqe, index, iov, 1, 0)
    sqe.pointed.flags = sqe.pointed.flags.or(IOSQE_FIXED_FILE.toUByte())
    sqe.pointed.user_data = 1u
    ret = io_uring_submit(ring)
    HasPosixErr.posixFailOn(ret != 1) {
        fprintf(stderr, "%s: got %d, wanted 1\n", __FUNCTION__, ret)
    }
    val cqe: CPointerVar<io_uring_cqe> = alloc()

    ret = io_uring_wait_cqe(ring, cqe.ptr)
    HasPosixErr.posixFailOn(ret < 0) {
        fprintf(stderr, "%s: io_uring_wait_cqe=%d\n", __FUNCTION__, ret)
    }
    HasPosixErr.posixFailOn(cqe.value!!.pointed.res != 4096) {
        fprintf(stderr, "%s: write cqe.pointed.res =%d\n", __FUNCTION__, cqe.value!!.pointed.res)
    }
    io_uring_cqe_seen(ring, cqe.value!!.pointed.ptr )
    println("cqe succeeded")
    sqe = io_uring_get_sqe(ring)!!

    io_uring_prep_readv(sqe, index, iov[1].ptr, 1, 0)
    sqe.pointed.flags = sqe.pointed.flags.plus(IOSQE_FIXED_FILE).toUByte()
    sqe.pointed.user_data = 2u

    ret = io_uring_submit(ring)
    HasPosixErr.posixFailOn(ret != 1) {
        fprintf(stderr, "%s: got %d, wanted 1\n", __FUNCTION__, ret)
    }

    ret = io_uring_wait_cqe(ring, cqe.ptr)
    HasPosixErr.posixFailOn(ret < 0) {
        fprintf(stderr, "%s: io_uring_wait_cqe=%d\n", __FUNCTION__, ret)
    }
    HasPosixErr.posixFailOn(cqe.pointed!!.res != 4096) {
        fprintf(stderr, "%s: read cqe.pointed.res =%d\n", __FUNCTION__, cqe.pointed!!.res)
    }
    io_uring_cqe_seen(ring, cqe.value)

    val mustNotBe = memcmp(iov[1].iov_base, iov[0].iov_base, 4096.toULong())
    HasPosixErr.posixFailOn(mustNotBe.nz) {
        fprintf(stderr, "%s: data mismatch\n", __FUNCTION__)
    }

    free(iov[0].iov_base)
    free(iov[1].iov_base)
    return 0
}

fun adjust_nfiles(want_files: Int): Unit = nativeHeap.run {
    val rlim: rlimit = alloc()

    if (getrlimit(RLIMIT_NOFILE, rlim.ptr.reinterpret()) < 0)
        return
    if (rlim.rlim_cur >= want_files.toUInt())
        return
    rlim.rlim_cur = want_files.toULong()
    setrlimit(RLIMIT_NOFILE, rlim.ptr.reinterpret())
}

/*
 * Register 8K of sparse files, update one at a random spot, then do some
 * file IO to verify it works.
 */
fun test_huge(ring: CPointer<io_uring>): Int {
//    int * files
//    ret:Int
    val __FUNCTION__ = "test_huge"
    adjust_nfiles(16384)


    do {
        val files = open_files(0, 8192, 0)
        var ret = io_uring_register_files(ring, files.toCValues(), 8192)
        if (ret.nz) {
            /* huge sets not supported */
            if(ret == -EMFILE) {
                fprintf(stdout, "%s: No huge file set support, skipping\n", __FUNCTION__)
                break
            }
           HasPosixErr.posixFailOn(true) { "$__FUNCTION__: register ret=$ret"   }
        }

        files[7193] = open(".reg.7193", O_RDWR or O_CREAT,  644.fromOctal())
        HasPosixErr.posixFailOn(files[7193] < 0) {
            fprintf(stderr, "%s: open=%d\n", __FUNCTION__, errno)
        }

        ret = io_uring_register_files_update(ring, 7193, files.sliceArray(7193 until files.size).toCValues(), 1)
        HasPosixErr.posixFailOn(ret != 1) {
            fprintf(stderr, "%s: update ret=%d\n", __FUNCTION__, ret)
        }

        HasPosixErr.posixFailOn(test_fixed_read_write(ring, 7193).nz) { "test_fixed_read_write fail" }

        ret = io_uring_unregister_files(ring)
        HasPosixErr.posixFailOn(ret.nz) {
            fprintf(stderr, "%s: unregister ret=%d\n", __FUNCTION__, ret)
        }

        if (files[7193] != -1) {
            close(files[7193])
            unlink(".reg.7193")
        }
    } while (false)

    return 0
}

fun test_skip(ring: CPointer<io_uring>): Int {
//    int * files
//    ret:Int
val __FUNCTION__="test_skip"
    val files = open_files(100, 0, 0)
    var ret = io_uring_register_files(ring, files.toCValues(), 100)
    do {
        if (ret.nz) {
            fprintf(stderr, "%s: register ret=%d\n", __FUNCTION__, ret)
        }

        files[90] = IORING_REGISTER_FILES_SKIP
        ret = io_uring_register_files_update(ring, 90, files.sliceArray( 90 until files.size).toCValues(), 1)
        if (ret != 1) {
            if (ret == -EBADF) {
                fprintf(stdout, "Skipping files not supported\n")
                break
            }
             HasPosixErr.posixFailOn(true) { fprintf(stderr, "%s: update ret=%d\n", __FUNCTION__, ret) }
        }

        /* verify can still use file index 90 */
        if (test_fixed_read_write(ring, 90).nz)
                    ret = io_uring_unregister_files(ring)
        if (ret.nz) {
            fprintf(stderr, "%s: unregister ret=%d\n", __FUNCTION__, ret)
        }
    }while (false)

//    done:
    close_files(files, 100, 0)
    return 0
//    err:
//    close_files(files, 100, 0)
//    return 1
}

fun test_sparse_updates(): Int= memScoped{
val     ring:io_uring=alloc()
//    ret:Int, i, *fds, newfd

    var ret = io_uring_queue_init(8, ring.ptr, 0)
    if (ret.nz) {
        fprintf(stderr, "queue_init: %d\n", ret)
        return ret
    }

    val fds = IntArray(256){-1}

    ret = io_uring_register_files(ring.ptr, fds.refTo(0), 256)
    if (ret.nz) {
        fprintf(stderr, "file_register: %d\n", ret)
        return ret
    }

    val newfd: IntVarOf<Int> = alloc<IntVar>{value=1}
    for (i in 0 until  256 ) {
        ret = io_uring_register_files_update(ring.ptr, i.toUInt(), newfd.ptr, 1)
        if (ret != 1) {
            fprintf(stderr, "file_update: %d\n", ret)
            return ret
        }
    }
    io_uring_unregister_files(ring.ptr)


    fds .fill(1)

    ret = io_uring_register_files(ring.ptr, fds.refTo(0), 256)
    if (ret.nz) {
        fprintf(stderr, "file_register: %d\n", ret)
        return ret
    }

    newfd .value=-1
    for (i in 0 until 256 ) {
        ret = io_uring_register_files_update(ring.ptr, i.toUInt(), newfd.ptr, 1)
        if (ret != 1) {
            fprintf(stderr, "file_update: %d\n", ret)
            return ret
        }
    }
    io_uring_unregister_files(ring.ptr)

    io_uring_queue_exit(ring.ptr)
    return 0
}

fun test_fixed_removal_ordering( ) : Int=memScoped{
    val buffer=ByteArray(128)
    val ring:io_uring=alloc()
    val ts:__kernel_timespec = alloc()


    var ret = io_uring_queue_init(8, ring.ptr, 0)
    if (ret < 0) {
        fprintf(stderr, "failed to init io_uring: %s\n", strerror(-ret))
        return ret
    }
//    val __pipedes = IntArray(2)
//    val __pipedes1 = __pipedes.refTo(0)
    val fds=allocArray<IntVar>(2)
    val linuxUringpipe = linux_uringPipe(fds)
    if (linuxUringpipe.nz) {
        perror("pipe")
        return -1
    }
    ret = io_uring_register_files(ring.ptr, fds, 2)
    if (ret.nz) {
        fprintf(stderr, "file_register: %d\n", ret)
        return ret
    }
    /* ring should have fds referenced, can close them */
    close(fds[0])
    close(fds[1])

    var sqe = io_uring_get_sqe(ring.ptr)!!

    /* outwait file recycling delay */
    ts.tv_sec = 3
    ts.tv_nsec = 0
    io_uring_prep_timeout(sqe, ts.ptr, 0, 0)
    sqe.pointed.flags = sqe.pointed.flags.or((IOSQE_IO_LINK or IOSQE_IO_HARDLINK).toUByte())
    sqe.pointed.user_data = 1u

    sqe = io_uring_get_sqe(ring.ptr)!!
    io_uring_prep_write(sqe, 1, buffer.toCValues(), buffer.toCValues().size.toUInt(), 0)
    sqe.pointed.flags = sqe.pointed.flags.plus(IOSQE_FIXED_FILE).toUByte()
    sqe.pointed.user_data = 2u

    ret = io_uring_submit(ring.ptr)
    if (ret != 2) {
        fprintf(stderr, "%s: got %d, wanted 2\n", "test_fixed_removal_ordering", ret)
        return -1
    }

    /* remove unused pipe end */
    var fd :IntVar =alloc{ value =  -1 }
    ret = io_uring_register_files_update(ring.ptr, 0, fd.ptr, 1)
    if (ret != 1) {
        fprintf(stderr, "update off=0 failed\n")
        return -1
    }

    /* remove used pipe end */
    fd.value = -1
    ret = io_uring_register_files_update(ring.ptr, 1, fd.ptr, 1)
    if (ret != 1) {
        fprintf(stderr, "update off=1 failed\n")
        return -1
    }

    val cqe  =alloc<CPointerVar<io_uring_cqe>>()
    repeat(2){i->
        ret = io_uring_wait_cqe(ring.ptr, cqe.ptr )
        if (ret < 0) {
            fprintf(stderr, "%s: io_uring_wait_cqe=%d\n", "test_fixed_removal_ordering", ret)
            return 1
        }
        io_uring_cqe_seen(ring.ptr, cqe.value)
    }

    io_uring_queue_exit(ring.ptr)
    return 0
}


fun main(): Unit = nativeHeap.run {
    val ring: io_uring = alloc()

    val __buf = malloc(1024)
    val getcwd = getcwd(__buf!!.reinterpret() ,1024 )
    println("cwd: ${getcwd!!.toKString()}")

    var ret = io_uring_queue_init(8, ring.ptr, 0)
    HasPosixErr.posixFailOn (ret.nz) {
        printf("ring setup failed\n")
    }

    ret = test_basic(ring.ptr, 0)
    HasPosixErr.posixFailOn (ret.nz) {
        printf("test_basic failed\n")
    }

    ret = test_basic(ring.ptr, 1)
    HasPosixErr.posixFailOn (ret.nz) {
        printf("test_basic failed\n")
    }

    ret = test_basic_many(ring.ptr)
    HasPosixErr.posixFailOn (ret.nz) {
        printf("test_basic_many failed\n")
    }

    ret = test_sparse(ring.ptr)
   HasPosixErr.posixFailOn (ret.nz) {
       printf("test_sparse failed\n")
   }
    if (no_update.nz)
        return

    ret = test_additions(ring.ptr)
    HasPosixErr.posixFailOn(ret.nz) {
        printf("test_additions failed\n")
    }

    ret = test_removals(ring.ptr)
    HasPosixErr.posixFailOn(ret.nz) {
        printf("test_removals failed\n")
    }

    ret = test_replace(ring.ptr)
    HasPosixErr.posixFailOn(ret.nz) {
        printf("test_replace failed\n")
    }

    ret = test_replace_all(ring.ptr)
    HasPosixErr.posixFailOn(ret.nz) {
        printf("test_replace_all failed\n")
    }

    ret = test_grow(ring.ptr)
    HasPosixErr.posixFailOn(ret.nz) {
        printf("test_grow failed\n")
    }

    ret = test_shrink(ring.ptr)
    HasPosixErr.posixFailOn(ret.nz) {
        printf("test_shrink failed\n")
    }

    ret = test_zero(ring.ptr)
    HasPosixErr.posixFailOn(ret.nz) {
        printf("test_zero failed\n")
    }

    ret = test_huge(ring.ptr)
    HasPosixErr.posixFailOn(ret.nz) {
        printf("test_huge failed\n")
    }

    ret = test_skip(ring.ptr)
    HasPosixErr.posixFailOn(ret.nz) {
        printf("test_skip failed\n")
    }

    ret = test_sparse_updates()
    HasPosixErr.posixFailOn(ret.nz) {
        printf("test_sparse_updates failed\n")
    }

    ret = test_fixed_removal_ordering()
    HasPosixErr.posixFailOn(ret.nz) {
        printf("test_fixed_removal_ordering failed\n")
    }

    return
}
