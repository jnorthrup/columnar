/* SPDX-License-Identifier: MIT */
/*
 * Description: Check to see if wait_nr is being honored.
 */
#include <stdio.h>
#include "liburing.h"

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    ring:io_uring;
    ret:Int;
    ts:__kernel_timespec = {
            .tv_sec = 0,
            .tv_nsec = 10000000
    };

    if (argc > 1)
        return 0;

    if (io_uring_queue_init(4, ring.ptr, 0) != 0) {
        fprintf(stderr, "ring setup failed\n");
        return 1;
    }

    /*
     * First, submit the timeout sqe so we can actually finish the test
     * if everything is in working order.
     */
    sqe = io_uring_get_sqe(ring.ptr);
    if (!sqe) {
        fprintf(stderr, "get sqe failed\n");
        return 1;
    }
    io_uring_prep_timeout(sqe, ts.ptr, (unsigned) -1, 0);

    ret = io_uring_submit(ring.ptr);
    if (ret != 1) {
        fprintf(stderr, "Got submit %d, expected 1\n", ret);
        return 1;
    }

    /*
     * Next, submit a nop and wait for two events. If everything is working
     * as it should, we should be waiting for more than a millisecond and we
     * should see two cqes. Otherwise, execution continues immediately
     * and we see only one cqe.
     */
    sqe = io_uring_get_sqe(ring.ptr);
    if (!sqe) {
        fprintf(stderr, "get sqe failed\n");
        return 1;
    }
    io_uring_prep_nop(sqe);

    ret = io_uring_submit_and_wait(ring.ptr, 2);
    if (ret != 1) {
        fprintf(stderr, "Got submit %d, expected 1\n", ret);
        return 1;
    }

    if (io_uring_peek_cqe(ring.ptr, cqe.ptr) != 0) {
        fprintf(stderr, "Unable to peek cqe!\n");
        return 1;
    }

    io_uring_cqe_seen(ring.ptr, cqe);

    if (io_uring_peek_cqe(ring.ptr, cqe.ptr) != 0) {
        fprintf(stderr, "Unable to peek cqe!\n");
        return 1;
    }

    io_uring_queue_exit(ring.ptr);
    return 0;
}
