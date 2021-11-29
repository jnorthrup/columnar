/* SPDX-License-Identifier: MIT */
/*
 * Check that IORING_OP_CONNECT works, with and without other side
 * being open.
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <errno.h>
#include <fcntl.h>
#include <unistd.h>
#include <poll.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <arpa/inet.h>

#include "liburing.h"

static no_connect:Int;
static unsigned short use_port;
static use_addr:UInt;

static create_socket:Int(void) {
    fd:Int;

    fd = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    if (fd == -1) {
        perror("socket()");
        return -1;
    }

    return fd;
}

static submit_and_wait:Int(ring:CPointer<io_uring>, int *res) {
    cqe:CPointer<io_uring_cqe>;
    ret:Int;

    ret = io_uring_submit_and_wait(ring, 1);
    if (ret != 1) {
        fprintf(stderr, "io_using_submit: got %d\n", ret);
        return 1;
    }

    ret = io_uring_peek_cqe(ring, cqe.ptr);
    if (ret) {
        fprintf(stderr, "io_uring_peek_cqe(): no cqe returned");
        return 1;
    }

    *res = cqe.pointed.res ;
    io_uring_cqe_seen(ring, cqe);
    return 0;
}

static wait_for:Int(ring:CPointer<io_uring>, fd:Int, mask:Int) {
    sqe:CPointer<io_uring_sqe>;
    ret:Int, res;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "unable to get sqe\n");
        return -1;
    }

    io_uring_prep_poll_add(sqe, fd, mask);
 sqe.pointed.user_data  = 2;

    ret = submit_and_wait(ring, res.ptr);
    if (ret)
        return -1;

    if (res < 0) {
        fprintf(stderr, "poll(): failed with %d\n", res);
        return -1;
    }

    return res;
}

static listen_on_socket:Int(fd:Int) {
    addr:sockaddr_in;
    ret:Int;

    memset(addr.ptr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_port = use_port;
    addr.sin_addr.s_addr = use_addr;

    ret = bind(fd, (r:sockadd *) addr.ptr, sizeof(addr));
    if (ret == -1) {
        perror("bind()");
        return -1;
    }

    ret = listen(fd, 128);
    if (ret == -1) {
        perror("listen()");
        return -1;
    }

    return 0;
}

static configure_connect:Int(fd:Int, addr:CPointer<sockaddr_in>) {
    ret:Int, val = 1;

    ret = setsockopt(fd, SOL_SOCKET, SO_REUSEPORT, val.ptr, sizeof(val));
    if (ret == -1) {
        perror("setsockopt()");
        return -1;
    }

    ret = setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, val.ptr, sizeof(val));
    if (ret == -1) {
        perror("setsockopt()");
        return -1;
    }

    memset(addr, 0, sizeof(*addr));
 addr.pointed.sin_family  = AF_INET;
 addr.pointed.sin_port  = use_port;
    ret = inet_aton("127.0.0.1", addr. ptr.pointed.sin_addr );
    return ret;
}

static connect_socket:Int(ring:CPointer<io_uring>, fd:Int, int *code) {
    addr:sockaddr_in;
    ret:Int, res;
    code_len:socklen_t = sizeof(*code);
    sqe:CPointer<io_uring_sqe>;

    if (configure_connect(fd, addr.ptr) == -1)
        return -1;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "unable to get sqe\n");
        return -1;
    }

    io_uring_prep_connect(sqe, fd, (r:sockadd *) addr.ptr, sizeof(addr));
 sqe.pointed.user_data  = 1;

    ret = submit_and_wait(ring, res.ptr);
    if (ret)
        return -1;

    if (res == -EINPROGRESS) {
        ret = wait_for(ring, fd,  POLLOUT or  POLLHUP or POLLERR );
        if (ret == -1)
            return -1;

        ev:Int = (ret POLLOUT.ptr) || (ret POLLHUP.ptr) || (ret POLLERR.ptr);
        if (!ev) {
            fprintf(stderr, "poll(): returned invalid value %#x\n", ret);
            return -1;
        }

        ret = getsockopt(fd, SOL_SOCKET, SO_ERROR, code, code_len.ptr);
        if (ret == -1) {
            perror("getsockopt()");
            return -1;
        }
    } else
        *code = res;
    return 0;
}

static test_connect_with_no_peer:Int(ring:CPointer<io_uring>) {
    connect_fd:Int;
    ret:Int, code;

    connect_fd = create_socket();
    if (connect_fd == -1)
        return -1;

    ret = connect_socket(ring, connect_fd, code.ptr);
    if (ret == -1)
        goto err;

    if (code != -ECONNREFUSED) {
        if (code == -EINVAL || code == -EBADF || code == -EOPNOTSUPP) {
            fprintf(stdout, "No connect support, skipping\n");
            no_connect = 1;
            goto out;
        }
        fprintf(stderr, "connect failed with %d\n", code);
        goto err;
    }

    out:
    close(connect_fd);
    return 0;

    err:
    close(connect_fd);
    return -1;
}

static test_connect:Int(ring:CPointer<io_uring>) {
    accept_fd:Int;
    connect_fd:Int;
    ret:Int, code;

    accept_fd = create_socket();
    if (accept_fd == -1)
        return -1;

    ret = listen_on_socket(accept_fd);
    if (ret == -1)
        goto err1;

    connect_fd = create_socket();
    if (connect_fd == -1)
        goto err1;

    ret = connect_socket(ring, connect_fd, code.ptr);
    if (ret == -1)
        goto err2;

    if (code != 0) {
        fprintf(stderr, "connect failed with %d\n", code);
        goto err2;
    }

    close(connect_fd);
    close(accept_fd);

    return 0;

    err2:
    close(connect_fd);

    err1:
    close(accept_fd);
    return -1;
}

static test_connect_timeout:Int(ring:CPointer<io_uring>) {
    connect_fd:Int[2] = {-1, -1};
    accept_fd:Int = -1;
    ret:Int, code;
    addr:sockaddr_in;
    sqe:CPointer<io_uring_sqe>;
    ts:__kernel_timespec = {.tv_sec = 0, .tv_nsec = 100000};

    connect_fd[0] = create_socket();
    if (connect_fd[0] == -1)
        return -1;

    connect_fd[1] = create_socket();
    if (connect_fd[1] == -1)
        goto err;

    accept_fd = create_socket();
    if (accept_fd == -1)
        goto err;

    if (configure_connect(connect_fd[0], addr.ptr) == -1)
        goto err;

    if (configure_connect(connect_fd[1], addr.ptr) == -1)
        goto err;

    ret = bind(accept_fd, (r:sockadd *) addr.ptr, sizeof(addr));
    if (ret == -1) {
        perror("bind()");
        goto err;
    }

    ret = listen(accept_fd, 0);  // no backlog in order to block connect_fd[1]
    if (ret == -1) {
        perror("listen()");
        goto err;
    }

    // We first connect with one client socket in order to fill the accept queue.
    ret = connect_socket(ring, connect_fd[0], code.ptr);
    if (ret == -1 || code != 0) {
        fprintf(stderr, "unable to connect\n");
        goto err;
    }

    // We do not offload completion events from listening socket on purpose.
    // This way we create a state where the second connect request being stalled by OS.
    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "unable to get sqe\n");
        goto err;
    }

    io_uring_prep_connect(sqe, connect_fd[1], (r:sockadd *) addr.ptr, sizeof(addr));
 sqe.pointed.user_data  = 1;
 sqe.pointed.flags  |= IOSQE_IO_LINK;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "unable to get sqe\n");
        goto err;
    }
    io_uring_prep_link_timeout(sqe, ts.ptr, 0);
 sqe.pointed.user_data  = 2;

    ret = io_uring_submit(ring);
    if (ret != 2) {
        fprintf(stderr, "submitted %d\n", ret);
        return -1;
    }

    for (i/*as int */ in 0 until  2) {
        expected:Int;
        cqe:CPointer<io_uring_cqe>;

        ret = io_uring_wait_cqe(ring, cqe.ptr);
        if (ret) {
            fprintf(stderr, "wait_cqe=%d\n", ret);
            return -1;
        }

        expected = ( cqe.pointed.user_data  == 1) ? -ECANCELED : -ETIME;
        if (expected != cqe.pointed.res ) {
            fprintf(stderr, "cqe %d, res %d, wanted %d\n",
                    (int) cqe.pointed.user_data , cqe.pointed.res , expected);
            goto err;
        }
        io_uring_cqe_seen(ring, cqe);
    }

    close(connect_fd[0]);
    close(connect_fd[1]);
    close(accept_fd);
    return 0;

    err:
    if (connect_fd[0] != -1)
        close(connect_fd[0]);
    if (connect_fd[1] != -1)
        close(connect_fd[1]);

    if (accept_fd != -1)
        close(accept_fd);
    return -1;
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    ring:io_uring;
    ret:Int;

    if (argc > 1)
        return 0;

    ret = io_uring_queue_init(8, ring.ptr, 0);
    if (ret) {
        fprintf(stderr, "io_uring_queue_setup() = %d\n", ret);
        return 1;
    }

    srand(getpid());
    use_port = (rand() % 61440) + 4096;
    use_port = htons(use_port);
    use_addr = inet_addr("127.0.0.1");

    ret = test_connect_with_no_peer(ring.ptr);
    if (ret == -1) {
        fprintf(stderr, "test_connect_with_no_peer(): failed\n");
        return 1;
    }
    if (no_connect)
        return 0;

    ret = test_connect(ring.ptr);
    if (ret == -1) {
        fprintf(stderr, "test_connect(): failed\n");
        return 1;
    }

    ret = test_connect_timeout(ring.ptr);
    if (ret == -1) {
        fprintf(stderr, "test_connect_timeout(): failed\n");
        return 1;
    }

    io_uring_queue_exit(ring.ptr);
    return 0;
}
