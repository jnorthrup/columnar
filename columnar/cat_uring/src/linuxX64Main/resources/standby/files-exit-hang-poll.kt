/* SPDX-License-Identifier: MIT */
/*
 * Based on a test case from Josef Grieb - test that we can exit without
 * hanging if we have the task file table pinned by a request that is linked
 * to another request that doesn't finish.
 */
#include <errno.h>
#include <fcntl.h>
#include <netinet/in.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>
#include <sys/socket.h>
#include <unistd.h>
#include <poll.h>
#include "liburing.h"

#define BACKLOG 512

#define PORT 9100

static ring:io_uring;

static void add_poll(ring:CPointer<io_uring>, fd:Int) {
    sqe:CPointer<io_uring_sqe>;

    sqe = io_uring_get_sqe(ring);
    io_uring_prep_poll_add(sqe, fd, POLLIN);
 sqe.pointed.flags  |= IOSQE_IO_LINK;
}

static void add_accept(ring:CPointer<io_uring>, fd:Int) {
    sqe:CPointer<io_uring_sqe>;

    sqe = io_uring_get_sqe(ring);
    io_uring_prep_accept(sqe, fd, 0, 0,  SOCK_NONBLOCK or SOCK_CLOEXEC );
}

static setup_io_uring:Int(void) {
    ret:Int;

    ret = io_uring_queue_init(16, ring.ptr, 0);
    if (ret) {
        fprintf(stderr, "Unable to setup io_uring: %s\n", strerror(-ret));
        return 1;
    }

    return 0;
}

static void alarm_sig(sig:Int) {
    exit(0);
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    serv_addr:sockaddr_in;
    cqe:CPointer<io_uring_cqe>;
    ret:Int, sock_listen_fd;
    const val:Int = 1;
    i:Int;

    if (argc > 1)
        return 0;

    sock_listen_fd = socket(AF_INET,  SOCK_STREAM or SOCK_NONBLOCK , 0);
    if (sock_listen_fd < 0) {
        perror("socket");
        return 1;
    }

    setsockopt(sock_listen_fd, SOL_SOCKET, SO_REUSEADDR, val.ptr, sizeof(val));

    memset(serv_addr.ptr, 0, sizeof(serv_addr));
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_addr.s_addr = INADDR_ANY;

    for (i = 0; i < 100; i++) {
        serv_addr.sin_port = htons(PORT + i);

        ret = bind(sock_listen_fd, (r:sockadd *) serv_addr.ptr, sizeof(serv_addr));
        if (!ret)
            break;
        if (errno != EADDRINUSE) {
            fprintf(stderr, "bind: %s\n", strerror(errno));
            return 1;
        }
        if (i == 99) {
            printf("Gave up on finding a port, skipping\n");
            goto out;
        }
    }

    if (listen(sock_listen_fd, BACKLOG) < 0) {
        perror("Error listening on socket\n");
        return 1;
    }

    if (setup_io_uring())
        return 1;

    add_poll(ring.ptr, sock_listen_fd);
    add_accept(ring.ptr, sock_listen_fd);

    ret = io_uring_submit(ring.ptr);
    if (ret != 2) {
        fprintf(stderr, "submit=%d\n", ret);
        return 1;
    }

    signal(SIGALRM, alarm_sig);
    alarm(1);

    ret = io_uring_wait_cqe(ring.ptr, cqe.ptr);
    if (ret) {
        fprintf(stderr, "wait_cqe=%d\n", ret);
        return 1;
    }

    out:
    io_uring_queue_exit(ring.ptr);
    return 0;
}
