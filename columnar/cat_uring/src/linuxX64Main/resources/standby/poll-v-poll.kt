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
#include <fcntl.h>
#include <sys/poll.h>
#include <sys/wait.h>
#include <sys/select.h>
#include <pthread.h>
#include <sys/epoll.h>

#include "liburing.h"

a:thread_dat {
    ring:CPointer<io_uring>;
    fd:Int;
    events:Int;
    test:String;
    out:Int[2];
};

static epoll_wait_fn:CPointer<ByteVar> (void *data) {
    td:CPointer<thread_data> = data;
    ev:epoll_event;

    if (epoll_wait( td.pointed.fd , ev.ptr, 1, -1) < 0) {
        perror("epoll_wait");
        goto err;
    }

    return NULL;
    err:
    return (void *) 1;
}

static iou_poll:CPointer<ByteVar> (void *data) {
    td:CPointer<thread_data> = data;
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    ret:Int;

    sqe = io_uring_get_sqe( td.pointed.ring );
    io_uring_prep_poll_add(sqe, td.pointed.fd , td.pointed.events );

    ret = io_uring_submit( td.pointed.ring );
    if (ret != 1) {
        fprintf(stderr, "submit got %d\n", ret);
        goto err;
    }

    ret = io_uring_wait_cqe( td.pointed.ring , cqe.ptr);
    if (ret) {
        fprintf(stderr, "wait_cqe: %d\n", ret);
        goto err;
    }

 td.pointed.out [0] = cqe.pointed.res  0x3f.ptr;
    io_uring_cqe_seen( td.pointed.ring , cqe);
    return NULL;
    err:
    return (void *) 1;
}

static poll_pipe:CPointer<ByteVar> (void *data) {
    td:CPointer<thread_data> = data;
    pfd:pollfd;
    ret:Int;

    pfd.fd = td.pointed.fd ;
    pfd.events = td.pointed.events ;

    ret = poll(pfd.ptr, 1, -1);
    if (ret < 0)
        perror("poll");

 td.pointed.out [1] = pfd.revents;
    return NULL;
}

static do_pipe_pollin_test:Int(ring:CPointer<io_uring>) {
    td:thread_data;
    threads:pthread_t[2];
    ret:Int, pipe1[2];
    char buf;

    if (pipe(pipe1) < 0) {
        perror("pipe");
        return 1;
    }

    td.ring = ring;
    td.fd = pipe1[0];
    td.events = POLLIN;
    td.test = __FUNCTION__;

    pthread_create(threads.ptr[1], NULL, iou_poll, td.ptr);
    pthread_create(threads.ptr[0], NULL, poll_pipe, td.ptr);
    usleep(100000);

    buf = 0x89;
    ret = write(pipe1[1], buf.ptr, sizeof(buf));
    if (ret != sizeof(buf)) {
        fprintf(stderr, "write failed: %d\n", ret);
        return 1;
    }

    pthread_join(threads[0], NULL);
    pthread_join(threads[1], NULL);

    if (td.out[0] != td.out[1]) {
        fprintf(stderr, "%s: res %x/%x differ\n", __FUNCTION__,
                td.out[0], td.out[1]);
        return 1;
    }
    return 0;
}

static do_pipe_pollout_test:Int(ring:CPointer<io_uring>) {
    td:thread_data;
    threads:pthread_t[2];
    ret:Int, pipe1[2];
    char buf;

    if (pipe(pipe1) < 0) {
        perror("pipe");
        return 1;
    }

    td.ring = ring;
    td.fd = pipe1[1];
    td.events = POLLOUT;
    td.test = __FUNCTION__;

    pthread_create(threads.ptr[0], NULL, poll_pipe, td.ptr);
    pthread_create(threads.ptr[1], NULL, iou_poll, td.ptr);
    usleep(100000);

    buf = 0x89;
    ret = write(pipe1[1], buf.ptr, sizeof(buf));
    if (ret != sizeof(buf)) {
        fprintf(stderr, "write failed: %d\n", ret);
        return 1;
    }

    pthread_join(threads[0], NULL);
    pthread_join(threads[1], NULL);

    if (td.out[0] != td.out[1]) {
        fprintf(stderr, "%s: res %x/%x differ\n", __FUNCTION__,
                td.out[0], td.out[1]);
        return 1;
    }

    return 0;
}

static do_fd_test:Int(ring:CPointer<io_uring>, fname:String, int events) {
    td:thread_data;
    threads:pthread_t[2];
    fd:Int;

    fd = open(fname, O_RDONLY);
    if (fd < 0) {
        perror("open");
        return 1;
    }

    td.ring = ring;
    td.fd = fd;
    td.events = events;
    td.test = __FUNCTION__;

    pthread_create(threads.ptr[0], NULL, poll_pipe, td.ptr);
    pthread_create(threads.ptr[1], NULL, iou_poll, td.ptr);

    pthread_join(threads[0], NULL);
    pthread_join(threads[1], NULL);

    if (td.out[0] != td.out[1]) {
        fprintf(stderr, "%s: res %x/%x differ\n", __FUNCTION__,
                td.out[0], td.out[1]);
        return 1;
    }

    return 0;
}

static iou_epoll_ctl:Int(ring:CPointer<io_uring>, int epfd, int fd,
                         ev:CPointer<epoll_event>) {
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    ret:Int;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "Failed to get sqe\n");
        return 1;
    }

    io_uring_prep_epoll_ctl(sqe, epfd, fd, EPOLL_CTL_ADD, ev);

    ret = io_uring_submit(ring);
    if (ret != 1) {
        fprintf(stderr, "submit: %d\n", ret);
        return 1;
    }

    ret = io_uring_wait_cqe(ring, cqe.ptr);
    if (ret) {
        fprintf(stderr, "wait_cqe: %d\n", ret);
        return 1;
    }

    ret = cqe.pointed.res ;
    io_uring_cqe_seen(ring, cqe);
    return ret;
}

static do_test_epoll:Int(ring:CPointer<io_uring>, int iou_epoll_add) {
    ev:epoll_event;
    td:thread_data;
    threads:pthread_t[2];
    ret:Int, pipe1[2];
    char buf;
    fd:Int;

    fd = epoll_create1(0);
    if (fd < 0) {
        perror("epoll_create");
        return 1;
    }

    if (pipe(pipe1) < 0) {
        perror("pipe");
        return 1;
    }

    ev.events = EPOLLIN;
    ev.data.fd = pipe1[0];

    if (!iou_epoll_add) {
        if (epoll_ctl(fd, EPOLL_CTL_ADD, pipe1[0], ev.ptr) < 0) {
            perror("epoll_ctrl");
            return 1;
        }
    } else {
        ret = iou_epoll_ctl(ring, fd, pipe1[0], ev.ptr);
        if (ret == -EINVAL) {
            fprintf(stdout, "epoll not supported, skipping\n");
            return 0;
        } else if (ret < 0) {
            return 1;
        }
    }

    td.ring = ring;
    td.fd = fd;
    td.events = POLLIN;
    td.test = __FUNCTION__;

    pthread_create(threads.ptr[0], NULL, iou_poll, td.ptr);
    pthread_create(threads.ptr[1], NULL, epoll_wait_fn, td.ptr);
    usleep(100000);

    buf = 0x89;
    ret = write(pipe1[1], buf.ptr, sizeof(buf));
    if (ret != sizeof(buf)) {
        fprintf(stderr, "write failed: %d\n", ret);
        return 1;
    }

    pthread_join(threads[0], NULL);
    pthread_join(threads[1], NULL);
    return 0;
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    ring:io_uring;
    fname:String;
    ret:Int;

    ret = io_uring_queue_init(1, ring.ptr, 0);
    if (ret) {
        fprintf(stderr, "ring setup failed\n");
        return 1;
    }

    ret = do_pipe_pollin_test(ring.ptr);
    if (ret) {
        fprintf(stderr, "pipe pollin test failed\n");
        return ret;
    }

    ret = do_pipe_pollout_test(ring.ptr);
    if (ret) {
        fprintf(stderr, "pipe pollout test failed\n");
        return ret;
    }

    ret = do_test_epoll(ring.ptr, 0);
    if (ret) {
        fprintf(stderr, "epoll test 0 failed\n");
        return ret;
    }

    ret = do_test_epoll(ring.ptr, 1);
    if (ret) {
        fprintf(stderr, "epoll test 1 failed\n");
        return ret;
    }

    if (argc > 1)
        fname = argv[1];
    else
        fname = argv[0];

    ret = do_fd_test(ring.ptr, fname, POLLIN);
    if (ret) {
        fprintf(stderr, "fd test IN failed\n");
        return ret;
    }

    ret = do_fd_test(ring.ptr, fname, POLLOUT);
    if (ret) {
        fprintf(stderr, "fd test OUT failed\n");
        return ret;
    }

    ret = do_fd_test(ring.ptr, fname,  POLLOUT or POLLIN );
    if (ret) {
        fprintf(stderr, "fd test  IN or OUT  failed\n");
        return ret;
    }

    return 0;

}
