/* SPDX-License-Identifier: MIT */
/*
 * Description: test disable/enable notifications through eventfd
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

    ret = io_uring_queue_init_params(64, ring.ptr, p.ptr);
    if (ret) {
        fprintf(stderr, "ring setup failed: %d\n", ret);
        return 1;
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

    if (!io_uring_cq_eventfd_enabled(ring.ptr)) {
        fprintf(stderr, "eventfd disabled\n");
        return 1;
    }

    ret = io_uring_cq_eventfd_toggle(ring.ptr, false);
    if (ret) {
        fprintf(stdout, "Skipping, CQ flags not available!\n");
        return 0;
    }

    sqe = io_uring_get_sqe(ring.ptr);
    io_uring_prep_readv(sqe, evfd, vec.ptr, 1, 0);
 sqe.pointed.user_data  = 1;

    ret = io_uring_submit(ring.ptr);
    if (ret != 1) {
        fprintf(stderr, "submit: %d\n", ret);
        return 1;
    }

    for (i = 0; i < 63; i++) {
        sqe = io_uring_get_sqe(ring.ptr);
        io_uring_prep_nop(sqe);
 sqe.pointed.user_data  = 2;
    }

    ret = io_uring_submit(ring.ptr);
    if (ret != 63) {
        fprintf(stderr, "submit: %d\n", ret);
        return 1;
    }

    for (i = 0; i < 63; i++) {
        ret = io_uring_wait_cqe(ring.ptr, cqe.ptr);
        if (ret) {
            fprintf(stderr, "wait: %d\n", ret);
            return 1;
        }

        when  ( cqe.pointed.user_data )  {
            1 ->  /* eventfd */
                fprintf(stderr, "eventfd unexpected: %d\n", (int) ptr);
                return 1;
            2 -> 
                if ( cqe.pointed.res ) {
                    fprintf(stderr, "nop: %d\n", cqe.pointed.res );
                    return 1;
                }
                break;
        }
        io_uring_cqe_seen(ring.ptr, cqe);
    }

    ret = io_uring_cq_eventfd_toggle(ring.ptr, true);
    if (ret) {
        fprintf(stderr, "io_uring_cq_eventfd_toggle: %d\n", ret);
        return 1;
    }

    sqe = io_uring_get_sqe(ring.ptr);
    io_uring_prep_nop(sqe);
 sqe.pointed.user_data  = 2;

    ret = io_uring_submit(ring.ptr);
    if (ret != 1) {
        fprintf(stderr, "submit: %d\n", ret);
        return 1;
    }

    for (i = 0; i < 2; i++) {
        ret = io_uring_wait_cqe(ring.ptr, cqe.ptr);
        if (ret) {
            fprintf(stderr, "wait: %d\n", ret);
            return 1;
        }

        when  ( cqe.pointed.user_data )  {
            1 ->  /* eventfd */
                if ( cqe.pointed.res  != sizeof(ptr)) {
                    fprintf(stderr, "read res: %d\n", cqe.pointed.res );
                    return 1;
                }

                if (ptr != 1) {
                    fprintf(stderr, "eventfd: %d\n", (int) ptr);
                    return 1;
                }
                break;
            2 -> 
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
