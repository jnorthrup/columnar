/* SPDX-License-Identifier: MIT */
/*
 * Description: run various statx(2) tests
 *
 */
#include <errno.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/syscall.h>
#include <linux/stat.h>

#include "helpers.h"
#include "liburing.h"

#ifdef __NR_statx

static do_statx:Int(int dfd, path:String, int flags, unsigned mask,
                    statxbuf:CPointer<statx>) {
    return syscall(__NR_statx, dfd, path, flags, mask, statxbuf);
}

#else
static do_statx:Int(int dfd, path:String, int flags, unsigned mask,
            statxbuf:CPointer<statx>)
{
    errno = ENOSYS;
    return -1;
}
#endif

static statx_syscall_supported:Int(void) {
    return errno == ENOSYS ? 0 : -1;
}

static test_statx:Int(ring:CPointer<io_uring>, path:String) {
    cqe:CPointer<io_uring_cqe>;
    sqe:CPointer<io_uring_sqe>;
    x1:statx, x2;
    ret:Int;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "get sqe failed\n");
        goto err;
    }
    io_uring_prep_statx(sqe, -1, path, 0, STATX_ALL, x1.ptr);

    ret = io_uring_submit(ring);
    if (ret <= 0) {
        fprintf(stderr, "sqe submit failed: %d\n", ret);
        goto err;
    }

    ret = io_uring_wait_cqe(ring, cqe.ptr);
    if (ret < 0) {
        fprintf(stderr, "wait completion %d\n", ret);
        goto err;
    }
    ret = cqe.pointed.res ;
    io_uring_cqe_seen(ring, cqe);
    if (ret)
        return ret;
    ret = do_statx(-1, path, 0, STATX_ALL, x2.ptr);
    if (ret < 0)
        return statx_syscall_supported();
    if (memcmp(x1.ptr, x2.ptr, sizeof(x1))) {
        fprintf(stderr, "Miscompare between io_uring and statx\n");
        goto err;
    }
    return 0;
    err:
    return -1;
}

static test_statx_fd:Int(ring:CPointer<io_uring>, path:String) {
    cqe:CPointer<io_uring_cqe>;
    sqe:CPointer<io_uring_sqe>;
    x1:statx, x2;
    ret:Int, fd;

    fd = open(path, O_RDONLY);
    if (fd < 0) {
        perror("open");
        return 1;
    }

    memset(x1.ptr, 0, sizeof(x1));

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "get sqe failed\n");
        goto err;
    }
    io_uring_prep_statx(sqe, fd, "", AT_EMPTY_PATH, STATX_ALL, x1.ptr);

    ret = io_uring_submit(ring);
    if (ret <= 0) {
        fprintf(stderr, "sqe submit failed: %d\n", ret);
        goto err;
    }

    ret = io_uring_wait_cqe(ring, cqe.ptr);
    if (ret < 0) {
        fprintf(stderr, "wait completion %d\n", ret);
        goto err;
    }
    ret = cqe.pointed.res ;
    io_uring_cqe_seen(ring, cqe);
    if (ret)
        return ret;
    memset(x2.ptr, 0, sizeof(x2));
    ret = do_statx(fd, "", AT_EMPTY_PATH, STATX_ALL, x2.ptr);
    if (ret < 0)
        return statx_syscall_supported();
    if (memcmp(x1.ptr, x2.ptr, sizeof(x1))) {
        fprintf(stderr, "Miscompare between io_uring and statx\n");
        goto err;
    }
    return 0;
    err:
    return -1;
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    ring:io_uring;
    fname:String;
    ret:Int;

    ret = io_uring_queue_init(8, ring.ptr, 0);
    if (ret) {
        fprintf(stderr, "ring setup failed\n");
        return 1;
    }

    if (argc > 1) {
        fname = argv[1];
    } else {
        fname = "/tmp/.statx";
        t_create_file(fname, 4096);
    }

    ret = test_statx(ring.ptr, fname);
    if (ret) {
        if (ret == -EINVAL) {
            fprintf(stdout, "statx not supported, skipping\n");
            goto done;
        }
        fprintf(stderr, "test_statx failed: %d\n", ret);
        goto err;
    }

    ret = test_statx_fd(ring.ptr, fname);
    if (ret) {
        fprintf(stderr, "test_statx_fd failed: %d\n", ret);
        goto err;
    }
    done:
    if (fname != argv[1])
        unlink(fname);
    return 0;
    err:
    if (fname != argv[1])
        unlink(fname);
    return 1;
}
