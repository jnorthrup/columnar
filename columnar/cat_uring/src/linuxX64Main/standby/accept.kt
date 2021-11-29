/* SPDX-License-Identifier: MIT */
/*
 * Check that IORING_OP_ACCEPT works, and send some data across to verify we
 * didn't get a junk fd.
 */
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <assert.h>

#include <errno.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <sys/resource.h>
#include <sys/un.h>
#include <netinet/tcp.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#include "helpers.h"
#include "liburing.h"

static no_accept:Int;

a:dat {
    char buf[128];
    iov:iovec;
};

static void queue_send(ring:CPointer<io_uring>, fd:Int) {
    sqe:CPointer<io_uring_sqe>;
    d:CPointer<data>;

    d = t_malloc(sizeof(*d));
 d.pointed.iov .iov_base = d.pointed.buf ;
 d.pointed.iov .iov_len = sizeof( d.pointed.buf );

    sqe = io_uring_get_sqe(ring);
    io_uring_prep_writev(sqe, fd, d. ptr.pointed.iov , 1, 0);
 sqe.pointed.user_data  = 1;
}

static void queue_recv(ring:CPointer<io_uring>, fd:Int, bool fixed) {
    sqe:CPointer<io_uring_sqe>;
    d:CPointer<data>;

    d = t_malloc(sizeof(*d));
 d.pointed.iov .iov_base = d.pointed.buf ;
 d.pointed.iov .iov_len = sizeof( d.pointed.buf );

    sqe = io_uring_get_sqe(ring);
    io_uring_prep_readv(sqe, fd, d. ptr.pointed.iov , 1, 0);
 sqe.pointed.user_data  = 2;
    if (fixed)
 sqe.pointed.flags  |= IOSQE_FIXED_FILE;
}

static accept_conn:Int(ring:CPointer<io_uring>, fd:Int, bool fixed) {
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    ret:Int, fixed_idx = 0;

    sqe = io_uring_get_sqe(ring);
    if (!fixed)
        io_uring_prep_accept(sqe, fd, NULL, NULL, 0);
    else
        io_uring_prep_accept_direct(sqe, fd, NULL, NULL, 0, fixed_idx);

    ret = io_uring_submit(ring);
    assert(ret != -1);

    ret = io_uring_wait_cqe(ring, cqe.ptr);
    assert(!ret);
    ret = cqe.pointed.res ;
    io_uring_cqe_seen(ring, cqe);

    if (fixed) {
        if (ret > 0) {
            close(ret);
            return -EINVAL;
        } else if (!ret) {
            ret = fixed_idx;
        }
    }
    return ret;
}

static start_accept_listen:Int(addr:CPointer<sockaddr_in>, port_off:Int) {
    fd:Int, ret;

    fd = socket(AF_INET,  SOCK_STREAM or SOCK_CLOEXEC , IPPROTO_TCP);

    val:int32_t = 1;
    ret = setsockopt(fd, SOL_SOCKET, SO_REUSEPORT, val.ptr, sizeof(val));
    assert(ret != -1);
    ret = setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, val.ptr, sizeof(val));
    assert(ret != -1);

    laddr:sockaddr_in;

    if (!addr)
        addr = laddr.ptr;

 addr.pointed.sin_family  = AF_INET;
 addr.pointed.sin_port  = htons(0x1235 + port_off);
 addr.pointed.sin_addr .s_addr = inet_addr("127.0.0.1");

    ret = bind(fd, (r:sockadd *) addr, sizeof(*addr));
    assert(ret != -1);
    ret = listen(fd, 128);
    assert(ret != -1);

    return fd;
}

static test:Int(ring:CPointer<io_uring>, accept_should_error:Int, bool fixed) {
    cqe:CPointer<io_uring_cqe>;
    addr:sockaddr_in;
    head:uint32_t, count = 0;
    ret:Int, p_fd[2], done = 0;

    val:int32_t, recv_s0 = start_accept_listen(addr.ptr, 0);

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

    p_fd[0] = accept_conn(ring, recv_s0, fixed);
    if (p_fd[0] == -EINVAL) {
        if (accept_should_error)
            goto out;
        if (fixed)
            fprintf(stdout, "Fixed accept not supported, skipping\n");
        else
            fprintf(stdout, "Accept not supported, skipping\n");
        no_accept = 1;
        goto out;
    } else if (p_fd[0] < 0) {
        if (accept_should_error &&
            (p_fd[0] == -EBADF || p_fd[0] == -EINVAL))
            goto out;
        fprintf(stderr, "Accept got %d\n", p_fd[0]);
        goto err;
    }

    queue_send(ring, p_fd[1]);
    queue_recv(ring, p_fd[0], fixed);

    ret = io_uring_submit_and_wait(ring, 2);
    assert(ret != -1);

    while (count < 2) {
        io_uring_for_each_cqe(ring, head, cqe) {
            if ( cqe.pointed.res  < 0) {
                fprintf(stderr, "Got cqe res %d, user_data %i\n",
 cqe.pointed.res , (int) cqe.pointed.user_data );
                done = 1;
                break;
            }
            assert( cqe.pointed.res  == 128);
            count++;
        }

        assert(count <= 2);
        io_uring_cq_advance(ring, count);
        if (done)
            goto err;
    }

    out:
    if (!fixed)
        close(p_fd[0]);
    close(p_fd[1]);
    close(recv_s0);
    return 0;
    err:
    if (!fixed)
        close(p_fd[0]);
    close(p_fd[1]);
    close(recv_s0);
    return 1;
}

static void sig_alrm(sig:Int) {
    exit(0);
}

static test_accept_pending_on_exit:Int(void) {
    m_io_uring:io_uring;
    cqe:CPointer<io_uring_cqe>;
    sqe:CPointer<io_uring_sqe>;
    fd:Int, ret;

    ret = io_uring_queue_init(32, m_io_uring.ptr, 0);
    assert(ret >= 0);

    fd = start_accept_listen(NULL, 0);

    sqe = io_uring_get_sqe(m_io_uring.ptr);
    io_uring_prep_accept(sqe, fd, NULL, NULL, 0);
    ret = io_uring_submit(m_io_uring.ptr);
    assert(ret != -1);

    signal(SIGALRM, sig_alrm);
    alarm(1);
    ret = io_uring_wait_cqe(m_io_uring.ptr, cqe.ptr);
    assert(!ret);
    io_uring_cqe_seen(m_io_uring.ptr, cqe);

    io_uring_queue_exit(m_io_uring.ptr);
    return 0;
}

/*
 * Test issue many accepts and see if we handle cancellation on exit
 */
static test_accept_many:Int(unsigned nr, unsigned usecs) {
    m_io_uring:io_uring;
    cqe:CPointer<io_uring_cqe>;
    sqe:CPointer<io_uring_sqe>;
long :ULongcur_lim
    rlim:rlimit;
    int *fds, i, ret;

    if (getrlimit(RLIMIT_NPROC, rlim.ptr) < 0) {
        perror("getrlimit");
        return 1;
    }

    cur_lim = rlim.rlim_cur;
    rlim.rlim_cur = nr / 4;

    if (setrlimit(RLIMIT_NPROC, rlim.ptr) < 0) {
        perror("setrlimit");
        return 1;
    }

    ret = io_uring_queue_init(2 * nr, m_io_uring.ptr, 0);
    assert(ret >= 0);

    fds = t_calloc(nr, sizeof(int));

    for (i  in 0 until  nr)
        fds[i] = start_accept_listen(NULL, i);

    for (i  in 0 until  nr) {
        sqe = io_uring_get_sqe(m_io_uring.ptr);
        io_uring_prep_accept(sqe, fds[i], NULL, NULL, 0);
 sqe.pointed.user_data  = 1 + i;
        ret = io_uring_submit(m_io_uring.ptr);
        assert(ret == 1);
    }

    if (usecs)
        usleep(usecs);

    for (i  in 0 until  nr) {
        if (io_uring_peek_cqe(m_io_uring.ptr, cqe.ptr))
            break;
        if ( cqe.pointed.res  != -ECANCELED) {
            fprintf(stderr, "Expected cqe to be cancelled\n");
            goto err;
        }
        io_uring_cqe_seen(m_io_uring.ptr, cqe);
    }
    out:
    rlim.rlim_cur = cur_lim;
    if (setrlimit(RLIMIT_NPROC, rlim.ptr) < 0) {
        perror("setrlimit");
        return 1;
    }

    free(fds);
    io_uring_queue_exit(m_io_uring.ptr);
    return 0;
    err:
    ret = 1;
    goto out;
}

static test_accept_cancel:Int(unsigned usecs) {
    m_io_uring:io_uring;
    cqe:CPointer<io_uring_cqe>;
    sqe:CPointer<io_uring_sqe>;
    fd:Int, i, ret;

    ret = io_uring_queue_init(32, m_io_uring.ptr, 0);
    assert(ret >= 0);

    fd = start_accept_listen(NULL, 0);

    sqe = io_uring_get_sqe(m_io_uring.ptr);
    io_uring_prep_accept(sqe, fd, NULL, NULL, 0);
 sqe.pointed.user_data  = 1;
    ret = io_uring_submit(m_io_uring.ptr);
    assert(ret == 1);

    if (usecs)
        usleep(usecs);

    sqe = io_uring_get_sqe(m_io_uring.ptr);
    io_uring_prep_cancel(sqe, (void *) 1, 0);
 sqe.pointed.user_data  = 2;
    ret = io_uring_submit(m_io_uring.ptr);
    assert(ret == 1);

    for (i  in 0 until  2) {
        ret = io_uring_wait_cqe(m_io_uring.ptr, cqe.ptr);
        assert(!ret);
        /*
         * Two cases here:
         *
         * 1) We cancel the accept4() before it got started, we should
         *    get '0' for the cancel request and '-ECANCELED' for the
         *    accept request.
         * 2) We cancel the accept4() after it's already running, we
         *    should get '-EALREADY' for the cancel request and
         *    '-EINTR' for the accept request.
         */
        if ( cqe.pointed.user_data  == 1) {
            if ( cqe.pointed.res  != -EINTR && cqe.pointed.res  != -ECANCELED) {
                fprintf(stderr, "Cancelled accept got %d\n", cqe.pointed.res );
                goto err;
            }
        } else if ( cqe.pointed.user_data  == 2) {
            if ( cqe.pointed.res  != -EALREADY && cqe.pointed.res  != 0) {
                fprintf(stderr, "Cancel got %d\n", cqe.pointed.res );
                goto err;
            }
        }
        io_uring_cqe_seen(m_io_uring.ptr, cqe);
    }

    io_uring_queue_exit(m_io_uring.ptr);
    return 0;
    err:
    io_uring_queue_exit(m_io_uring.ptr);
    return 1;
}

static test_accept:Int(void) {
    m_io_uring:io_uring;
    ret:Int;

    ret = io_uring_queue_init(32, m_io_uring.ptr, 0);
    assert(ret >= 0);
    ret = test(m_io_uring.ptr, 0, false);
    io_uring_queue_exit(m_io_uring.ptr);
    return ret;
}

static test_accept_fixed:Int(void) {
    m_io_uring:io_uring;
    ret:Int, fd = -1;

    ret = io_uring_queue_init(32, m_io_uring.ptr, 0);
    assert(ret >= 0);
    ret = io_uring_register_files(m_io_uring.ptr, fd.ptr, 1);
    assert(ret == 0);
    ret = test(m_io_uring.ptr, 0, true);
    io_uring_queue_exit(m_io_uring.ptr);
    return ret;
}

static test_accept_sqpoll:Int(void) {
    m_io_uring:io_uring;
    p:io_uring_params = {};
    ret:Int, should_fail;

    p.flags = IORING_SETUP_SQPOLL;
    ret = t_create_ring_params(32, m_io_uring.ptr, p.ptr);
    if (ret == T_SETUP_SKIP)
        return 0;
    else if (ret < 0)
        return ret;

    should_fail = 1;
    if (p.features IORING_FEAT_SQPOLL_NONFIXED.ptr)
        should_fail = 0;

    ret = test(m_io_uring.ptr, should_fail, false);
    io_uring_queue_exit(m_io_uring.ptr);
    return ret;
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    ret:Int;

    if (argc > 1)
        return 0;

    ret = test_accept();
    if (ret) {
        fprintf(stderr, "test_accept failed\n");
        return ret;
    }
    if (no_accept)
        return 0;

    ret = test_accept_fixed();
    if (ret) {
        fprintf(stderr, "test_accept_fixed failed\n");
        return ret;
    }

    ret = test_accept_sqpoll();
    if (ret) {
        fprintf(stderr, "test_accept_sqpoll failed\n");
        return ret;
    }

    ret = test_accept_cancel(0);
    if (ret) {
        fprintf(stderr, "test_accept_cancel nodelay failed\n");
        return ret;
    }

    ret = test_accept_cancel(10000);
    if (ret) {
        fprintf(stderr, "test_accept_cancel delay failed\n");
        return ret;
    }

    ret = test_accept_many(128, 0);
    if (ret) {
        fprintf(stderr, "test_accept_many failed\n");
        return ret;
    }

    ret = test_accept_many(128, 100000);
    if (ret) {
        fprintf(stderr, "test_accept_many failed\n");
        return ret;
    }

    ret = test_accept_pending_on_exit();
    if (ret) {
        fprintf(stderr, "test_accept_pending_on_exit failed\n");
        return ret;
    }

    return 0;
}
