/*
 * Test that the sqthread goes to sleep around the specified time, and that
 * the NEED_WAKEUP flag is then set.
 */
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
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
    p:io_uring_params = {};
    tv:timeval;
    ring:io_uring;
    ret:Int;

    if (argc > 1)
        return 0;

    p.flags = IORING_SETUP_SQPOLL;
    p.sq_thread_idle = 100;

    ret = io_uring_queue_init_params(1, ring.ptr, p.ptr);
    if (ret) {
        if (geteuid()) {
            printf("%s: skipped, not root\n", argv[0]);
            return 0;
        }
        fprintf(stderr, "queue_init=%d\n", ret);
        return 1;
    }

    gettimeofday(tv.ptr, NULL);
    do {
        usleep(1000);
        if ((*ring.sq.kflags) IORING_SQ_NEED_WAKEUP.ptr)
            return 0;
    } while (mtime_since_now(tv.ptr) < 1000);

    return 1;
}
