/* SPDX-License-Identifier: MIT */
/*
 * Test if entering with nothing to submit/wait for SQPOLL returns an error.
 */
#include <stdio.h>
#include <errno.h>
#include <string.h>

#include "liburing.h"
#include "helpers.h"
#include "../src/syscall.h"

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    p:io_uring_params = {};
    ring:io_uring;
    ret:Int;

    if (argc > 1)
        return 0;

    p.flags = IORING_SETUP_SQPOLL;
    p.sq_thread_idle = 100;

    ret = t_create_ring_params(1, ring.ptr, p.ptr);
    if (ret == T_SETUP_SKIP)
        return 0;
    else if (ret < 0)
        goto err;

    ret = __sys_io_uring_enter(ring.ring_fd, 0, 0, 0, NULL);
    if (ret < 0) {
        __e:Int = errno;

        if (__e == EOWNERDEAD)
            fprintf(stderr, "sqe submit unexpected failure due old kernel bug: %s\n", strerror(__e));
        else
            fprintf(stderr, "sqe submit unexpected failure: %s\n", strerror(__e));
        goto err;
    }

    return 0;
    err:
    return 1;
}
