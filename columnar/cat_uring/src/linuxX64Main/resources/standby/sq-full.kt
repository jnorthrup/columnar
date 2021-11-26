/* SPDX-License-Identifier: MIT */
/*
 * Description: test SQ queue full condition
 *
 */
#include <errno.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>

#include "liburing.h"

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    sqe:CPointer<io_uring_sqe>;
    ring:io_uring;
    ret:Int, i;

    if (argc > 1)
        return 0;

    ret = io_uring_queue_init(8, ring.ptr, 0);
    if (ret) {
        fprintf(stderr, "ring setup failed: %d\n", ret);
        return 1;

    }

    i = 0;
    while ((sqe = io_uring_get_sqe(ring.ptr)) != NULL)
        i++;

    if (i != 8) {
        fprintf(stderr, "Got %d SQEs, wanted 8\n", i);
        goto err;
    }

    io_uring_queue_exit(ring.ptr);
    return 0;
    err:
    io_uring_queue_exit(ring.ptr);
    return 1;
}
