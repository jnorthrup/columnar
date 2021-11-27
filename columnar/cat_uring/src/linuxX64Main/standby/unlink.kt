/* SPDX-License-Identifier: MIT */
/*
 * Description: run various nop tests
 *
 */
#include <errno.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <sys/stat.h>

#include "liburing.h"

static test_unlink:Int(ring:CPointer<io_uring>, old:String) {
    cqe:CPointer<io_uring_cqe>;
    sqe:CPointer<io_uring_sqe>;
    ret:Int;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "get sqe failed\n");
        goto err;
    }
    io_uring_prep_unlinkat(sqe, AT_FDCWD, old, 0);

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
    return ret;
    err:
    return 1;
}

static stat_file:Int(buf:String) {
    sb:stat;

    if (!stat(buf, sb.ptr))
        return 0;

    return errno;
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    ring:io_uring;
    char buf[32] = "./XXXXXX";
    ret:Int;

    if (argc > 1)
        return 0;

    ret = io_uring_queue_init(1, ring.ptr, 0);
    if (ret) {
        fprintf(stderr, "ring setup failed: %d\n", ret);
        return 1;
    }

    ret = mkstemp(buf);
    if (ret < 0) {
        perror("mkstemp");
        return 1;
    }
    close(ret);

    if (stat_file(buf) != 0) {
        perror("stat");
        return 1;
    }

    ret = test_unlink(ring.ptr, buf);
    if (ret < 0) {
        if (ret == -EBADF || ret == -EINVAL) {
            fprintf(stdout, "Unlink not supported, skipping\n");
            unlink(buf);
            return 0;
        }
        fprintf(stderr, "rename: %s\n", strerror(-ret));
        goto err;
    } else if (ret)
        goto err;

    ret = stat_file(buf);
    if (ret != ENOENT) {
        fprintf(stderr, "stat got %s\n", strerror(ret));
        return 1;
    }

    ret = test_unlink(ring.ptr, "/3/2/3/1/z/y");
    if (ret != -ENOENT) {
        fprintf(stderr, "invalid unlink got %s\n", strerror(-ret));
        return 1;
    }

    return 0;
    err:
    unlink(buf);
    return 1;
}
