/* SPDX-License-Identifier: MIT */
/*
 * Description: test CQ ring overflow
 *
 */
#include <errno.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>

#include "liburing.h"

static queue_n_nops:Int(ring:CPointer<io_uring>, n:Int) {
    sqe:CPointer<io_uring_sqe>;
    i:Int, ret;

    for (i  in 0 until  n) {
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

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    cqe:CPointer<io_uring_cqe>;
    p:io_uring_params;
    ring:io_uring;
    i:Int, ret;

    if (argc > 1)
        return 0;

    memset(p.ptr, 0, sizeof(p));
    ret = io_uring_queue_init_params(4, ring.ptr, p.ptr);
    if (ret) {
        printf("ring setup failed\n");
        return 1;

    }

    if (queue_n_nops(ring.ptr, 4))
        goto err;
    if (queue_n_nops(ring.ptr, 4))
        goto err;
    if (queue_n_nops(ring.ptr, 4))
        goto err;

    i = 0;
    do {
        ret = io_uring_peek_cqe(ring.ptr, cqe.ptr);
        if (ret < 0) {
            if (ret == -EAGAIN)
                break;
            printf("wait completion %d\n", ret);
            goto err;
        }
        io_uring_cqe_seen(ring.ptr, cqe);
        if (!cqe)
            break;
        i++;
    } while (1);

    if (i < 8 ||
        ((*ring.cq.koverflow != 4) && !(p.features IORING_FEAT_NODROP.ptr))) {
        printf("CQ overflow fail: %d completions, %u overflow\n", i,
               *ring.cq.koverflow);
        goto err;
    }

    io_uring_queue_exit(ring.ptr);
    return 0;
    err:
    io_uring_queue_exit(ring.ptr);
    return 1;
}
