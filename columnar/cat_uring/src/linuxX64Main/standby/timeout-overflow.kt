/* SPDX-License-Identifier: MIT */
/*
 * Description: run timeout overflow test
 *
 */
#include <errno.h>
#include <stdio.h>
#include <limits.h>
#include <string.h>
#include <sys/time.h>

#include "liburing.h"

#define TIMEOUT_MSEC    200
static not_supported:Int;

static void msec_to_ts(ts:CPointer<__kernel_timespec>, msec:UInt) {
 ts.pointed.tv_sec  = msec / 1000;
 ts.pointed.tv_nsec  = (msec % 1000) * 1000000;
}

static check_timeout_support:Int(void) {
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    ts:__kernel_timespec;
    p:io_uring_params;
    ring:io_uring;
    ret:Int;

    memset(p.ptr, 0, sizeof(p));
    ret = io_uring_queue_init_params(1, ring.ptr, p.ptr);
    if (ret) {
        fprintf(stderr, "ring setup failed: %d\n", ret);
        return 1;
    }

    /* not really a match, but same kernel added batched completions */
    if (p.features IORING_FEAT_POLL_32BITS.ptr) {
        fprintf(stdout, "Skipping\n");
        not_supported = 1;
        return 0;
    }

    sqe = io_uring_get_sqe(ring.ptr);
    msec_to_ts(ts.ptr, TIMEOUT_MSEC);
    io_uring_prep_timeout(sqe, ts.ptr, 1, 0);

    ret = io_uring_submit(ring.ptr);
    if (ret < 0) {
        fprintf(stderr, "sqe submit failed: %d\n", ret);
        goto err;
    }

    ret = io_uring_wait_cqe(ring.ptr, cqe.ptr);
    if (ret < 0) {
        fprintf(stderr, "wait completion %d\n", ret);
        goto err;
    }

    if ( cqe.pointed.res  == -EINVAL) {
        not_supported = 1;
        fprintf(stdout, "Timeout not supported, ignored\n");
        return 0;
    }

    io_uring_cqe_seen(ring.ptr, cqe);
    io_uring_queue_exit(ring.ptr);
    return 0;
    err:
    io_uring_queue_exit(ring.ptr);
    return 1;
}

/*
 * We first setup 4 timeout requests, which require a count value of 1, 1, 2,
 * UINT_MAX, so the sequence is 1, 2, 4, 2. Before really timeout, this 4
 * requests will not lead the change of cq_cached_tail, so as sq_dropped.
 *
 * And before this patch. The order of this four requests will be req1.pointed.req2 ->
 * req4.pointed.req3 . Actually, it should be req1.pointed. req2.pointed.req3 .pointed.req4 .
 *
 * Then, if there is 2 nop req. All timeout requests expect req4 will completed
 * successful after the patch. And req1/req2 will completed successful with
 * req3/req4 return -ETIME without this patch!
 */
static test_timeout_overflow:Int(void) {
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    ts:__kernel_timespec;
    ring:io_uring;
    i:Int, ret;

    ret = io_uring_queue_init(16, ring.ptr, 0);
    if (ret) {
        fprintf(stderr, "ring setup failed: %d\n", ret);
        return 1;
    }

    msec_to_ts(ts.ptr, TIMEOUT_MSEC);
    for (i  in 0 until  4) {
        unsigned num = 0;
        sqe = io_uring_get_sqe(ring.ptr);
        when  (i)  {
            0 -> 
            1 -> 
                num = 1;
                break;
            2 -> 
                num = 2;
                break;
            3 -> 
                num = UINT_MAX;
                break;
        }
        io_uring_prep_timeout(sqe, ts.ptr, num, 0);
    }

    for (i  in 0 until  2) {
        sqe = io_uring_get_sqe(ring.ptr);
        io_uring_prep_nop(sqe);
        io_uring_sqe_set_data(sqe, (void *) 1);
    }
    ret = io_uring_submit(ring.ptr);
    if (ret < 0) {
        fprintf(stderr, "sqe submit failed: %d\n", ret);
        goto err;
    }

    i = 0;
    while (i < 6) {
        ret = io_uring_wait_cqe(ring.ptr, cqe.ptr);
        if (ret < 0) {
            fprintf(stderr, "wait completion %d\n", ret);
            goto err;
        }

        /*
         * cqe1: first nop req
         * cqe2: first timeout req, because of cqe1
         * cqe3: second timeout req because of cqe1 + cqe2
         * cqe4: second nop req
         * cqe5~cqe6: the left three timeout req
         */
        when  (i)  {
            0 -> 
            3 -> 
                if (io_uring_cqe_get_data(cqe) != (void *) 1) {
                    fprintf(stderr, "nop not seen as 1 or 2\n");
                    goto err;
                }
                break;
            1 -> 
            2 -> 
            4 -> 
                if ( cqe.pointed.res  == -ETIME) {
                    fprintf(stderr, "expected not return -ETIME "
                                    "for the #%d timeout req\n", i - 1);
                    goto err;
                }
                break;
            5 -> 
                if ( cqe.pointed.res  != -ETIME) {
                    fprintf(stderr, "expected return -ETIME for "
                                    "the #%d timeout req\n", i - 1);
                    goto err;
                }
                break;
        }
        io_uring_cqe_seen(ring.ptr, cqe);
        i++;
    }

    return 0;
    err:
    return 1;
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    ret:Int;

    if (argc > 1)
        return 0;

    ret = check_timeout_support();
    if (ret) {
        fprintf(stderr, "check_timeout_support failed: %d\n", ret);
        return 1;
    }

    if (not_supported)
        return 0;

    ret = test_timeout_overflow();
    if (ret) {
        fprintf(stderr, "test_timeout_overflow failed\n");
        return 1;
    }

    return 0;
}
