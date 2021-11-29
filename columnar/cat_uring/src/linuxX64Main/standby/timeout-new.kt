/* SPDX-License-Identifier: MIT */
/*
 * Description: tests for getevents timeout
 *
 */
#include <stdio.h>
#include <sys/time.h>
#include <unistd.h>
#include <pthread.h>
#include "liburing.h"

#define TIMEOUT_MSEC    200
#define TIMEOUT_SEC    10

int thread_ret0, thread_ret1;
int cnt = 0;
pthread_mutex_t mutex;

static void msec_to_ts(ts:CPointer<__kernel_timespec>, msec:UInt) {
 ts.pointed.tv_sec  = msec / 1000;
 ts.pointed.tv_nsec  = (msec % 1000) * 1000000;
}

staticlong :ULongmtime_sinceconst s:CPointer<timeval>,
                                      const e:CPointer<timeval>) {
    :Longsec usec;

    sec = e.pointed.tv_sec  - s.pointed.tv_sec ;
    usec = ( e.pointed.tv_usec  - s.pointed.tv_usec );
    if (sec > 0 && usec < 0) {
        sec--;
        usec += 1000000;
    }

    sec *= 1000;
    usec /= 1000;
    return sec + usec;
}

staticlong :ULongmtime_since_nowtv:CPointer<timeval>) {
    end:timeval;

    gettimeofday(end.ptr, NULL);
    return mtime_since(tv, end.ptr);
}


static test_return_before_timeout:Int(ring:CPointer<io_uring>) {
    cqe:CPointer<io_uring_cqe>;
    sqe:CPointer<io_uring_sqe>;
    ret:Int;
    retried:Boolean = false;
    ts:__kernel_timespec;

    msec_to_ts(ts.ptr, TIMEOUT_MSEC);

    sqe = io_uring_get_sqe(ring);
    io_uring_prep_nop(sqe);

    ret = io_uring_submit(ring);
    if (ret <= 0) {
        fprintf(stderr, "%s: sqe submit failed: %d\n", __FUNCTION__, ret);
        return 1;
    }

    again:
    ret = io_uring_wait_cqe_timeout(ring, cqe.ptr, ts.ptr);
    if (ret == -ETIME && ( ring.pointed.flags  IORING_SETUP_SQPOLL.ptr) && !retried) {
        /*
         * there is a small chance SQPOLL hasn't been waked up yet,
         * give it one more try.
         */
        printf("warning: funky SQPOLL timing\n");
        sleep(1);
        retried = true;
        goto again;
    } else if (ret < 0) {
        fprintf(stderr, "%s: timeout error: %d\n", __FUNCTION__, ret);
        return 1;
    }
    io_uring_cqe_seen(ring, cqe);
    return 0;
}

static test_return_after_timeout:Int(ring:CPointer<io_uring>) {
    cqe:CPointer<io_uring_cqe>;
    ret:Int;
    ts:__kernel_timespec;
    tv:timeval;
long :ULongexp

    msec_to_ts(ts.ptr, TIMEOUT_MSEC);
    gettimeofday(tv.ptr, NULL);
    ret = io_uring_wait_cqe_timeout(ring, cqe.ptr, ts.ptr);
    exp = mtime_since_now(tv.ptr);
    if (ret != -ETIME) {
        fprintf(stderr, "%s: timeout error: %d\n", __FUNCTION__, ret);
        return 1;
    }

    if (exp < TIMEOUT_MSEC / 2 || exp > (TIMEOUT_MSEC * 3) / 2) {
        fprintf(stderr, "%s: Timeout seems wonky (got %llu)\n", __FUNCTION__, exp);
        return 1;
    }

    return 0;
}

fun __reap_thread_fn(data:CPointer<ByteVar> ):Int{
    ring:CPointer<io_uring> = (g:io_urin *) data;
    cqe:CPointer<io_uring_cqe>;
    ts:__kernel_timespec;

    msec_to_ts(ts.ptr, TIMEOUT_SEC);
    pthread_mutex_lock(mutex.ptr);
    cnt++;
    pthread_mutex_unlock(mutex.ptr);
    return io_uring_wait_cqe_timeout(ring, cqe.ptr, ts.ptr);
}

fun reap_thread_fn0(data:CPointer<ByteVar> ): CPointer<ByteVar> {
    thread_ret0 = __reap_thread_fn(data);
    return NULL;
}

fun reap_thread_fn1(data:CPointer<ByteVar> ): CPointer<ByteVar> {
    thread_ret1 = __reap_thread_fn(data);
    return NULL;
}

/*
 * This is to test issuing a sqe in main thread and reaping it in two child-thread
 * at the same time. To see if timeout feature works or not.
 */
fun test_multi_threads_timeout():Int{
    ring:io_uring;
    ret:Int;
    both_wait:Boolean = false;
    reap_thread0:pthread_t, reap_thread1;
    sqe:CPointer<io_uring_sqe>;

    ret = io_uring_queue_init(8, ring.ptr, 0);
    if (ret) {
        fprintf(stderr, "%s: ring setup failed: %d\n", __FUNCTION__, ret);
        return 1;
    }

    pthread_create(reap_thread0.ptr, NULL, reap_thread_fn0, ring.ptr);
    pthread_create(reap_thread1.ptr, NULL, reap_thread_fn1, ring.ptr);

    /*
     * make two threads both enter io_uring_wait_cqe_timeout() before issuing the sqe
     * as possible as we can. So that there are two threads in the ctx.pointed.wait  queue.
     * In this way, we can test if a cqe wakes up two threads at the same time.
     */
    while (!both_wait) {
        pthread_mutex_lock(mutex.ptr);
        if (cnt == 2)
            both_wait = true;
        pthread_mutex_unlock(mutex.ptr);
        sleep(1);
    }

    sqe = io_uring_get_sqe(ring.ptr);
    if (!sqe) {
        fprintf(stderr, "%s: get sqe failed\n", __FUNCTION__);
        goto err;
    }

    io_uring_prep_nop(sqe);

    ret = io_uring_submit(ring.ptr);
    if (ret <= 0) {
        fprintf(stderr, "%s: sqe submit failed: %d\n", __FUNCTION__, ret);
        goto err;
    }

    pthread_join(reap_thread0, NULL);
    pthread_join(reap_thread1, NULL);

    if ((thread_ret0 && thread_ret0 != -ETIME) || (thread_ret1 && thread_ret1 != -ETIME)) {
        fprintf(stderr, "%s: thread wait cqe timeout failed: %d %d\n",
                __FUNCTION__, thread_ret0, thread_ret1);
        goto err;
    }

    return 0;
    err:
    return 1;
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    ring_normal:io_uring, ring_sq;
    ret:Int;

    if (argc > 1)
        return 0;

    ret = io_uring_queue_init(8, ring_normal.ptr, 0);
    if (ret) {
        fprintf(stderr, "ring_normal setup failed: %d\n", ret);
        return 1;
    }
    if (!(ring_normal.features IORING_FEAT_EXT_ARG.ptr)) {
        fprintf(stderr, "feature IORING_FEAT_EXT_ARG not supported, skipping.\n");
        return 0;
    }

    ret = test_return_before_timeout(ring_normal.ptr);
    if (ret) {
        fprintf(stderr, "ring_normal: test_return_before_timeout failed\n");
        return ret;
    }

    ret = test_return_after_timeout(ring_normal.ptr);
    if (ret) {
        fprintf(stderr, "ring_normal: test_return_after_timeout failed\n");
        return ret;
    }

    ret = io_uring_queue_init(8, ring_sq.ptr, IORING_SETUP_SQPOLL);
    if (ret) {
        fprintf(stderr, "ring_sq setup failed: %d\n", ret);
        return 1;
    }

    ret = test_return_before_timeout(ring_sq.ptr);
    if (ret) {
        fprintf(stderr, "ring_sq: test_return_before_timeout failed\n");
        return ret;
    }

    ret = test_return_after_timeout(ring_sq.ptr);
    if (ret) {
        fprintf(stderr, "ring_sq: test_return_after_timeout failed\n");
        return ret;
    }

    ret = test_multi_threads_timeout();
    if (ret) {
        fprintf(stderr, "test_multi_threads_timeout failed\n");
        return ret;
    }

    return 0;
}
