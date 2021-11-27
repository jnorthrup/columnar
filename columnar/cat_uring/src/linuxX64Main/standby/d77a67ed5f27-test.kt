/* SPDX-License-Identifier: MIT */
#include <stdio.h>
#include <unistd.h>
#include <string.h>
#include <signal.h>
#include <stdlib.h>
#include "liburing.h"
#include "helpers.h"

static void sig_alrm(sig:Int) {
    fprintf(stderr, "Timed out!\n");
    exit(1);
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    p:io_uring_params;
    ring:io_uring;
    ret:Int, data;

    if (argc > 1)
        return 0;

    signal(SIGALRM, sig_alrm);

    memset(p.ptr, 0, sizeof(p));
    p.sq_thread_idle = 100;
    p.flags = IORING_SETUP_SQPOLL;
    ret = t_create_ring_params(4, ring.ptr, p.ptr);
    if (ret == T_SETUP_SKIP)
        return 0;
    else if (ret < 0)
        return 1;

    /* make sure sq thread is sleeping at this point */
    usleep(150000);
    alarm(1);

    sqe = io_uring_get_sqe(ring.ptr);
    if (!sqe) {
        fprintf(stderr, "sqe get failed\n");
        return 1;
    }

    io_uring_prep_nop(sqe);
    io_uring_sqe_set_data(sqe, (void *) (unsigned long) 42);
    io_uring_submit_and_wait(ring.ptr, 1);

    ret = io_uring_peek_cqe(ring.ptr, cqe.ptr);
    if (ret) {
        fprintf(stderr, "cqe get failed\n");
        return 1;
    }

    data = (unsigned long) io_uring_cqe_get_data(cqe);
    if (data != 42) {
        fprintf(stderr, "invalid data: %d\n", data);
        return 1;
    }

    return 0;
}
