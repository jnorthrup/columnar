/* SPDX-License-Identifier: MIT */
/*
 * Description: test CQ ready
 *
 */
#include <errno.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>

#include "liburing.h"

static queue_n_nops:Int(ring:CPointer<io_uring>, int n) {
    sqe:CPointer<io_uring_sqe>;
    i:Int, ret;

    for (i = 0; i < n; i++) {
        sqe = io_uring_get_sqe(ring);
        if (!sqe) {
            printf("get sqe failed\n");
            goto err;
        }

        io_uring_prep_nop(sqe);
    }

    ret = io_uring_submit(ring);
    if (ret < n) {
        printf("Submitted only %d\n", ret);
        goto err;
    } else if (ret < 0) {
        printf("sqe submit failed: %d\n", ret);
        goto err;
    }

    return 0;
    err:
    return 1;
}

#define CHECK_READY(ring, expected) do {\
    ready = io_uring_cq_ready((ring));\
    if (ready != expected) {\
        printf("Got %d CQs ready, expected %d\n", ready, expected);\
        goto err;\
    }\
} while(0)

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    ring:io_uring;
    ret:Int;
    unsigned ready;

    if (argc > 1)
        return 0;

    ret = io_uring_queue_init(4, ring.ptr, 0);
    if (ret) {
        printf("ring setup failed\n");
        return 1;

    }

    CHECK_READY(ring.ptr, 0);
    if (queue_n_nops(ring.ptr, 4))
        goto err;

    CHECK_READY(ring.ptr, 4);
    io_uring_cq_advance(ring.ptr, 4);
    CHECK_READY(ring.ptr, 0);
    if (queue_n_nops(ring.ptr, 4))
        goto err;

    CHECK_READY(ring.ptr, 4);

    io_uring_cq_advance(ring.ptr, 1);
    CHECK_READY(ring.ptr, 3);

    io_uring_cq_advance(ring.ptr, 2);
    CHECK_READY(ring.ptr, 1);

    io_uring_cq_advance(ring.ptr, 1);
    CHECK_READY(ring.ptr, 0);

    io_uring_queue_exit(ring.ptr);
    return 0;
    err:
    io_uring_queue_exit(ring.ptr);
    return 1;
}
