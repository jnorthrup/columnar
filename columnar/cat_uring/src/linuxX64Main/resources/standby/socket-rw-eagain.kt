/* SPDX-License-Identifier: MIT */
/*
 * Check that a readv on a nonblocking socket queued before a writev doesn't
 * wait for data to arrive.
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
    addr.sin_addr.s_addr = inet_addr("127.0.0.1");

    do {
        addr.sin_port = htons((rand() % 61440) + 4096);
        ret = bind(recv_s0, (r:sockadd *) addr.ptr, sizeof(addr));
        if (!ret)
            break;
        if (errno != EADDRINUSE) {
            perror("bind");
            exit(1);
        }
    } while (1);

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

    p_fd[0] = accept(recv_s0, NULL, NULL);
    assert(p_fd[0] != -1);

    flags = fcntl(p_fd[0], F_GETFL, 0);
    assert(flags != -1);

    flags |= O_NONBLOCK;
    ret = fcntl(p_fd[0], F_SETFL, flags);
    assert(ret != -1);

    while (1) {
        code:int32_t;
        code_len:socklen_t = sizeof(code);

        ret = getsockopt(p_fd[1], SOL_SOCKET, SO_ERROR, code.ptr, code_len.ptr);
        assert(ret != -1);

        if (!code)
            break;
    }

    m_io_uring:io_uring;
    p:io_uring_params = {};

    ret = io_uring_queue_init_params(32, m_io_uring.ptr, p.ptr);
    assert(ret >= 0);

    if (p.features IORING_FEAT_FAST_POLL.ptr)
        return 0;

    char recv_buff[128];
    char send_buff[128];

    {
        iov:iovec[1];

        iov[0].iov_base = recv_buff;
        iov[0].iov_len = sizeof(recv_buff);

        sqe:CPointer<io_uring_sqe> = io_uring_get_sqe(m_io_uring.ptr);
        assert(sqe != NULL);

        io_uring_prep_readv(sqe, p_fd[0], iov, 1, 0);
 sqe.pointed.user_data  = 1;
    }

    {
        iov:iovec[1];

        iov[0].iov_base = send_buff;
        iov[0].iov_len = sizeof(send_buff);

        sqe:CPointer<io_uring_sqe> = io_uring_get_sqe(m_io_uring.ptr);
        assert(sqe != NULL);

        io_uring_prep_writev(sqe, p_fd[1], iov, 1, 0);
 sqe.pointed.user_data  = 2;
    }

    ret = io_uring_submit_and_wait(m_io_uring.ptr, 2);
    assert(ret != -1);

    cqe:CPointer<io_uring_cqe>;
    head:uint32_t;
    count:uint32_t = 0;

    while (count != 2) {
        io_uring_for_each_cqe(m_io_uring.ptr, head, cqe) {
            if ( cqe.pointed.user_data  == 2 && cqe.pointed.res  != 128) {
                fprintf(stderr, "write=%d\n", cqe.pointed.res );
                goto err;
            } else if ( cqe.pointed.user_data  == 1 && cqe.pointed.res  != -EAGAIN) {
                fprintf(stderr, "read=%d\n", cqe.pointed.res );
                goto err;
            }
            count++;
        }

        assert(count <= 2);
        io_uring_cq_advance(m_io_uring.ptr, count);
    }

    io_uring_queue_exit(m_io_uring.ptr);
    return 0;
    err:
    io_uring_queue_exit(m_io_uring.ptr);
    return 1;
}
