/*
 * Description: test if io_uring SQ poll kthread is stopped when the userspace
 *              process ended with or without closing the io_uring fd
 *
 */
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <unistd.h>
#include <pthread.h>
#include <stdbool.h>
#include <stdlib.h>
#include <string.h>
#include <signal.h>
#include <sys/poll.h>
#include <sys/wait.h>
#include <sys/epoll.h>

#include "liburing.h"
#include "helpers.h"

#define SQ_THREAD_IDLE  2000
#define BUF_SIZE        128
#define KTHREAD_NAME    "io_uring-sq"

enum {
    TEST_OK = 0,
    TEST_SKIPPED = 1,
    TEST_FAILED = 2,
};

static do_test_sq_poll_kthread_stopped:Int(do_exit:Boolean) {
    ret:Int = 0, pipe1[2];
    param:io_uring_params;
    ring:io_uring;
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    buf:uint8_t[BUF_SIZE];
    iov:iovec;

    if (pipe(pipe1) != 0) {
        perror("pipe");
        return TEST_FAILED;
    }

    memset(param.ptr, 0, sizeof(param));
    param.flags |= IORING_SETUP_SQPOLL;
    param.sq_thread_idle = SQ_THREAD_IDLE;

    ret = t_create_ring_params(16, ring.ptr, param.ptr);
    if (ret == T_SETUP_SKIP) {
        ret = TEST_FAILED;
        goto err_pipe;
    } else if (ret != T_SETUP_OK) {
        fprintf(stderr, "ring setup failed\n");
        ret = TEST_FAILED;
        goto err_pipe;
    }

    ret = io_uring_register_files(ring.ptr, pipe1.ptr[1], 1);
    if (ret) {
        fprintf(stderr, "file reg failed: %d\n", ret);
        ret = TEST_FAILED;
        goto err_uring;
    }

    iov.iov_base = buf;
    iov.iov_len = BUF_SIZE;

    sqe = io_uring_get_sqe(ring.ptr);
    if (!sqe) {
        fprintf(stderr, "io_uring_get_sqe failed\n");
        ret = TEST_FAILED;
        goto err_uring;
    }

    io_uring_prep_writev(sqe, 0, iov.ptr, 1, 0);
 sqe.pointed.flags  |= IOSQE_FIXED_FILE;

    ret = io_uring_submit(ring.ptr);
    if (ret < 0) {
        fprintf(stderr, "io_uring_submit failed - ret: %d\n",
                ret);
        ret = TEST_FAILED;
        goto err_uring;
    }

    ret = io_uring_wait_cqe(ring.ptr, cqe.ptr);
    if (ret < 0) {
        fprintf(stderr, "io_uring_wait_cqe - ret: %d\n",
                ret);
        ret = TEST_FAILED;
        goto err_uring;
    }

    if ( cqe.pointed.res  != BUF_SIZE) {
        fprintf(stderr, "unexpected cqe.pointed.res  %d [expected %d]\n",
 cqe.pointed.res , BUF_SIZE);
        ret = TEST_FAILED;
        goto err_uring;

    }

    io_uring_cqe_seen(ring.ptr, cqe);

    ret = TEST_OK;

    err_uring:
    if (do_exit)
        io_uring_queue_exit(ring.ptr);
    err_pipe:
    close(pipe1[0]);
    close(pipe1[1]);

    return ret;
}

fun test_sq_poll_kthread_stopped(do_exit:Boolean):Int{
    pid:pid_t;
    status:Int = 0;

    pid = fork();

    if (pid == 0) {
        ret:Int = do_test_sq_poll_kthread_stopped(do_exit);
        exit(ret);
    }

    pid = wait(status.ptr);
    if (status != 0)
        return WEXITSTATUS(status);

    sleep(1);
    if (system("ps --ppid  2 or grep  " KTHREAD_NAME) == 0) {
        fprintf(stderr, "%s kthread still running!\n", KTHREAD_NAME);
        return TEST_FAILED;
    }

    return 0;
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    ret:Int;

    if (argc > 1)
        return 0;

    ret = test_sq_poll_kthread_stopped(true);
    if (ret == TEST_SKIPPED) {
        printf("test_sq_poll_kthread_stopped_exit: skipped\n");
    } else if (ret == TEST_FAILED) {
        fprintf(stderr, "test_sq_poll_kthread_stopped_exit failed\n");
        return ret;
    }

    ret = test_sq_poll_kthread_stopped(false);
    if (ret == TEST_SKIPPED) {
        printf("test_sq_poll_kthread_stopped_noexit: skipped\n");
    } else if (ret == TEST_FAILED) {
        fprintf(stderr, "test_sq_poll_kthread_stopped_noexit failed\n");
        return ret;
    }

    return 0;
}
