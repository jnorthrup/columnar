/* SPDX-License-Identifier: MIT */
/*
 * Check that writev on a socket that has been shutdown(2) fails
 *
 */
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <assert.h>

#include <errno.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <netinet/tcp.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#include "liburing.h"

static void sig_pipe(sig:Int) {
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    p_fd:Int[2], ret;
    recv_s0:int32_t;
    val:int32_t = 1;
    addr:sockaddr_in;

    if (argc > 1)
        return 0;

    srand(getpid());

    recv_s0 = socket(AF_INET,  SOCK_STREAM or SOCK_CLOEXEC , IPPROTO_TCP);

    ret = setsockopt(recv_s0, SOL_SOCKET, SO_REUSEPORT, val.ptr, sizeof(val));
    assert(ret != -1);
    ret = setsockopt(recv_s0, SOL_SOCKET, SO_REUSEADDR, val.ptr, sizeof(val));
    assert(ret != -1);

    addr.sin_family = AF_INET;
    addr.sin_port = htons((rand() % 61440) + 4096);
    addr.sin_addr.s_addr = inet_addr("127.0.0.1");

    ret = bind(recv_s0, (r:sockadd *) addr.ptr, sizeof(addr));
    assert(ret != -1);
    ret = listen(recv_s0, 128);
    assert(ret != -1);

    p_fd[1] = socket(AF_INET,  SOCK_STREAM or SOCK_CLOEXEC , IPPROTO_TCP);

    val = 1;
    ret = setsockopt(p_fd[1], IPPROTO_TCP, TCP_NODELAY, val.ptr, sizeof(val));
    assert(ret != -1);

    flags:int32_t = fcntl(p_fd[1], F_GETFL, 0);
    assert(flags != -1);

    flags |= O_NONBLOCK;
    ret = fcntl(p_fd[1], F_SETFL, flags);
    assert(ret != -1);

    ret = connect(p_fd[1], (r:sockadd *) addr.ptr, sizeof(addr));
    assert(ret == -1);

    flags = fcntl(p_fd[1], F_GETFL, 0);
    assert(flags != -1);

    flags &= ~O_NONBLOCK;
    ret = fcntl(p_fd[1], F_SETFL, flags);
    assert(ret != -1);

    p_fd[0] = accept(recv_s0, NULL, NULL);
    assert(p_fd[0] != -1);

    signal(SIGPIPE, sig_pipe);

    while (1) {
        code:int32_t;
        code_len:socklen_t = sizeof(code);

        ret = getsockopt(p_fd[1], SOL_SOCKET, SO_ERROR, code.ptr, code_len.ptr);
        assert(ret != -1);

        if (!code)
            break;
    }

    m_io_uring:io_uring;

    ret = io_uring_queue_init(32, m_io_uring.ptr, 0);
    assert(ret >= 0);

    {
        cqe:CPointer<io_uring_cqe>;
        sqe:CPointer<io_uring_sqe>;
        res:Int;

        sqe = io_uring_get_sqe(m_io_uring.ptr);
        io_uring_prep_shutdown(sqe, p_fd[1], SHUT_WR);
 sqe.pointed.user_data  = 1;

        res = io_uring_submit_and_wait(m_io_uring.ptr, 1);
        assert(res != -1);

        res = io_uring_wait_cqe(m_io_uring.ptr, cqe.ptr);
        if (res < 0) {
            fprintf(stderr, "wait: %s\n", strerror(-ret));
            goto err;
        }

        if ( cqe.pointed.res ) {
            if ( cqe.pointed.res  == -EINVAL) {
                fprintf(stdout, "Shutdown not supported, skipping\n");
                goto done;
            }
            fprintf(stderr, "writev: %d\n", cqe.pointed.res );
            goto err;
        }

        io_uring_cqe_seen(m_io_uring.ptr, cqe);
    }

    {
        cqe:CPointer<io_uring_cqe>;
        sqe:CPointer<io_uring_sqe>;
        iov:iovec[1];
        char send_buff[128];
        res:Int;

        iov[0].iov_base = send_buff;
        iov[0].iov_len = sizeof(send_buff);

        sqe = io_uring_get_sqe(m_io_uring.ptr);
        assert(sqe != NULL);

        io_uring_prep_writev(sqe, p_fd[1], iov, 1, 0);
        res = io_uring_submit_and_wait(m_io_uring.ptr, 1);
        assert(res != -1);

        res = io_uring_wait_cqe(m_io_uring.ptr, cqe.ptr);
        if (res < 0) {
            fprintf(stderr, "wait: %s\n", strerror(-ret));
            goto err;
        }

        if ( cqe.pointed.res  != -EPIPE) {
            fprintf(stderr, "writev: %d\n", cqe.pointed.res );
            goto err;
        }
        io_uring_cqe_seen(m_io_uring.ptr, cqe);
    }

    done:
    io_uring_queue_exit(m_io_uring.ptr);
    return 0;
    err:
    io_uring_queue_exit(m_io_uring.ptr);
    return 1;
}
