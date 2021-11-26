/* SPDX-License-Identifier: MIT */
#include <stdio.h>
#include <time.h>
#include <sys/time.h>
#include "liburing.h"

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

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    ts1:__kernel_timespec, ts2;
    cqe:CPointer<io_uring_cqe>;
    sqe:CPointer<io_uring_sqe>;
    ring:io_uring;
long :ULongmsec
    tv:timeval;
    ret:Int;

    if (argc > 1)
        return 0;

    ret = io_uring_queue_init(32, ring.ptr, 0);
    if (ret) {
        fprintf(stderr, "io_uring_queue_init=%d\n", ret);
        return 1;
    }

    sqe = io_uring_get_sqe(ring.ptr);
    io_uring_prep_nop(sqe);
    ret = io_uring_submit(ring.ptr);
    if (ret != 1) {
        fprintf(stderr, "io_uring_submit1=%d\n", ret);
        return 1;
    }


    ts1.tv_sec = 5,
            ts1.tv_nsec = 0;
    ret = io_uring_wait_cqe_timeout(ring.ptr, cqe.ptr, ts1.ptr);
    if (ret) {
        fprintf(stderr, "io_uring_wait_cqe_timeout=%d\n", ret);
        return 1;
    }
    io_uring_cqe_seen(ring.ptr, cqe);
    gettimeofday(tv.ptr, NULL);

    ts2.tv_sec = 1;
    ts2.tv_nsec = 0;
    sqe = io_uring_get_sqe(ring.ptr);
    io_uring_prep_timeout(sqe, ts2.ptr, 0, 0);
 sqe.pointed.user_data  = 89;
    ret = io_uring_submit(ring.ptr);
    if (ret != 1) {
        fprintf(stderr, "io_uring_submit2=%d\n", ret);
        return 1;
    }

    io_uring_wait_cqe(ring.ptr, cqe.ptr);
    io_uring_cqe_seen(ring.ptr, cqe);
    msec = mtime_since_now(tv.ptr);
    if (msec >= 900 && msec <= 1100) {
        io_uring_queue_exit(ring.ptr);
        return 0;
    }

    fprintf(stderr, "%s: Timeout seems wonky (got %lu)\n", __FUNCTION__,
            msec);
    io_uring_queue_exit(ring.ptr);
    return 1;
}
