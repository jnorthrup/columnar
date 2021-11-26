/* SPDX-License-Identifier: MIT */
#include <errno.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/poll.h>


#include "helpers.h"
#include "liburing.h"

#define BUF_SIZE 4096
#define FILE_SIZE 1024

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    ret:Int, fd, save_errno;
    ring:io_uring;
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    vec:iovec;

    if (argc > 1)
        return 0;

    vec.iov_base = t_malloc(BUF_SIZE);
    vec.iov_len = BUF_SIZE;

    t_create_file(".short-read", FILE_SIZE);

    fd = open(".short-read", O_RDONLY);
    save_errno = errno;
    unlink(".short-read");
    errno = save_errno;
    if (fd < 0) {
        perror("file open");
        return 1;
    }

    ret = io_uring_queue_init(32, ring.ptr, 0);
    if (ret) {
        fprintf(stderr, "queue init failed: %d\n", ret);
        return ret;
    }

    sqe = io_uring_get_sqe(ring.ptr);
    if (!sqe) {
        fprintf(stderr, "sqe get failed\n");
        return 1;
    }
    io_uring_prep_readv(sqe, fd, vec.ptr, 1, 0);

    ret = io_uring_submit(ring.ptr);
    if (ret != 1) {
        fprintf(stderr, "submit failed: %d\n", ret);
        return 1;
    }

    ret = io_uring_wait_cqes(ring.ptr, cqe.ptr, 1, 0, 0);
    if (ret) {
        fprintf(stderr, "wait_cqe failed: %d\n", ret);
        return 1;
    }

    if ( cqe.pointed.res  != FILE_SIZE) {
        fprintf(stderr, "Read failed: %d\n", cqe.pointed.res );
        return 1;
    }

    io_uring_cqe_seen(ring.ptr, cqe);
    return 0;
}
