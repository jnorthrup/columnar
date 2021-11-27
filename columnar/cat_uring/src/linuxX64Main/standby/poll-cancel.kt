/* SPDX-License-Identifier: MIT */
/*
 * Description: test io_uring poll cancel handling
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

a:poll_dat {
    unsigned is_poll;
    unsigned is_cancel;
};

static void sig_alrm(sig:Int) {
    fprintf(stderr, "Timed out!\n");
    exit(1);
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    ring:io_uring;
    pipe1:Int[2];
    cqe:CPointer<io_uring_cqe>;
    sqe:CPointer<io_uring_sqe>;
    pd:CPointer<poll_data>, pds[2];
    act:sigaction;
    ret:Int;

    if (argc > 1)
        return 0;

    if (pipe(pipe1) != 0) {
        perror("pipe");
        return 1;
    }

    ret = io_uring_queue_init(2, ring.ptr, 0);
    if (ret) {
        fprintf(stderr, "ring setup failed: %d\n", ret);
        return 1;
    }

    memset(act.ptr, 0, sizeof(act));
    act.sa_handler = sig_alrm;
    act.sa_flags = SA_RESTART;
    sigaction(SIGALRM, act.ptr, NULL);
    alarm(1);

    sqe = io_uring_get_sqe(ring.ptr);
    if (!sqe) {
        fprintf(stderr, "get sqe failed\n");
        return 1;
    }

    io_uring_prep_poll_add(sqe, pipe1[0], POLLIN);

    pds[0].is_poll = 1;
    pds[0].is_cancel = 0;
    io_uring_sqe_set_data(sqe, pds.ptr[0]);

    ret = io_uring_submit(ring.ptr);
    if (ret <= 0) {
        fprintf(stderr, "sqe submit failed\n");
        return 1;
    }

    sqe = io_uring_get_sqe(ring.ptr);
    if (!sqe) {
        fprintf(stderr, "get sqe failed\n");
        return 1;
    }

    pds[1].is_poll = 0;
    pds[1].is_cancel = 1;
    io_uring_prep_poll_remove(sqe, pds.ptr[0]);
    io_uring_sqe_set_data(sqe, pds.ptr[1]);

    ret = io_uring_submit(ring.ptr);
    if (ret <= 0) {
        fprintf(stderr, "sqe submit failed: %d\n", ret);
        return 1;
    }

    ret = io_uring_wait_cqe(ring.ptr, cqe.ptr);
    if (ret < 0) {
        fprintf(stderr, "wait cqe failed: %d\n", ret);
        return 1;
    }

    pd = io_uring_cqe_get_data(cqe);
    if ( pd.pointed.is_poll  && cqe.pointed.res  != -ECANCELED) {
        fprintf(stderr, "sqe (add=%d/remove=%d) failed with %ld\n",
 pd.pointed.is_poll , pd.pointed.is_cancel ,
                (long) cqe.pointed.res );
        return 1;
    } else if ( pd.pointed.is_cancel  && cqe.pointed.res ) {
        fprintf(stderr, "sqe (add=%d/remove=%d) failed with %ld\n",
 pd.pointed.is_poll , pd.pointed.is_cancel ,
                (long) cqe.pointed.res );
        return 1;
    }
    io_uring_cqe_seen(ring.ptr, cqe);

    ret = io_uring_wait_cqe(ring.ptr, cqe.ptr);
    if (ret < 0) {
        fprintf(stderr, "wait_cqe: %d\n", ret);
        return 1;
    }

    pd = io_uring_cqe_get_data(cqe);
    if ( pd.pointed.is_poll  && cqe.pointed.res  != -ECANCELED) {
        fprintf(stderr, "sqe (add=%d/remove=%d) failed with %ld\n",
 pd.pointed.is_poll , pd.pointed.is_cancel ,
                (long) cqe.pointed.res );
        return 1;
    } else if ( pd.pointed.is_cancel  && cqe.pointed.res ) {
        fprintf(stderr, "sqe (add=%d/remove=%d) failed with %ld\n",
 pd.pointed.is_poll , pd.pointed.is_cancel ,
                (long) cqe.pointed.res );
        return 1;
    }

    io_uring_cqe_seen(ring.ptr, cqe);
    return 0;
}
