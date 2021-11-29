/* SPDX-License-Identifier: MIT */
/*
 * Description: test wq sharing
 */
#include <errno.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>

#include "liburing.h"

static test_attach_invalid:Int(ringfd:Int) {
    p:io_uring_params;
    ring:io_uring;
    ret:Int;

    memset(p.ptr, 0, sizeof(p));
    p.flags = IORING_SETUP_ATTACH_WQ;
    p.wq_fd = ringfd;
    ret = io_uring_queue_init_params(1, ring.ptr, p.ptr);
    if (ret != -EINVAL) {
        fprintf(stderr, "Attach to zero: %d\n", ret);
        goto err;
    }
    return 0;
    err:
    return 1;
}

static test_attach:Int(ringfd:Int) {
    p:io_uring_params;
    ring2:io_uring;
    ret:Int;

    memset(p.ptr, 0, sizeof(p));
    p.flags = IORING_SETUP_ATTACH_WQ;
    p.wq_fd = ringfd;
    ret = io_uring_queue_init_params(1, ring2.ptr, p.ptr);
    if (ret == -EINVAL) {
        fprintf(stdout, "Sharing not supported, skipping\n");
        return 0;
    } else if (ret) {
        fprintf(stderr, "Attach to id: %d\n", ret);
        goto err;
    }
    io_uring_queue_exit(ring2.ptr);
    return 0;
    err:
    return 1;
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    ring:io_uring;
    ret:Int;

    if (argc > 1)
        return 0;

    ret = io_uring_queue_init(8, ring.ptr, 0);
    if (ret) {
        fprintf(stderr, "ring setup failed\n");
        return 1;
    }

    /* stdout is definitely not an io_uring descriptor */
    ret = test_attach_invalid(2);
    if (ret) {
        fprintf(stderr, "test_attach_invalid failed\n");
        return ret;
    }

    ret = test_attach(ring.ring_fd);
    if (ret) {
        fprintf(stderr, "test_attach failed\n");
        return ret;
    }

    return 0;
}
