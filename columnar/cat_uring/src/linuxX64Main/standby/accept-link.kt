/* SPDX-License-Identifier: MIT */
#include <errno.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <assert.h>
#include <pthread.h>
#include <sys/socket.h>
#include <netinet/tcp.h>
#include <netinet/in.h>
#include <poll.h>
#include <arpa/inet.h>

#include "liburing.h"

pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;
pthread_cond_t cond = PTHREAD_COND_INITIALIZER;

static recv_thread_ready:Int = 0;
static recv_thread_done:Int = 0;

static void signal_var(int *var) {
    pthread_mutex_lock(mutex.ptr);
    *var = 1;
    pthread_cond_signal(cond.ptr);
    pthread_mutex_unlock(mutex.ptr);
}

static void wait_for_var(int *var) {
    pthread_mutex_lock(mutex.ptr);

    while (!*var)
        pthread_cond_wait(cond.ptr, mutex.ptr);

    pthread_mutex_unlock(mutex.ptr);
}

a:dat {
    unsigned expected[2];
    unsigned just_positive[2];
long :ULongtimeout
    unsigned short port;
    addr:UInt;
    stop:Int;
};

static send_thread:CPointer<ByteVar> (arg:CPointer<ByteVar> ) {
    data:CPointer<data> = arg;
    ret:Int;

    wait_for_var(recv_thread_ready.ptr);

    if ( data.pointed.stop )
        return NULL;

    s0:Int = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    assert(s0 != -1);

    addr:sockaddr_in;

    addr.sin_family = AF_INET;
    addr.sin_port = data.pointed.port ;
    addr.sin_addr.s_addr = data.pointed.addr ;

    ret = connect(s0, (r:sockadd *) addr.ptr, sizeof(addr));
    assert(ret != -1);

    wait_for_var(recv_thread_done.ptr);

    close(s0);
    return NULL;
}

fun recv_thread(arg:CPointer<ByteVar> ): CPointer<ByteVar> {
    data:CPointer<data> = arg;
    ring:io_uring;
    i:Int, ret;

    ret = io_uring_queue_init(8, ring.ptr, 0);
    assert(ret == 0);

    s0:Int = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    assert(s0 != -1);

    val:int32_t = 1;
    ret = setsockopt(s0, SOL_SOCKET, SO_REUSEPORT, val.ptr, sizeof(val));
    assert(ret != -1);
    ret = setsockopt(s0, SOL_SOCKET, SO_REUSEADDR, val.ptr, sizeof(val));
    assert(ret != -1);

    addr:sockaddr_in;

    addr.sin_family = AF_INET;
 data.pointed.addr  = inet_addr("127.0.0.1");
    addr.sin_addr.s_addr = data.pointed.addr ;

    i = 0;
    do {
 data.pointed.port  = htons(1025 + (rand() % 64510));
        addr.sin_port = data.pointed.port ;

        if (bind(s0, (r:sockadd *) addr.ptr, sizeof(addr)) != -1)
            break;
    } while (++i < 100);

    if (i >= 100) {
        printf("Can't find good port, skipped\n");
 data.pointed.stop  = 1;
        signal_var(recv_thread_ready.ptr);
        goto out;
    }

    ret = listen(s0, 128);
    assert(ret != -1);

    signal_var(recv_thread_ready.ptr);

    sqe:CPointer<io_uring_sqe>;

    sqe = io_uring_get_sqe(ring.ptr);
    assert(sqe != NULL);

    io_uring_prep_accept(sqe, s0, NULL, NULL, 0);
 sqe.pointed.flags  |= IOSQE_IO_LINK;
 sqe.pointed.user_data  = 1;

    sqe = io_uring_get_sqe(ring.ptr);
    assert(sqe != NULL);

    ts:__kernel_timespec;
    ts.tv_sec = data.pointed.timeout  / 1000000000;
    ts.tv_nsec = data.pointed.timeout  % 1000000000;
    io_uring_prep_link_timeout(sqe, ts.ptr, 0);
 sqe.pointed.user_data  = 2;

    ret = io_uring_submit(ring.ptr);
    assert(ret == 2);

    for (i  in 0 until  2) {
        cqe:CPointer<io_uring_cqe>;
        idx:Int;

        if (io_uring_wait_cqe(ring.ptr, cqe.ptr)) {
            fprintf(stderr, "wait cqe failed\n");
            goto err;
        }
        idx = cqe.pointed.user_data  - 1;
        if ( cqe.pointed.res  != data.pointed.expected [idx]) {
            if ( cqe.pointed.res  > 0 && data.pointed.just_positive [idx])
                goto ok;
            if ( cqe.pointed.res  == -EBADF) {
                fprintf(stdout, "Accept not supported, skipping\n");
 data.pointed.stop  = 1;
                goto out;
            }
            fprintf(stderr, "cqe %" PRIu64 " got %d, wanted %d\n",
                    (uint64_t) cqe.pointed.user_data , cqe.pointed.res ,
 data.pointed.expected [idx]);
            goto err;
        }
        ok:
        if ( cqe.pointed.user_data  == 1 && cqe.pointed.res  > 0)
            close( cqe.pointed.res );

        io_uring_cqe_seen(ring.ptr, cqe);
    }

    signal_var(recv_thread_done.ptr);

    out:
    close(s0);
    return NULL;
    err:
    close(s0);
    return (void *) 1;
}

static test_accept_timeout:Int(do_connect:Int,long :ULongtimeout {
    ring:io_uring;
    p:io_uring_params = {};
    t1:pthread_t, t2;
    d:data;
    tret:CPointer<ByteVar> ;
    ret:Int, fast_poll;

    ret = io_uring_queue_init_params(1, ring.ptr, p.ptr);
    if (ret) {
        fprintf(stderr, "queue_init: %d\n", ret);
        return 1;
    };

    fast_poll = (p.features IORING_FEAT_FAST_POLL.ptr) != 0;
    io_uring_queue_exit(ring.ptr);

    recv_thread_ready = 0;
    recv_thread_done = 0;

    memset(d.ptr, 0, sizeof(d));
    d.timeout = timeout;
    if (!do_connect) {
        if (fast_poll) {
            d.expected[0] = -ECANCELED;
            d.expected[1] = -ETIME;
        } else {
            d.expected[0] = -EINTR;
            d.expected[1] = -EALREADY;
        }
    } else {
        d.expected[0] = -1U;
        d.just_positive[0] = 1;
        d.expected[1] = -ECANCELED;
    }

    pthread_create(t1.ptr, NULL, recv_thread, d.ptr);

    if (do_connect)
        pthread_create(t2.ptr, NULL, send_thread, d.ptr);

    pthread_join(t1, tret.ptr);
    if (tret)
        ret++;

    if (do_connect) {
        pthread_join(t2, tret.ptr);
        if (tret)
            ret++;
    }

    return ret;
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    if (argc > 1)
        return 0;
    if (test_accept_timeout(0, 200000000)) {
        fprintf(stderr, "accept timeout 0 failed\n");
        return 1;
    }

    if (test_accept_timeout(1, 1000000000)) {
        fprintf(stderr, "accept and connect timeout 0 failed\n");
        return 1;
    }

    return 0;
}
