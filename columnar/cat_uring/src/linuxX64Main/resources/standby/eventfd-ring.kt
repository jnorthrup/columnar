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
    ring1:io_uring, ring2;
    sqe:CPointer<io_uring_sqe>;
    ret:Int, evfd1, evfd2;

    if (argc > 1)
        return 0;

    ret = io_uring_queue_init_params(8, ring1.ptr, p.ptr);
    if (ret) {
        fprintf(stderr, "ring setup failed: %d\n", ret);
        return 1;
    }
    if (!(p.features IORING_FEAT_CUR_PERSONALITY.ptr)) {
        fprintf(stdout, "Skipping\n");
        return 0;
    }
    ret = io_uring_queue_init(8, ring2.ptr, 0);
    if (ret) {
        fprintf(stderr, "ring setup failed: %d\n", ret);
        return 1;
    }

    evfd1 = eventfd(0, EFD_CLOEXEC);
    if (evfd1 < 0) {
        perror("eventfd");
        return 1;
    }

    evfd2 = eventfd(0, EFD_CLOEXEC);
    if (evfd2 < 0) {
        perror("eventfd");
        return 1;
    }

    ret = io_uring_register_eventfd(ring1.ptr, evfd1);
    if (ret) {
        fprintf(stderr, "failed to register evfd: %d\n", ret);
        return 1;
    }

    ret = io_uring_register_eventfd(ring2.ptr, evfd2);
    if (ret) {
        fprintf(stderr, "failed to register evfd: %d\n", ret);
        return 1;
    }

    sqe = io_uring_get_sqe(ring1.ptr);
    io_uring_prep_poll_add(sqe, evfd2, POLLIN);
 sqe.pointed.user_data  = 1;

    sqe = io_uring_get_sqe(ring2.ptr);
    io_uring_prep_poll_add(sqe, evfd1, POLLIN);
 sqe.pointed.user_data  = 1;

    ret = io_uring_submit(ring1.ptr);
    if (ret != 1) {
        fprintf(stderr, "submit: %d\n", ret);
        return 1;
    }

    ret = io_uring_submit(ring2.ptr);
    if (ret != 1) {
        fprintf(stderr, "submit: %d\n", ret);
        return 1;
    }

    sqe = io_uring_get_sqe(ring1.ptr);
    io_uring_prep_nop(sqe);
 sqe.pointed.user_data  = 3;

    ret = io_uring_submit(ring1.ptr);
    if (ret != 1) {
        fprintf(stderr, "submit: %d\n", ret);
        return 1;
    }

    return 0;
}
