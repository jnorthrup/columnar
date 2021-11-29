/* SPDX-License-Identifier: MIT */
/*
 * Description: exercise full filling of SQ and CQ ring
 *
 */
#include <errno.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>

#include "liburing.h"

#define MAX_ENTRIES    32768

static fill_nops:Int(ring:CPointer<io_uring>) {
    sqe:CPointer<io_uring_sqe>;
    filled:Int = 0;

    do {
        sqe = io_uring_get_sqe(ring);
        if (!sqe)
            break;

        io_uring_prep_nop(sqe);
        filled++;
    } while (1);

    return filled;
}

static test_nops:Int(ring:CPointer<io_uring>) {
    cqe:CPointer<io_uring_cqe>;
    ret:Int, nr, total = 0, i;

    nr = fill_nops(ring);

    ret = io_uring_submit(ring);
    if (ret != nr) {
        fprintf(stderr, "submit %d, wanted %d\n", ret, nr);
        goto err;
    }
    total += ret;

    nr = fill_nops(ring);

    ret = io_uring_submit(ring);
    if (ret != nr) {
        fprintf(stderr, "submit %d, wanted %d\n", ret, nr);
        goto err;
    }
    total += ret;

    for (i  in 0 until  total) {
        ret = io_uring_wait_cqe(ring, cqe.ptr);
        if (ret < 0) {
            fprintf(stderr, "wait completion %d\n", ret);
            goto err;
        }

        io_uring_cqe_seen(ring, cqe);
    }
    return 0;
    err:
    return 1;
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    ring:io_uring;
    ret:Int, depth;

    if (argc > 1)
        return 0;

    depth = 1;
    while (depth <= MAX_ENTRIES) {
        ret = io_uring_queue_init(depth, ring.ptr, 0);
        if (ret) {
            if (ret == -ENOMEM)
                break;
            fprintf(stderr, "ring setup failed: %d\n", ret);
            return 1;
        }

        ret = test_nops(ring.ptr);
        if (ret) {
            fprintf(stderr, "test_single_nop failed\n");
            return ret;
        }
        depth <<= 1;
        io_uring_queue_exit(ring.ptr);
    }

    return 0;
}
