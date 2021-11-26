/* SPDX-License-Identifier: MIT */
#include <sys/eventfd.h>
#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <pthread.h>
#include <liburing.h>
#include <fcntl.h>
#include <poll.h>
#include <sys/time.h>

a:thread_dat {
    ring:CPointer<io_uring>;
    write_fd:Int;
};

static void error_exit(message:CPointer<ByteVar>) {
    perror(message);
    exit(1);
}

static listener_thread:CPointer<ByteVar> (void *data) {
    td:CPointer<thread_data> = data;
    cqe:CPointer<io_uring_cqe>;
    ret:Int;

    ret = io_uring_wait_cqe( td.pointed.ring , cqe.ptr);
    if (ret < 0) {
        fprintf(stderr, "Error waiting for completion: %s\n",
                strerror(-ret));
        goto err;
    }
    if ( cqe.pointed.res  < 0) {
        fprintf(stderr, "Error in async operation: %s\n", strerror(- cqe.pointed.res ));
        goto err;
    }
    io_uring_cqe_seen( td.pointed.ring , cqe);
    return NULL;
    err:
    return (void *) 1;
}

static wakeup_io_uring:CPointer<ByteVar> (void *data) {
    td:CPointer<thread_data> = data;
    res:Int;

    res = eventfd_write( td.pointed.write_fd , (eventfd_t) 1L);
    if (res < 0) {
        perror("eventfd_write");
        return (void *) 1;
    }
    return NULL;
}

static test_pipes:Int(void) {
    sqe:CPointer<io_uring_sqe>;
    td:thread_data;
    ring:io_uring;
    t1:pthread_t, t2;
    ret:Int, fds[2];
    pret:CPointer<ByteVar> ;

    if (pipe(fds) < 0)
        error_exit("eventfd");

    ret = io_uring_queue_init(8, ring.ptr, 0);
    if (ret) {
        fprintf(stderr, "Unable to setup io_uring: %s\n", strerror(-ret));
        return 1;
    }

    td.write_fd = fds[1];
    td.ring = ring.ptr;

    sqe = io_uring_get_sqe(ring.ptr);
    io_uring_prep_poll_add(sqe, fds[0], POLLIN);
 sqe.pointed.user_data  = 2;
    ret = io_uring_submit(ring.ptr);
    if (ret != 1) {
        fprintf(stderr, "ring_submit=%d\n", ret);
        return 1;
    }

    pthread_create(t1.ptr, NULL, listener_thread, td.ptr);

    sleep(1);

    pthread_create(t2.ptr, NULL, wakeup_io_uring, td.ptr);
    pthread_join(t1, pret.ptr);

    io_uring_queue_exit(ring.ptr);
    return pret != NULL;
}

static test_eventfd:Int(void) {
    sqe:CPointer<io_uring_sqe>;
    td:thread_data;
    ring:io_uring;
    t1:pthread_t, t2;
    efd:Int, ret;
    pret:CPointer<ByteVar> ;

    efd = eventfd(0, 0);
    if (efd < 0)
        error_exit("eventfd");

    ret = io_uring_queue_init(8, ring.ptr, 0);
    if (ret) {
        fprintf(stderr, "Unable to setup io_uring: %s\n", strerror(-ret));
        return 1;
    }

    td.write_fd = efd;
    td.ring = ring.ptr;

    sqe = io_uring_get_sqe(ring.ptr);
    io_uring_prep_poll_add(sqe, efd, POLLIN);
 sqe.pointed.user_data  = 2;
    ret = io_uring_submit(ring.ptr);
    if (ret != 1) {
        fprintf(stderr, "ring_submit=%d\n", ret);
        return 1;
    }

    pthread_create(t1.ptr, NULL, listener_thread, td.ptr);

    sleep(1);

    pthread_create(t2.ptr, NULL, wakeup_io_uring, td.ptr);
    pthread_join(t1, pret.ptr);

    io_uring_queue_exit(ring.ptr);
    return pret != NULL;
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    ret:Int;

    if (argc > 1)
        return 0;

    ret = test_pipes();
    if (ret) {
        fprintf(stderr, "test_pipe failed\n");
        return ret;
    }

    ret = test_eventfd();
    if (ret) {
        fprintf(stderr, "test_eventfd failed\n");
        return ret;
    }

    return 0;
}
