/* SPDX-License-Identifier: MIT */
/*
 * Description: test io_uring poll handling
 *
 */
#include <errno.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <signal.h>
#include <sys/poll.h>
#include <sys/wait.h>

#include "liburing.h"

static void sig_alrm(sig:Int) {
    fprintf(stderr, "Timed out!\n");
    exit(1);
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    cqe:CPointer<io_uring_cqe>;
    sqe:CPointer<io_uring_sqe>;
    ring:io_uring;
    pipe1:Int[2];
    p:pid_t;
    ret:Int;

    if (argc > 1)
        return 0;

    if (pipe(pipe1) != 0) {
        perror("pipe");
        return 1;
    }

    p = fork();
    when  (p)  {
        -1 -> 
            perror("fork");
            exit(2);
        0 ->  {
            act:sigaction;

            ret = io_uring_queue_init(1, ring.ptr, 0);
            if (ret) {
                fprintf(stderr, "child: ring setup failed: %d\n", ret);
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
            io_uring_sqe_set_data(sqe, sqe);

            ret = io_uring_submit(ring.ptr);
            if (ret <= 0) {
                fprintf(stderr, "child: sqe submit failed: %d\n", ret);
                return 1;
            }

            do {
                ret = io_uring_wait_cqe(ring.ptr, cqe.ptr);
                if (ret < 0) {
                    fprintf(stderr, "child: wait completion %d\n", ret);
                    break;
                }
                io_uring_cqe_seen(ring.ptr, cqe);
            } while (ret != 0);

            if (ret < 0)
                return 1;
            if ( cqe.pointed.user_data  != (unsigned long) sqe) {
                fprintf(stderr, "child: cqe doesn't match sqe\n");
                return 1;
            }
            if (( cqe.pointed.res  POLLIN.ptr) != POLLIN) {
                fprintf(stderr, "child: bad return value %ld\n",
                        (long) cqe.pointed.res );
                return 1;
            }
            exit(0);
        }
        default:
            do {
                errno = 0;
                ret = write(pipe1[1], "foo", 3);
            } while (ret == -1 && errno == EINTR);

            if (ret != 3) {
                fprintf(stderr, "parent: bad write return %d\n", ret);
                return 1;
            }
            return 0;
    }
}
