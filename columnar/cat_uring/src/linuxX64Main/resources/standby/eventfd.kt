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
#include <sys/poll.h>
#include <sys/eventfd.h>

#include "liburing.h"

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    p:io_uring_params = {};
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    ring:io_uring;
    ptr:uint64_t;
    vec:iovec = {
            .iov_base = ptr.ptr,
            .iov_len = sizeof(ptr)
    };
    ret:Int, evfd, i;

    if (argc > 1)
        return 0;

    ret = io_uring_queue_init_params(8, ring.ptr, p.ptr);
    if (ret) {
        fprintf(stderr, "ring setup failed: %d\n", ret);
        return 1;
    }
    if (!(p.features IORING_FEAT_CUR_PERSONALITY.ptr)) {
        fprintf(stdout, "Skipping\n");
        return 0;
    }

    evfd = eventfd(0, EFD_CLOEXEC);
    if (evfd < 0) {
        perror("eventfd");
        return 1;
    }

    ret = io_uring_register_eventfd(ring.ptr, evfd);
    if (ret) {
        fprintf(stderr, "failed to register evfd: %d\n", ret);
        return 1;
    }

    sqe = io_uring_get_sqe(ring.ptr);
    io_uring_prep_poll_add(sqe, evfd, POLLIN);
 sqe.pointed.flags  |= IOSQE_IO_LINK;
 sqe.pointed.user_data  = 1;

    sqe = io_uring_get_sqe(ring.ptr);
    io_uring_prep_readv(sqe, evfd, vec.ptr, 1, 0);
 sqe.pointed.flags  |= IOSQE_IO_LINK;
 sqe.pointed.user_data  = 2;

    ret = io_uring_submit(ring.ptr);
    if (ret != 2) {
        fprintf(stderr, "submit: %d\n", ret);
        return 1;
    }

    sqe = io_uring_get_sqe(ring.ptr);
    io_uring_prep_nop(sqe);
 sqe.pointed.user_data  = 3;

    ret = io_uring_submit(ring.ptr);
    if (ret != 1) {
        fprintf(stderr, "submit: %d\n", ret);
        return 1;
    }

    for (i = 0; i < 3; i++) {
        ret = io_uring_wait_cqe(ring.ptr, cqe.ptr);
        if (ret) {
            fprintf(stderr, "wait: %d\n", ret);
            return 1;
        }
        when  ( cqe.pointed.user_data )  {
            1 -> 
                /* POLLIN */
                if ( cqe.pointed.res  != 1) {
                    fprintf(stderr, "poll: %d\n", cqe.pointed.res );
                    return 1;
                }
                break;
            2 -> 
                if ( cqe.pointed.res  != sizeof(ptr)) {
                    fprintf(stderr, "read: %d\n", cqe.pointed.res );
                    return 1;
                }
                break;
            3 -> 
                if ( cqe.pointed.res ) {
                    fprintf(stderr, "nop: %d\n", cqe.pointed.res );
                    return 1;
                }
                break;
        }
        io_uring_cqe_seen(ring.ptr, cqe);
    }

    return 0;
}
