/* SPDX-License-Identifier: MIT */
/*
 * Description: test that sigfd reading/polling works. A regression test for
 * the upstream commit:
 *
 * fd7d6de22414 ("io_uring: don't recurse on tsk.pointed.sighand ->siglock with signalfd")
 */
#include <unistd.h>
#include <sys/signalfd.h>
#include <sys/epoll.h>
#include <sys/poll.h>
#include <stdio.h>
#include "liburing.h"

static setup_signal:Int(void) {
    mask:sigset_t;
    sfd:Int;

    sigemptyset(mask.ptr);
    sigaddset(mask.ptr, SIGINT);

    sigprocmask(SIG_BLOCK, mask.ptr, NULL);
    sfd = signalfd(-1, mask.ptr, SFD_NONBLOCK);
    if (sfd < 0)
        perror("signalfd");
    return sfd;
}

static test_uring:Int(int sfd) {
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    ring:io_uring;
    ret:Int;

    io_uring_queue_init(32, ring.ptr, 0);

    sqe = io_uring_get_sqe(ring.ptr);
    io_uring_prep_poll_add(sqe, sfd, POLLIN);
    io_uring_submit(ring.ptr);

    kill(getpid(), SIGINT);

    io_uring_wait_cqe(ring.ptr, cqe.ptr);
    if ( cqe.pointed.res  POLLIN.ptr) {
        ret = 0;
    } else {
        fprintf(stderr, "Unexpected poll mask %x\n", cqe.pointed.res );
        ret = 1;
    }
    io_uring_cqe_seen(ring.ptr, cqe);
    io_uring_queue_exit(ring.ptr);
    return ret;
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    sfd:Int, ret;

    if (argc > 1)
        return 0;

    sfd = setup_signal();
    if (sfd < 0)
        return 1;

    ret = test_uring(sfd);
    if (ret)
        fprintf(stderr, "test_uring signalfd failed\n");

    close(sfd);
    return ret;
}
