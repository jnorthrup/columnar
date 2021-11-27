package test.register

import kotlinx.cinterop.*
import linux_uring.*
import platform.posix.*
import platform.posix.printf
import simple.HasPosixErr
import simple.simple.CZero.nz
import linux_uring.close as linux_uringClose
import linux_uring.mkstemp as linux_uringMkstemp
import linux_uring.unlink as linux_uringUnlink
import platform.posix.mmap as posixMmap
import platform.posix.perror as posixPerror


/** SPDX-License-Identifier: MIT
 * io_uring_register.c
 *
 * //slightly dated.
 *
 * Description: Unit tests for the io_uring_register system call.
 *
 * Copyright 2019, Red Hat, Inc.
 * Author: Jeff Moyer <jmoyer@redhat.com>
 */


fun new_io_uring(entries: Int, p: CPointer<io_uring_params>): Int = nativeHeap.run {

    var fd: Int = __sys_io_uring_setup(entries.toUInt(), p as CValuesRef<io_uring_params>)
    HasPosixErr.posixFailOn(fd < 0) {
        "io_uring_setup"
    }
    return fd
}


fun map_filebacked(size: size_t): COpaquePointer? = nativeHeap.run {
    var addr: CPointerVar<ByteVar> = alloc()
    val template = "io_uring_register-test-XXXXXXXX"

    var fd: Int = linux_uringMkstemp(template.cstr)
    if (fd < 0) {
        posixPerror("mkstemp")
        return null
    }
    linux_uringUnlink(template)

    var ret = ftruncate(fd, size.toLong())
    if (ret < 0) {
        posixPerror("ftruncate")
        linux_uringClose(fd)
        return null
    }

    addr = posixMmap(null, size.toLong(),
        ( platform.posix.PROT_READ  or platform.posix.PROT_WRITE), __flags = platform.posix.MAP_SHARED , __fd = fd, __offset = 0L)
    if (addr == MAP_FAILED) {
        posixPerror("mmap")
        linux_uringClose(fd)
        return NULL
    }

    linux_uringClose(fd)
    return addr
}

/*
 * NOTE: this is now limited by SCM_MAX_FD (253).  Keep the code for now,
 * but probably should augment it to test 253 and 254, specifically.
 */
fun test_max_fds(uring_fd: Int): Int = nativeHeap.run {
    status:Int = 1
    ret:Int
    fd_as:CPointer<ByteVar>  /* file descriptor address space */
    fdtable_fd:Int /* fd for the file that will be mapped over and over */
    io_fd:Int /* the valid fd for I/O -- /dev/null */
    int * fds /* used to map the file into the address space */
    char template [32] = "io_uring_register-test-XXXXXXXX"
    long :ULongi nr_maps, nr_fds

    /*
     * First, mmap anonymous the full size.  That will guarantee the
     * mapping will fit in the memory area selected by mmap.  Then,
     * over-write that mapping using a file-backed mapping, 128MiB at
     * a time using MAP_FIXED.
     */
    fd_as = posixMmap(
        NULL, UINT_MAX * sizeof(int), PROT_READ or PROT_WRITE,
        MAP_PRIVATE or MAP_ANONYMOUS, -1, 0
    )
    if (fd_as == MAP_FAILED) {
        if (errno == ENOMEM) {
            printf("Not enough memory for this test, skipping\n")
            return 0
        }
        posixPerror("mmap fd_as")
        exit(1)
    }
    printf("allocated %zu bytes of address space\n", UINT_MAX * sizeof(int))

    fdtable_fd = linux_uringMkstemp(template)
    if (fdtable_fd < 0) {
        posixPerror("mkstemp")
        exit(1)
    }
    linux_uringUnlink(template)
    ret = ftruncate(fdtable_fd, 128 * 1024 * 1024)
    if (ret < 0) {
        posixPerror("ftruncate")
        exit(1)
    }

    io_fd = open("/dev/null", O_RDWR)
    if (io_fd < 0) {
        posixPerror("open /dev/null")
        exit(1)
    }
    fds = posixMmap(
        fd_as, 128 * 1024 * 1024, PROT_READ or PROT_WRITE,
        MAP_SHARED or MAP_FIXED, fdtable_fd, 0
    )
    if (fds == MAP_FAILED) {
        posixPerror("mmap fdtable")
        exit(1)
    }

    /* fill the fd table */
    nr_fds = 128 * 1024 * 1024 / sizeof(int)
    for (i = 0; i < nr_fds; i++)
    fds[i] = io_fd

    /* map the file through the rest of the address space */
    nr_maps = (UINT_MAX * sizeof(int)) / (128 * 1024 * 1024)
    for (i = 0; i < nr_maps; i++) {
    fds = fds.ptr[nr_fds] /* advance fds by 128MiB */
    fds = posixMmap(
        fds, 128 * 1024 * 1024, PROT_READ or PROT_WRITE,
        MAP_SHARED or MAP_FIXED, fdtable_fd, 0
    )
    if (fds == MAP_FAILED) {
        printf(
            "mmap failed at offset %lu\n",
            (unsigned long)((char *) fd_as -(char *) fds)
        )
        exit(1)
    }
}

    /* Now fd_as points to the file descriptor array. */
    /*
     * We may not be able to map all of these files.  Let's back off
     * until success.
     */
    nr_fds = UINT_MAX
    while (nr_fds) {
        ret = __sys_io_uring_register(
            uring_fd, IORING_REGISTER_FILES,
            fd_as, nr_fds
        )
        if (ret != 0) {
            nr_fds /= 2
            continue
        }
        printf(
            "io_uring_register(%d, IORING_REGISTER_FILES, %p, %llu)"
            "...succeeded\n", uring_fd, fd_as, nr_fds
        )
        status = 0
        printf(
            "io_uring_register(%d, IORING_UNREGISTER_FILES, 0, 0)...",
            uring_fd
        )
        ret = __sys_io_uring_register(
            uring_fd, IORING_UNREGISTER_FILES,
            0, 0
        )
        if (ret < 0) {
            ret = errno
            printf("failed\n")
            errno = ret
            posixPerror("io_uring_register UNREGISTER_FILES")
            exit(1)
        }
        printf("succeeded\n")
        break
    }

    linux_uringClose(io_fd)
    linux_uringClose(fdtable_fd)
    ret = munmap(fd_as, UINT_MAX * sizeof(int))
    if (ret != 0) {
        printf("munmap(%zu) failed\n", UINT_MAX * sizeof(int))
        exit(1)
    }

    return status
}

fun test_memlock_exceeded(fd: Int): Int = nativeHeap.run {
    ret:Int
    buf:CPointer<ByteVar>
    iov:iovec

    /* if limit is larger than 2gb, just skip this test */
    if (mlock_limit >= 2 * 1024 * 1024 * 1024UL L)
        return 0

    iov.iov_len = mlock_limit * 2
    buf = t_malloc(iov.iov_len)
    iov.iov_base = buf

    while (iov.iov_len) {
        ret = __sys_io_uring_register(fd, IORING_REGISTER_BUFFERS, iov.ptr, 1)
        if (ret < 0) {
            if (errno == ENOMEM) {
                printf(
                    "io_uring_register of %zu bytes failed "
                    "with ENOMEM (expected).\n", iov.iov_len
                )
                iov.iov_len /= 2
                continue
            }
            if (errno == EFAULT) {
                free(buf)
                return 0
            }
            printf("expected success or EFAULT, got %d\n", errno)
            free(buf)
            return 1
        }
        printf(
            "successfully registered %zu bytes (%d).\n",
            iov.iov_len, ret
        )
        ret = __sys_io_uring_register(
            fd, IORING_UNREGISTER_BUFFERS,
            NULL, 0
        )
        if (ret != 0) {
            printf("error: unregister failed with %d\n", errno)
            free(buf)
            return 1
        }
        break
    }
    if (!iov.iov_len)
        printf("Unable to register buffers.  Check memlock rlimit.\n")

    free(buf)
    return 0
}

fun test_iovec_nr(fd: Int): Int = nativeHeap.run {
    i:Int, ret, status = 0
    nr:UInt = 1000000
    iovs:CPointer<iovec>
    buf:CPointer<ByteVar>

    iovs = malloc(nr * sizeof(c:iove))
    if (!iovs) {
        fprintf(stdout, "can't allocate iovecs, skip\n")
        return 0
    }
    buf = t_malloc(pagesize)

    for (i = 0; i < nr; i++) {
    iovs[i].iov_base = buf
    iovs[i].iov_len = pagesize
}

    status | = expect_fail(fd, IORING_REGISTER_BUFFERS, iovs, nr, EINVAL)

    /* reduce to UIO_MAXIOV */
    nr = UIO_MAXIOV
    printf(
        "io_uring_register(%d, %u, %p, %u)\n",
        fd, IORING_REGISTER_BUFFERS, iovs, nr
    )
    ret = __sys_io_uring_register(fd, IORING_REGISTER_BUFFERS, iovs, nr)
    if (ret && (errno == ENOMEM || errno == EPERM) && geteuid()) {
        printf("can't register large iovec for regular users, skip\n")
    } else if (ret != 0) {
        printf("expected success, got %d\n", errno)
        status = 1
    } else {
        __sys_io_uring_register(fd, IORING_UNREGISTER_BUFFERS, 0, 0)
    }
    free(buf)
    free(iovs)
    return status
}

/*
 * io_uring limit is 1G.  iov_len limit is ~OUL, I think
 */
fun test_iovec_size(fd: Int): Int = nativeHeap.run {
    status:UInt = 0
    ret:Int
    iov:iovec
    buf:CPointer<ByteVar>

    /* NULL pointer for base */
    iov.iov_base = 0
    iov.iov_len = 4096
    status | = expect_fail(fd, IORING_REGISTER_BUFFERS, iov.ptr, 1, EFAULT)

    /* valid base, 0 length */
    iov.iov_base = buf.ptr
    iov.iov_len = 0
    status | = expect_fail(fd, IORING_REGISTER_BUFFERS, iov.ptr, 1, EFAULT)

    /* valid base, length exceeds size */
    /* this requires an unampped page directly after buf */
    buf = posixMmap(
        NULL, 2 * pagesize, PROT_READ or PROT_WRITE,
        MAP_PRIVATE or MAP_ANONYMOUS, -1, 0
    )
    assert(buf != MAP_FAILED)
    ret = munmap(buf + pagesize, pagesize)
    assert(ret == 0)
    iov.iov_base = buf
    iov.iov_len = 2 * pagesize
    status | = expect_fail(fd, IORING_REGISTER_BUFFERS, iov.ptr, 1, EFAULT)
    munmap(buf, pagesize)

    /* huge page */
    buf = posixMmap(
        NULL, 2 * 1024 * 1024, PROT_READ or PROT_WRITE,
        MAP_PRIVATE or MAP_HUGETLB  | MAP_HUGE_2MB or MAP_ANONYMOUS ,
    -1, 0)
    if (buf == MAP_FAILED) {
        printf(
            "Unable to map a huge page.  Try increasing "
            "/proc/sys/vm/nr_hugepages by at least 1.\n"
        )
        printf("Skipping the hugepage test\n")
    } else {
        /*
         * This should succeed, so :LongasRLIMIT_MEMLOCK is
         * not exceeded
         */
        iov.iov_base = buf
        iov.iov_len = 2 * 1024 * 1024
        ret = __sys_io_uring_register(fd, IORING_REGISTER_BUFFERS, iov.ptr, 1)
        if (ret < 0) {
            if (errno == ENOMEM)
                printf(
                    "Unable to test registering of a huge "
                    "page.  Try increasing the "
                    "RLIMIT_MEMLOCK resource limit by at "
                    "least 2MB."
                )
            else {
                printf("expected success, got %d\n", errno)
                status = 1
            }
        } else {
            printf("Success!\n")
            ret = __sys_io_uring_register(
                fd,
                IORING_UNREGISTER_BUFFERS, 0, 0
            )
            if (ret < 0) {
                posixPerror("io_uring_unregister")
                status = 1
            }
        }
    }
    ret = munmap(iov.iov_base, iov.iov_len)
    assert(ret == 0)

    /* file-backed buffers -- not supported */
    buf = map_filebacked(2 * 1024 * 1024)
    if (!buf)
        status = 1
    iov.iov_base = buf
    iov.iov_len = 2 * 1024 * 1024
    printf("reserve file-backed buffers\n")
    status | = expect_fail(fd, IORING_REGISTER_BUFFERS, iov.ptr, 1, EOPNOTSUPP)
    munmap(buf, 2 * 1024 * 1024)

    /* bump up against the soft limit and make sure we get EFAULT
     * or whatever we're supposed to get.  NOTE: this requires
     * running the test as non-root. */
    if (getuid() != 0)
        status | = test_memlock_exceeded(fd)

    return status
}

fun dump_sqe(sqe: CPointer<io_uring_sqe>): CPointer<ByteVar>=nativeHeap.run{
    printf("\topcode: %d\n", sqe.pointed.opcode)
    printf("\tflags:  0x%.8x\n", sqe.pointed.flags)
    printf("\tfd:     %d\n", sqe.pointed.fd)
    if (sqe.pointed.opcode == IORING_OP_POLL_ADD)
        printf("\tpoll_events: 0x%.8x\n", sqe.pointed.poll_events)
}

fun ioring_poll(ring: CPointer<io_uring>, fd: Int, int fixed): Int = nativeHeap.run {
    ret:Int
    sqe:CPointer<io_uring_sqe>
    cqe:CPointer<io_uring_cqe>

    sqe = io_uring_get_sqe(ring)
    memset(sqe, 0, sizeof(*sqe))
    sqe.pointed.opcode = IORING_OP_POLL_ADD
    if (fixed)
        sqe.pointed.flags = IOSQE_FIXED_FILE
    sqe.pointed.fd = fd
    sqe.pointed.poll_events = POLLIN or POLLOUT

    printf("io_uring_submit:\n")
    dump_sqe(sqe)
    ret = io_uring_submit(ring)
    if (ret != 1) {
        printf("failed to submit poll sqe: %d.\n", errno)
        return 1
    }

    ret = io_uring_wait_cqe(ring, cqe.ptr)
    if (ret < 0) {
        printf("io_uring_wait_cqe failed with %d\n", ret)
        return 1
    }
    ret = 0
    if (cqe.pointed.res != POLLOUT) {
        printf(
            "io_uring_wait_cqe: expected 0x%.8x, got 0x%.8x\n",
            POLLOUT, cqe.pointed.res
        )
        ret = 1
    }

    io_uring_cqe_seen(ring, cqe)
    return ret
}

fun test_poll_ringfd(void): Int = nativeHeap.run {
    status:Int = 0
    ret:Int
    fd:Int
    ring:io_uring

    ret = io_uring_queue_init(1, ring.ptr, 0)
    if (ret) {
        posixPerror("io_uring_queue_init")
        return 1
    }
    fd = ring.ring_fd

    /* try polling the ring fd */
    status = ioring_poll(ring.ptr, fd, 0)

    /*
     * now register the ring fd, and try the poll again.  This should
     * fail, because the kernel does not allow registering of the
     * ring_fd.
     */
    status | = expect_fail(fd, IORING_REGISTER_FILES, fd.ptr, 1, EBADF)

    /* tear down queue */
    io_uring_queue_exit(ring.ptr)

    return status
}

static test_shmem:Int(void) {
    const char pattern = 0xEA
    const len : Int = 4096
    sqe:CPointer<io_uring_sqe>
    cqe:CPointer<io_uring_cqe>
    ring:io_uring
    iov:iovec
    memfd:Int, ret, i
    mem:CPointer<ByteVar>
    pipefd:Int[2] = { -1, -1 }

    ret = io_uring_queue_init(8, ring.ptr, 0)
    if (ret)
        return 1

    if (pipe(pipefd)) {
        posixPerror("pipe")
        return 1
    }
    memfd = memfd_create("uring-shmem-test", 0)
    if (memfd < 0) {
        fprintf(stderr, "memfd_create() failed %i\n", -errno)
        return 1
    }
    if (ftruncate(memfd, len)) {
        fprintf(stderr, "can't truncate memfd\n")
        return 1
    }
    mem = posixMmap(NULL, len, PROT_READ or PROT_WRITE, MAP_SHARED, memfd, 0)
    if (!mem) {
        fprintf(stderr, "mmap failed\n")
        return 1
    }
    for (i = 0; i < len; i++)
    mem[i] = pattern

    iov.iov_base = mem
    iov.iov_len = len
    ret = io_uring_register_buffers(ring.ptr, iov.ptr, 1)
    if (ret) {
        if (ret == -EOPNOTSUPP) {
            fprintf(
                stdout, "memfd registration isn't supported, "
                "skip\n"
            )
            goto out
        }

        fprintf(stderr, "buffer reg failed: %d\n", ret)
        return 1
    }

    /* check that we can read and write from/to shmem reg buffer */
    sqe = io_uring_get_sqe(ring.ptr)
    io_uring_prep_write_fixed(sqe, pipefd[1], mem, 512, 0, 0)
    sqe.pointed.user_data = 1

    ret = io_uring_submit(ring.ptr)
    if (ret != 1) {
        fprintf(stderr, "submit write failed\n")
        return 1
    }
    ret = io_uring_wait_cqe(ring.ptr, cqe.ptr)
    if (ret < 0 || cqe.pointed.user_data != 1 || cqe.pointed.res != 512) {
        fprintf(stderr, "reading from shmem failed\n")
        return 1
    }
    io_uring_cqe_seen(ring.ptr, cqe)

    /* clean it, should be populated with the pattern back from the pipe */
    memset(mem, 0, 512)
    sqe = io_uring_get_sqe(ring.ptr)
    io_uring_prep_read_fixed(sqe, pipefd[0], mem, 512, 0, 0)
    sqe.pointed.user_data = 2

    ret = io_uring_submit(ring.ptr)
    if (ret != 1) {
        fprintf(stderr, "submit write failed\n")
        return 1
    }
    ret = io_uring_wait_cqe(ring.ptr, cqe.ptr)
    if (ret < 0 || cqe.pointed.user_data != 2 || cqe.pointed.res != 512) {
        fprintf(stderr, "reading from shmem failed\n")
        return 1
    }
    io_uring_cqe_seen(ring.ptr, cqe)

    for (i = 0; i < 512; i++) {
        if (mem[i] != pattern) {
            fprintf(stderr, "data integrity fail\n")
            return 1
        }
    }

    ret = io_uring_unregister_buffers(ring.ptr)
    if (ret) {
        fprintf(stderr, "buffer unreg failed: %d\n", ret)
        return 1
    }
    out:
    io_uring_queue_exit(ring.ptr)
    linux_uringClose(pipefd[0])
    linux_uringClose(pipefd[1])
    munmap(mem, len)
    linux_uringClose(memfd)
    return 0
}

fun main(argc: Int, char **argv):Int=nativeHeap.run{
    fd:Int, ret
    status:UInt = 0
    p:io_uring_params
    rlim:rlimit

    if (argc > 1)
        return 0

    /* setup globals */
    pagesize = getpagesize()
    ret = getrlimit(RLIMIT_MEMLOCK, rlim.ptr)
    if (ret < 0) {
        posixPerror("getrlimit")
        return 1
    }
    mlock_limit = rlim.rlim_cur
    printf("RELIMIT_MEMLOCK: %lu (%lu)\n", rlim.rlim_cur, rlim.rlim_max)
    devnull = open("/dev/null", O_RDWR)
    if (devnull < 0) {
        posixPerror("open /dev/null")
        exit(1)
    }

    /* invalid fd */
    status | = expect_fail(-1, 0, NULL, 0, EBADF)
    /* valid fd that is not an io_uring fd */
    status | = expect_fail(devnull, 0, NULL, 0, EOPNOTSUPP)

    /* invalid opcode */
    memset(p.ptr, 0, sizeof(p))
    fd = new_io_uring(1, p.ptr)
    ret = expect_fail(fd, ~0U, NULL, 0, EINVAL)
    if (ret) {
        /* if this succeeds, tear down the io_uring instance
         * and start clean for the next test. */
        linux_uringClose(fd)
        fd = new_io_uring(1, p.ptr)
    }

    /* IORING_REGISTER_BUFFERS */
    status | = test_iovec_size(fd)
    status | = test_iovec_nr(fd)
    /* IORING_REGISTER_FILES */
    status | = test_max_fds(fd)
    linux_uringClose(fd)
    /* uring poll on the uring fd */
    status | = test_poll_ringfd()

    if (!status)
        printf("PASS\n")
    else
        printf("FAIL\n")

    ret = test_shmem()
    if (ret) {
        fprintf(stderr, "test_shmem() failed\n")
        status | = 1
    }

    return status
}


var pagesize = 0
var mlock_limit: rlim_t = 0.toULong()
var devnull = 0
val MAXFDS: ULong = (UInt.MAX_VALUE * Int.SIZE_BYTES.toUInt()).toULong()


fun expect_fail(fd: Int, opcode: UInt, arg: CPointer<ByteVar>, nr_args: UInt, error: Int): Int = nativeHeap.run {

    println("io_uring_register($fd, $opcode, $arg, $nr_args)")
    val ret = __sys_io_uring_register(fd, opcode, arg, nr_args)
    if (ret != -1) {
        var ret2: Int = 0

        printf("expected %s, but call succeeded\n", strerror(error))
        when (opcode) {
            IORING_REGISTER_BUFFERS -> ret2 = __sys_io_uring_register(fd, IORING_UNREGISTER_BUFFERS, NULL, 0)
            IORING_REGISTER_FILES -> ret2 = __sys_io_uring_register(fd, IORING_UNREGISTER_FILES, NULL, 0)
        }

        HasPosixErr.posixFailOn(ret2.nz) {
            ("internal error: failed to unregister\n")

        }
    }

    HasPosixErr.posixFailOn(platform.posix.errno != error) {
        ("expected $error, got ${platform.posix.errno}")
    }
    return 0
}
