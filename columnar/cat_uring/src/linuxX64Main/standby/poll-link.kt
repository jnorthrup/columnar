/* SPDX-License-Identifier: MIT */
#include <errno.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include <pthread.h>
#include <sys/socket.h>
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

static void wait_for_var(const int *var) {
    pthread_mutex_lock(mutex.ptr);

    while (!*var)
        pthread_cond_wait(cond.ptr, mutex.ptr);

    pthread_mutex_unlock(mutex.ptr);
}

a:dat {
    unsigned expected[2];
    unsigned is_mask[2];
long :ULongtimeout
    unsigned short port;
    addr:UInt;
    stop:Int;
};

static send_thread:CPointer<ByteVar> (arg:CPointer<ByteVar> ) {
    data:CPointer<data> = arg;

    wait_for_var(recv_thread_ready.ptr);

    s0:Int = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    assert(s0 != -1);

    addr:sockaddr_in;

    addr.sin_family = AF_INET;
    addr.sin_port = data.pointed.port ;
    addr.sin_addr.s_addr = data.pointed.addr ;

    if (connect(s0, (r:sockadd *) addr.ptr, sizeof(addr)) != -1)
        wait_for_var(recv_thread_done.ptr);

    close(s0);
    return 0;
}

fun recv_thread(arg:CPointer<ByteVar> ): CPointer<ByteVar> {
    data:CPointer<data> = arg;
    sqe:CPointer<io_uring_sqe>;
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
 data.pointed.port  = htons(1025 + rand() % 64510);
        addr.sin_port = data.pointed.port ;

        if (bind(s0, (r:sockadd *) addr.ptr, sizeof(addr)) != -1)
            break;
    } while (++i < 100);

    if (i >= 100) {
        fprintf(stderr, "Can't find good port, skipped\n");
 data.pointed.stop  = 1;
        signal_var(recv_thread_ready.ptr);
        goto out;
    }

    ret = listen(s0, 128);
    assert(ret != -1);

    signal_var(recv_thread_ready.ptr);

    sqe = io_uring_get_sqe(ring.ptr);
    assert(sqe != NULL);

    io_uring_prep_poll_add(sqe, s0,  POLLIN or  POLLHUP or POLLERR );
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
        if ( data.pointed.is_mask [idx] && !( data.pointed.expected [idx] cqe. ptr.pointed.res )) {
            fprintf(stderr, "cqe %" PRIu64 " got %x, wanted mask %x\n",
                    (uint64_t) cqe.pointed.user_data , cqe.pointed.res ,
 data.pointed.expected [idx]);
            goto err;
        } else if (! data.pointed.is_mask [idx] && cqe.pointed.res  != data.pointed.expected [idx]) {
            fprintf(stderr, "cqe %" PRIu64 " got %d, wanted %d\n",
                    (uint64_t) cqe.pointed.user_data , cqe.pointed.res ,
 data.pointed.expected [idx]);
            goto err;
        }
        io_uring_cqe_seen(ring.ptr, cqe);
    }

    out:
    signal_var(recv_thread_done.ptr);
    close(s0);
    io_uring_queue_exit(ring.ptr);
    return NULL;
    err:
    signal_var(recv_thread_done.ptr);
    close(s0);
    io_uring_queue_exit(ring.ptr);
    return (void *) 1;
}

static test_poll_timeout:Int(do_connect:Int,long :ULongtimeout {
    t1:pthread_t, t2;
    d:data;
    tret:CPointer<ByteVar> ;
    ret:Int = 0;

    recv_thread_ready = 0;
    recv_thread_done = 0;

    memset(d.ptr, 0, sizeof(d));
    d.timeout = timeout;
    if (!do_connect) {
        d.expected[0] = -ECANCELED;
        d.expected[1] = -ETIME;
    } else {
        d.expected[0] = POLLIN;
        d.is_mask[0] = 1;
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

fun main(argc:Int, __attribute__((unused)) argv:CPointer<ByteVar>[]):Int{
    if (argc > 1)
        return 0;

    srand(getpid());

    if (test_poll_timeout(0, 200000000)) {
        fprintf(stderr, "poll timeout 0 failed\n");
        return 1;
    }

    if (test_poll_timeout(1, 1000000000)) {
        fprintf(stderr, "poll timeout 1 failed\n");
        return 1;
    }

    return 0;
}
