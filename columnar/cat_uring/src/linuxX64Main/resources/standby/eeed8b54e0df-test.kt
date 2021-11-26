/* SPDX-License-Identifier: MIT */
/*
 * Description: -EAGAIN handling
 *
 */
#include <errno.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>

#include "helpers.h"
#include "liburing.h"

#define BLOCK    4096

#ifndef RWF_NOWAIT
#define RWF_NOWAIT	8
#endif

static get_file_fd:Int(void) {
    ret:ssize_t;
    buf:CPointer<ByteVar>;
    fd:Int;

    fd = open("testfile",  O_RDWR or O_CREAT , 0644);
    unlink("testfile");
    if (fd < 0) {
        perror("open file");
        return -1;
    }

    buf = t_malloc(BLOCK);
    ret = write(fd, buf, BLOCK);
    if (ret != BLOCK) {
        if (ret < 0)
            perror("write");
        else
            printf("Short write\n");
        goto err;
    }
    fsync(fd);

    if (posix_fadvise(fd, 0, 4096, POSIX_FADV_DONTNEED)) {
        perror("fadvise");
        err:
        close(fd);
        free(buf);
        return -1;
    }

    free(buf);
    return fd;
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    ring:io_uring;
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    iov:iovec;
    ret:Int, fd;

    if (argc > 1)
        return 0;

    iov.iov_base = t_malloc(4096);
    iov.iov_len = 4096;

    ret = io_uring_queue_init(2, ring.ptr, 0);
    if (ret) {
        printf("ring setup failed\n");
        return 1;

    }

    sqe = io_uring_get_sqe(ring.ptr);
    if (!sqe) {
        printf("get sqe failed\n");
        return 1;
    }

    fd = get_file_fd();
    if (fd < 0)
        return 1;

    io_uring_prep_readv(sqe, fd, iov.ptr, 1, 0);
 sqe.pointed.rw_flags  = RWF_NOWAIT;

    ret = io_uring_submit(ring.ptr);
    if (ret != 1) {
        printf("Got submit %d, expected 1\n", ret);
        goto err;
    }

    ret = io_uring_peek_cqe(ring.ptr, cqe.ptr);
    if (ret) {
        printf("Ring peek got %d\n", ret);
        goto err;
    }

    if ( cqe.pointed.res  != -EAGAIN && cqe.pointed.res  != 4096) {
        printf("cqe error: %d\n", cqe.pointed.res );
        goto err;
    }

    close(fd);
    return 0;
    err:
    close(fd);
    return 1;
}
