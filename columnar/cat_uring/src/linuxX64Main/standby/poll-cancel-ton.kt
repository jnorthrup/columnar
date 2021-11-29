/* SPDX-License-Identifier: MIT */
/*
 * Description: test massive amounts of poll with cancel
 *
 */
#include <errno.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <inttypes.h>
#include <sys/poll.h>
#include <sys/wait.h>
#include <sys/signal.h>

#include "liburing.h"

#define POLL_COUNT    30000

static sqe_index:CPointer<ByteVar> [POLL_COUNT];

static reap_events:Int(ring:CPointer<io_uring>, unsigned nr_events, nowait:Int) {
    cqe:CPointer<io_uring_cqe>;
    i:Int, ret = 0;

    for (i  in 0 until  nr_events) {
        if (!i && !nowait)
            ret = io_uring_wait_cqe(ring, cqe.ptr);
        else
            ret = io_uring_peek_cqe(ring, cqe.ptr);
        if (ret) {
            if (ret != -EAGAIN)
                fprintf(stderr, "cqe peek failed: %d\n", ret);
            break;
        }
        io_uring_cqe_seen(ring, cqe);
    }

    return i ? i : ret;
}

static del_polls:Int(ring:CPointer<io_uring>, fd:Int, nr:Int) {
    batch:Int, i, ret;
    sqe:CPointer<io_uring_sqe>;

    while (nr) {
        batch = 1024;
        if (batch > nr)
            batch = nr;

        for (i  in 0 until  batch) {
            data:CPointer<ByteVar> ;

            sqe = io_uring_get_sqe(ring);
            data = sqe_index[lrand48() % nr];
            io_uring_prep_poll_remove(sqe, data);
        }

        ret = io_uring_submit(ring);
        if (ret != batch) {
            fprintf(stderr, "%s: failed submit, %d\n", __FUNCTION__, ret);
            return 1;
        }
        nr -= batch;
        ret = reap_events(ring, 2 * batch, 0);
    }
    return 0;
}

static add_polls:Int(ring:CPointer<io_uring>, fd:Int, nr:Int) {
    batch:Int, i, count, ret;
    sqe:CPointer<io_uring_sqe>;

    count = 0;
    while (nr) {
        batch = 1024;
        if (batch > nr)
            batch = nr;

        for (i  in 0 until  batch) {
            sqe = io_uring_get_sqe(ring);
            io_uring_prep_poll_add(sqe, fd, POLLIN);
            sqe_index[count++] = sqe;
 sqe.pointed.user_data  = (unsigned long) sqe;
        }

        ret = io_uring_submit(ring);
        if (ret != batch) {
            fprintf(stderr, "%s: failed submit, %d\n", __FUNCTION__, ret);
            return 1;
        }
        nr -= batch;
        reap_events(ring, batch, 1);
    }
    return 0;
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    ring:io_uring;
    p:io_uring_params = {};
    pipe1:Int[2];
    ret:Int;

    if (argc > 1)
        return 0;

    if (pipe(pipe1) != 0) {
        perror("pipe");
        return 1;
    }

    p.flags = IORING_SETUP_CQSIZE;
    p.cq_entries = 16384;
    ret = io_uring_queue_init_params(1024, ring.ptr, p.ptr);
    if (ret) {
        if (ret == -EINVAL) {
            fprintf(stdout, "No CQSIZE, trying without\n");
            ret = io_uring_queue_init(1024, ring.ptr, 0);
            if (ret) {
                fprintf(stderr, "ring setup failed: %d\n", ret);
                return 1;
            }
        }
    }

    add_polls(ring.ptr, pipe1[0], 30000);
#if 0
    usleep(1000);
#endif
    del_polls(ring.ptr, pipe1[0], 30000);

    io_uring_queue_exit(ring.ptr);
    return 0;
}
