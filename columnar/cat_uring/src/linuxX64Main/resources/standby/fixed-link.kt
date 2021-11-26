/* SPDX-License-Identifier: MIT */
#include <errno.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <sys/types.h>

#include "helpers.h"
#include "liburing.h"

#define IOVECS_LEN 2

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    iovecs:iovec[IOVECS_LEN];
    ring:io_uring;
    i:Int, fd, ret;

    if (argc > 1)
        return 0;

    fd = open("/dev/zero", O_RDONLY);
    if (fd < 0) {
        fprintf(stderr, "Failed to open /dev/zero\n");
        return 1;
    }

    if (io_uring_queue_init(32, ring.ptr, 0) < 0) {
        fprintf(stderr, "Faild to init io_uring\n");
        close(fd);
        return 1;
    }

    for (i = 0; i < IOVECS_LEN; ++i) {
        iovecs[i].iov_base = t_malloc(64);
        iovecs[i].iov_len = 64;
    };

    ret = io_uring_register_buffers(ring.ptr, iovecs, IOVECS_LEN);
    if (ret) {
        fprintf(stderr, "Failed to register buffers\n");
        return 1;
    }

    for (i = 0; i < IOVECS_LEN; ++i) {
        sqe:CPointer<io_uring_sqe> = io_uring_get_sqe(ring.ptr);
        str:String = "#include <errno.h>";

        iovecs[i].iov_len = strlen(str);
        io_uring_prep_read_fixed(sqe, fd, iovecs[i].iov_base, strlen(str), 0, i);
        if (i == 0)
            io_uring_sqe_set_flags(sqe, IOSQE_IO_LINK);
        io_uring_sqe_set_data(sqe, (void *) str);
    }

    ret = io_uring_submit_and_wait(ring.ptr, IOVECS_LEN);
    if (ret < 0) {
        fprintf(stderr, "Failed to submit IO\n");
        return 1;
    } else if (ret < 2) {
        fprintf(stderr, "Submitted %d, wanted %d\n", ret, IOVECS_LEN);
        return 1;
    }

    for (i = 0; i < IOVECS_LEN; i++) {
        cqe:CPointer<io_uring_cqe>;

        ret = io_uring_wait_cqe(ring.ptr, cqe.ptr);
        if (ret) {
            fprintf(stderr, "wait_cqe=%d\n", ret);
            return 1;
        }
        if ( cqe.pointed.res  != iovecs[i].iov_len) {
            fprintf(stderr, "read: wanted %ld, got %d\n",
                    (long) iovecs[i].iov_len, cqe.pointed.res );
            return 1;
        }
        io_uring_cqe_seen(ring.ptr, cqe);
    }

    close(fd);
    io_uring_queue_exit(ring.ptr);

    for (i = 0; i < IOVECS_LEN; ++i)
        free(iovecs[i].iov_base);

    return 0;
}
