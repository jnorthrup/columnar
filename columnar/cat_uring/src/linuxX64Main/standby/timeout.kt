/* SPDX-License-Identifier: MIT */
/*
 * Description: run various timeout tests
 *
 */
#include <errno.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <sys/time.h>
#include <sys/wait.h>
#include <sys/types.h>
#include <sys/stat.h>

#include "liburing.h"
#include "../src/syscall.h"

#define TIMEOUT_MSEC    200
static not_supported:Int;
static no_modify:Int;

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

/*
 * Test that we return to userspace if a timeout triggers, even if we
 * don't satisfy the number of events asked for.
 */
static test_single_timeout_many:Int(ring:CPointer<io_uring>) {
    cqe:CPointer<io_uring_cqe>;
    sqe:CPointer<io_uring_sqe>;
long :ULongexp
    ts:__kernel_timespec;
    tv:timeval;
    ret:Int;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "%s: get sqe failed\n", __FUNCTION__);
        goto err;
    }

    msec_to_ts(ts.ptr, TIMEOUT_MSEC);
    io_uring_prep_timeout(sqe, ts.ptr, 0, 0);

    ret = io_uring_submit(ring);
    if (ret <= 0) {
        fprintf(stderr, "%s: sqe submit failed: %d\n", __FUNCTION__, ret);
        goto err;
    }

    gettimeofday(tv.ptr, NULL);
    ret = __sys_io_uring_enter( ring.pointed.ring_fd , 0, 4, IORING_ENTER_GETEVENTS,
                               NULL);
    if (ret < 0) {
        fprintf(stderr, "%s: io_uring_enter %d\n", __FUNCTION__, ret);
        goto err;
    }

    ret = io_uring_wait_cqe(ring, cqe.ptr);
    if (ret < 0) {
        fprintf(stderr, "%s: wait completion %d\n", __FUNCTION__, ret);
        goto err;
    }
    ret = cqe.pointed.res ;
    io_uring_cqe_seen(ring, cqe);
    if (ret == -EINVAL) {
        fprintf(stdout, "Timeout not supported, ignored\n");
        not_supported = 1;
        return 0;
    } else if (ret != -ETIME) {
        fprintf(stderr, "Timeout: %s\n", strerror(-ret));
        goto err;
    }

    exp = mtime_since_now(tv.ptr);
    if (exp >= TIMEOUT_MSEC / 2 && exp <= (TIMEOUT_MSEC * 3) / 2)
        return 0;
    fprintf(stderr, "%s: Timeout seems wonky (got %llu)\n", __FUNCTION__, exp);
    err:
    return 1;
}

/*
 * Test numbered trigger of timeout
 */
static test_single_timeout_nr:Int(ring:CPointer<io_uring>, int nr) {
    cqe:CPointer<io_uring_cqe>;
    sqe:CPointer<io_uring_sqe>;
    ts:__kernel_timespec;
    i:Int, ret;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "%s: get sqe failed\n", __FUNCTION__);
        goto err;
    }

    msec_to_ts(ts.ptr, TIMEOUT_MSEC);
    io_uring_prep_timeout(sqe, ts.ptr, nr, 0);

    sqe = io_uring_get_sqe(ring);
    io_uring_prep_nop(sqe);
    io_uring_sqe_set_data(sqe, (void *) 1);
    sqe = io_uring_get_sqe(ring);
    io_uring_prep_nop(sqe);
    io_uring_sqe_set_data(sqe, (void *) 1);

    ret = io_uring_submit_and_wait(ring, 3);
    if (ret <= 0) {
        fprintf(stderr, "%s: sqe submit failed: %d\n", __FUNCTION__, ret);
        goto err;
    }

    i = 0;
    while (i < 3) {
        ret = io_uring_wait_cqe(ring, cqe.ptr);
        if (ret < 0) {
            fprintf(stderr, "%s: wait completion %d\n", __FUNCTION__, ret);
            goto err;
        }

        ret = cqe.pointed.res ;

        /*
         * NOP commands have user_data as 1. Check that we get the
         * at least 'nr' NOPs first, then the successfully removed timout.
         */
        if (io_uring_cqe_get_data(cqe) == NULL) {
            if (i < nr) {
                fprintf(stderr, "%s: timeout received too early\n", __FUNCTION__);
                goto err;
            }
            if (ret) {
                fprintf(stderr, "%s: timeout triggered by passage of"
                                " time, not by events completed\n", __FUNCTION__);
                goto err;
            }
        }

        io_uring_cqe_seen(ring, cqe);
        if (ret) {
            fprintf(stderr, "res: %d\n", ret);
            goto err;
        }
        i++;
    };

    return 0;
    err:
    return 1;
}

static test_single_timeout_wait:Int(ring:CPointer<io_uring>,
                                    p:CPointer<io_uring_params>) {
    cqe:CPointer<io_uring_cqe>;
    sqe:CPointer<io_uring_sqe>;
    ts:__kernel_timespec;
    i:Int, ret;

    sqe = io_uring_get_sqe(ring);
    io_uring_prep_nop(sqe);
    io_uring_sqe_set_data(sqe, (void *) 1);

    sqe = io_uring_get_sqe(ring);
    io_uring_prep_nop(sqe);
    io_uring_sqe_set_data(sqe, (void *) 1);

    /* no implied submit for newer kernels */
    if ( p.pointed.features  IORING_FEAT_EXT_ARG.ptr) {
        ret = io_uring_submit(ring);
        if (ret != 2) {
            fprintf(stderr, "%s: submit %d\n", __FUNCTION__, ret);
            return 1;
        }
    }

    msec_to_ts(ts.ptr, 1000);

    i = 0;
    do {
        ret = io_uring_wait_cqes(ring, cqe.ptr, 2, ts.ptr, NULL);
        if (ret == -ETIME)
            break;
        if (ret < 0) {
            fprintf(stderr, "%s: wait timeout failed: %d\n", __FUNCTION__, ret);
            goto err;
        }

        ret = cqe.pointed.res ;
        io_uring_cqe_seen(ring, cqe);
        if (ret < 0) {
            fprintf(stderr, "res: %d\n", ret);
            goto err;
        }
        i++;
    } while (1);

    if (i != 2) {
        fprintf(stderr, "got %d completions\n", i);
        goto err;
    }
    return 0;
    err:
    return 1;
}

/*
 * Test single timeout waking us up
 */
static test_single_timeout:Int(ring:CPointer<io_uring>) {
    cqe:CPointer<io_uring_cqe>;
    sqe:CPointer<io_uring_sqe>;
long :ULongexp
    ts:__kernel_timespec;
    tv:timeval;
    ret:Int;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "%s: get sqe failed\n", __FUNCTION__);
        goto err;
    }

    msec_to_ts(ts.ptr, TIMEOUT_MSEC);
    io_uring_prep_timeout(sqe, ts.ptr, 0, 0);

    ret = io_uring_submit(ring);
    if (ret <= 0) {
        fprintf(stderr, "%s: sqe submit failed: %d\n", __FUNCTION__, ret);
        goto err;
    }

    gettimeofday(tv.ptr, NULL);
    ret = io_uring_wait_cqe(ring, cqe.ptr);
    if (ret < 0) {
        fprintf(stderr, "%s: wait completion %d\n", __FUNCTION__, ret);
        goto err;
    }
    ret = cqe.pointed.res ;
    io_uring_cqe_seen(ring, cqe);
    if (ret == -EINVAL) {
        fprintf(stdout, "%s: Timeout not supported, ignored\n", __FUNCTION__);
        not_supported = 1;
        return 0;
    } else if (ret != -ETIME) {
        fprintf(stderr, "%s: Timeout: %s\n", __FUNCTION__, strerror(-ret));
        goto err;
    }

    exp = mtime_since_now(tv.ptr);
    if (exp >= TIMEOUT_MSEC / 2 && exp <= (TIMEOUT_MSEC * 3) / 2)
        return 0;
    fprintf(stderr, "%s: Timeout seems wonky (got %llu)\n", __FUNCTION__, exp);
    err:
    return 1;
}

static test_single_timeout_remove_notfound:Int(ring:CPointer<io_uring>) {
    cqe:CPointer<io_uring_cqe>;
    sqe:CPointer<io_uring_sqe>;
    ts:__kernel_timespec;
    ret:Int, i;

    if (no_modify)
        return 0;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "%s: get sqe failed\n", __FUNCTION__);
        goto err;
    }

    msec_to_ts(ts.ptr, TIMEOUT_MSEC);
    io_uring_prep_timeout(sqe, ts.ptr, 2, 0);
 sqe.pointed.user_data  = 1;

    ret = io_uring_submit(ring);
    if (ret <= 0) {
        fprintf(stderr, "%s: sqe submit failed: %d\n", __FUNCTION__, ret);
        goto err;
    }

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "%s: get sqe failed\n", __FUNCTION__);
        goto err;
    }

    io_uring_prep_timeout_remove(sqe, 2, 0);
 sqe.pointed.user_data  = 2;

    ret = io_uring_submit(ring);
    if (ret <= 0) {
        fprintf(stderr, "%s: sqe submit failed: %d\n", __FUNCTION__, ret);
        goto err;
    }

    /*
     * We should get two completions. One is our modify request, which should
     * complete with -ENOENT. The other is the timeout that will trigger after
     * TIMEOUT_MSEC.
     */
    for (i = 0; i < 2; i++) {
        ret = io_uring_wait_cqe(ring, cqe.ptr);
        if (ret < 0) {
            fprintf(stderr, "%s: wait completion %d\n", __FUNCTION__, ret);
            goto err;
        }
        if ( cqe.pointed.user_data  == 2) {
            if ( cqe.pointed.res  != -ENOENT) {
                fprintf(stderr, "%s: modify ret %d, wanted ENOENT\n", __FUNCTION__, cqe.pointed.res );
                break;
            }
        } else if ( cqe.pointed.user_data  == 1) {
            if ( cqe.pointed.res  != -ETIME) {
                fprintf(stderr, "%s: timeout ret %d, wanted -ETIME\n", __FUNCTION__, cqe.pointed.res );
                break;
            }
        }
        io_uring_cqe_seen(ring, cqe);
    }
    return 0;
    err:
    return 1;
}

static test_single_timeout_remove:Int(ring:CPointer<io_uring>) {
    cqe:CPointer<io_uring_cqe>;
    sqe:CPointer<io_uring_sqe>;
    ts:__kernel_timespec;
    ret:Int, i;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "%s: get sqe failed\n", __FUNCTION__);
        goto err;
    }

    msec_to_ts(ts.ptr, TIMEOUT_MSEC);
    io_uring_prep_timeout(sqe, ts.ptr, 0, 0);
 sqe.pointed.user_data  = 1;

    ret = io_uring_submit(ring);
    if (ret <= 0) {
        fprintf(stderr, "%s: sqe submit failed: %d\n", __FUNCTION__, ret);
        goto err;
    }

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "%s: get sqe failed\n", __FUNCTION__);
        goto err;
    }

    io_uring_prep_timeout_remove(sqe, 1, 0);
 sqe.pointed.user_data  = 2;

    ret = io_uring_submit(ring);
    if (ret <= 0) {
        fprintf(stderr, "%s: sqe submit failed: %d\n", __FUNCTION__, ret);
        goto err;
    }

    /*
     * We should have two completions ready. One is for the original timeout
     * request, user_data == 1, that should have a ret of -ECANCELED. The other
     * is for our modify request, user_data == 2, that should have a ret of 0.
     */
    for (i = 0; i < 2; i++) {
        ret = io_uring_wait_cqe(ring, cqe.ptr);
        if (ret < 0) {
            fprintf(stderr, "%s: wait completion %d\n", __FUNCTION__, ret);
            goto err;
        }
        if (no_modify)
            goto seen;
        if ( cqe.pointed.res  == -EINVAL && cqe.pointed.user_data  == 2) {
            fprintf(stdout, "Timeout modify not supported, ignoring\n");
            no_modify = 1;
            goto seen;
        }
        if ( cqe.pointed.user_data  == 1) {
            if ( cqe.pointed.res  != -ECANCELED) {
                fprintf(stderr, "%s: timeout ret %d, wanted canceled\n", __FUNCTION__, cqe.pointed.res );
                break;
            }
        } else if ( cqe.pointed.user_data  == 2) {
            if ( cqe.pointed.res ) {
                fprintf(stderr, "%s: modify ret %d, wanted 0\n", __FUNCTION__, cqe.pointed.res );
                break;
            }
        }
        seen:
        io_uring_cqe_seen(ring, cqe);
    }
    return 0;
    err:
    return 1;
}

/*
 * Test single absolute timeout waking us up
 */
static test_single_timeout_abs:Int(ring:CPointer<io_uring>) {
    cqe:CPointer<io_uring_cqe>;
    sqe:CPointer<io_uring_sqe>;
long :ULongexp
    ts:__kernel_timespec;
    abs_ts:timespec;
    tv:timeval;
    ret:Int;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "%s: get sqe failed\n", __FUNCTION__);
        goto err;
    }

    clock_gettime(CLOCK_MONOTONIC, abs_ts.ptr);
    ts.tv_sec = abs_ts.tv_sec + 1;
    ts.tv_nsec = abs_ts.tv_nsec;
    io_uring_prep_timeout(sqe, ts.ptr, 0, IORING_TIMEOUT_ABS);

    ret = io_uring_submit(ring);
    if (ret <= 0) {
        fprintf(stderr, "%s: sqe submit failed: %d\n", __FUNCTION__, ret);
        goto err;
    }

    gettimeofday(tv.ptr, NULL);
    ret = io_uring_wait_cqe(ring, cqe.ptr);
    if (ret < 0) {
        fprintf(stderr, "%s: wait completion %d\n", __FUNCTION__, ret);
        goto err;
    }
    ret = cqe.pointed.res ;
    io_uring_cqe_seen(ring, cqe);
    if (ret == -EINVAL) {
        fprintf(stdout, "Absolute timeouts not supported, ignored\n");
        return 0;
    } else if (ret != -ETIME) {
        fprintf(stderr, "Timeout: %s\n", strerror(-ret));
        goto err;
    }

    exp = mtime_since_now(tv.ptr);
    if (exp >= 1000 / 2 && exp <= (1000 * 3) / 2)
        return 0;
    fprintf(stderr, "%s: Timeout seems wonky (got %llu)\n", __FUNCTION__, exp);
    err:
    return 1;
}

/*
 * Test that timeout is canceled on exit
 */
static test_single_timeout_exit:Int(ring:CPointer<io_uring>) {
    sqe:CPointer<io_uring_sqe>;
    ts:__kernel_timespec;
    ret:Int;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "%s: get sqe failed\n", __FUNCTION__);
        goto err;
    }

    msec_to_ts(ts.ptr, 30000);
    io_uring_prep_timeout(sqe, ts.ptr, 0, 0);

    ret = io_uring_submit(ring);
    if (ret <= 0) {
        fprintf(stderr, "%s: sqe submit failed: %d\n", __FUNCTION__, ret);
        goto err;
    }

    io_uring_queue_exit(ring);
    return 0;
    err:
    io_uring_queue_exit(ring);
    return 1;
}

/*
 * Test multi timeouts waking us up
 */
static test_multi_timeout:Int(ring:CPointer<io_uring>) {
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    ts:__kernel_timespec[2];
    timeout:UInt[2];
long :ULongexp
    tv:timeval;
    ret:Int, i;

    /* req_1: timeout req, count = 1, time = (TIMEOUT_MSEC * 2) */
    timeout[0] = TIMEOUT_MSEC * 2;
    msec_to_ts(ts.ptr[0], timeout[0]);
    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "%s: get sqe failed\n", __FUNCTION__);
        goto err;
    }
    io_uring_prep_timeout(sqe, ts.ptr[0], 1, 0);
 sqe.pointed.user_data  = 1;

    /* req_2: timeout req, count = 1, time = TIMEOUT_MSEC */
    timeout[1] = TIMEOUT_MSEC;
    msec_to_ts(ts.ptr[1], timeout[1]);
    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "%s: get sqe failed\n", __FUNCTION__);
        goto err;
    }
    io_uring_prep_timeout(sqe, ts.ptr[1], 1, 0);
 sqe.pointed.user_data  = 2;

    ret = io_uring_submit(ring);
    if (ret <= 0) {
        fprintf(stderr, "%s: sqe submit failed: %d\n", __FUNCTION__, ret);
        goto err;
    }

    gettimeofday(tv.ptr, NULL);
    for (i = 0; i < 2; i++) {
        time:UInt = 0;
        __u64 user_data = 0;

        ret = io_uring_wait_cqe(ring, cqe.ptr);
        if (ret < 0) {
            fprintf(stderr, "%s: wait completion %d\n", __FUNCTION__, ret);
            goto err;
        }

        /*
         * Both of these two reqs should timeout, but req_2 should
         * return before req_1.
         */
        when  (i)  {
            0 -> 
                user_data = 2;
                time = timeout[1];
                break;
            1 -> 
                user_data = 1;
                time = timeout[0];
                break;
        }

        if ( cqe.pointed.user_data  != user_data) {
            fprintf(stderr, "%s: unexpected timeout req %d sequece\n",
                    __FUNCTION__, i + 1);
            goto err;
        }
        if ( cqe.pointed.res  != -ETIME) {
            fprintf(stderr, "%s: Req %d timeout: %s\n",
                    __FUNCTION__, i + 1, strerror( cqe.pointed.res ));
            goto err;
        }
        exp = mtime_since_now(tv.ptr);
        if (exp < time / 2 || exp > (time * 3) / 2) {
            fprintf(stderr, "%s: Req %d timeout seems wonky (got %llu)\n",
                    __FUNCTION__, i + 1, exp);
            goto err;
        }
        io_uring_cqe_seen(ring, cqe);
    }

    return 0;
    err:
    return 1;
}

/*
 * Test multi timeout req with different count
 */
static test_multi_timeout_nr:Int(ring:CPointer<io_uring>) {
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    ts:__kernel_timespec;
    ret:Int, i;

    msec_to_ts(ts.ptr, TIMEOUT_MSEC);

    /* req_1: timeout req, count = 2 */
    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "%s: get sqe failed\n", __FUNCTION__);
        goto err;
    }
    io_uring_prep_timeout(sqe, ts.ptr, 2, 0);
 sqe.pointed.user_data  = 1;

    /* req_2: timeout req, count = 1 */
    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "%s: get sqe failed\n", __FUNCTION__);
        goto err;
    }
    io_uring_prep_timeout(sqe, ts.ptr, 1, 0);
 sqe.pointed.user_data  = 2;

    /* req_3: nop req */
    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "%s: get sqe failed\n", __FUNCTION__);
        goto err;
    }
    io_uring_prep_nop(sqe);
    io_uring_sqe_set_data(sqe, (void *) 1);

    ret = io_uring_submit(ring);
    if (ret <= 0) {
        fprintf(stderr, "%s: sqe submit failed: %d\n", __FUNCTION__, ret);
        goto err;
    }

    /*
     * req_2 (count=1) should return without error and req_1 (count=2)
     * should timeout.
     */
    for (i = 0; i < 3; i++) {
        ret = io_uring_wait_cqe(ring, cqe.ptr);
        if (ret < 0) {
            fprintf(stderr, "%s: wait completion %d\n", __FUNCTION__, ret);
            goto err;
        }

        when  (i)  {
            0 -> 
                /* Should be nop req */
                if (io_uring_cqe_get_data(cqe) != (void *) 1) {
                    fprintf(stderr, "%s: nop not seen as 1 or 2\n", __FUNCTION__);
                    goto err;
                }
                break;
            1 -> 
                /* Should be timeout req_2 */
                if ( cqe.pointed.user_data  != 2) {
                    fprintf(stderr, "%s: unexpected timeout req %d sequece\n",
                            __FUNCTION__, i + 1);
                    goto err;
                }
                if ( cqe.pointed.res  < 0) {
                    fprintf(stderr, "%s: Req %d res %d\n",
                            __FUNCTION__, i + 1, cqe.pointed.res );
                    goto err;
                }
                break;
            2 -> 
                /* Should be timeout req_1 */
                if ( cqe.pointed.user_data  != 1) {
                    fprintf(stderr, "%s: unexpected timeout req %d sequece\n",
                            __FUNCTION__, i + 1);
                    goto err;
                }
                if ( cqe.pointed.res  != -ETIME) {
                    fprintf(stderr, "%s: Req %d timeout: %s\n",
                            __FUNCTION__, i + 1, strerror( cqe.pointed.res ));
                    goto err;
                }
                break;
        }
        io_uring_cqe_seen(ring, cqe);
    }

    return 0;
    err:
    return 1;
}

/*
 * Test timeout <link> timeout <drain> timeout
 */
static test_timeout_flags1:Int(ring:CPointer<io_uring>) {
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    ts:__kernel_timespec;
    ret:Int, i;

    msec_to_ts(ts.ptr, TIMEOUT_MSEC);

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "%s: get sqe failed\n", __FUNCTION__);
        goto err;
    }
    io_uring_prep_timeout(sqe, ts.ptr, 0, 0);
 sqe.pointed.user_data  = 1;
 sqe.pointed.flags  |= IOSQE_IO_LINK;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "%s: get sqe failed\n", __FUNCTION__);
        goto err;
    }
    io_uring_prep_timeout(sqe, ts.ptr, 0, 0);
 sqe.pointed.user_data  = 2;
 sqe.pointed.flags  |= IOSQE_IO_DRAIN;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "%s: get sqe failed\n", __FUNCTION__);
        goto err;
    }
    io_uring_prep_timeout(sqe, ts.ptr, 0, 0);
 sqe.pointed.user_data  = 3;

    ret = io_uring_submit(ring);
    if (ret <= 0) {
        fprintf(stderr, "%s: sqe submit failed: %d\n", __FUNCTION__, ret);
        goto err;
    }

    for (i = 0; i < 3; i++) {
        ret = io_uring_wait_cqe(ring, cqe.ptr);
        if (ret < 0) {
            fprintf(stderr, "%s: wait completion %d\n", __FUNCTION__, ret);
            goto err;
        }

        if ( cqe.pointed.res  == -EINVAL) {
            if (!i)
                fprintf(stdout, "%s: timeout flags not supported\n",
                        __FUNCTION__);
            io_uring_cqe_seen(ring, cqe);
            continue;
        }

        when  ( cqe.pointed.user_data )  {
            1 -> 
                if ( cqe.pointed.res  != -ETIME) {
                    fprintf(stderr, "%s: got %d, wanted %d\n",
                            __FUNCTION__, cqe.pointed.res , -ETIME);
                    goto err;
                }
                break;
            2 -> 
                if ( cqe.pointed.res  != -ECANCELED) {
                    fprintf(stderr, "%s: got %d, wanted %d\n",
                            __FUNCTION__, cqe.pointed.res ,
                            -ECANCELED);
                    goto err;
                }
                break;
            3 -> 
                if ( cqe.pointed.res  != -ETIME) {
                    fprintf(stderr, "%s: got %d, wanted %d\n",
                            __FUNCTION__, cqe.pointed.res , -ETIME);
                    goto err;
                }
                break;
        }
        io_uring_cqe_seen(ring, cqe);
    }

    return 0;
    err:
    return 1;
}

/*
 * Test timeout <link> timeout <link> timeout
 */
static test_timeout_flags2:Int(ring:CPointer<io_uring>) {
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    ts:__kernel_timespec;
    ret:Int, i;

    msec_to_ts(ts.ptr, TIMEOUT_MSEC);

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "%s: get sqe failed\n", __FUNCTION__);
        goto err;
    }
    io_uring_prep_timeout(sqe, ts.ptr, 0, 0);
 sqe.pointed.user_data  = 1;
 sqe.pointed.flags  |= IOSQE_IO_LINK;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "%s: get sqe failed\n", __FUNCTION__);
        goto err;
    }
    io_uring_prep_timeout(sqe, ts.ptr, 0, 0);
 sqe.pointed.user_data  = 2;
 sqe.pointed.flags  |= IOSQE_IO_LINK;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "%s: get sqe failed\n", __FUNCTION__);
        goto err;
    }
    io_uring_prep_timeout(sqe, ts.ptr, 0, 0);
 sqe.pointed.user_data  = 3;

    ret = io_uring_submit(ring);
    if (ret <= 0) {
        fprintf(stderr, "%s: sqe submit failed: %d\n", __FUNCTION__, ret);
        goto err;
    }

    for (i = 0; i < 3; i++) {
        ret = io_uring_wait_cqe(ring, cqe.ptr);
        if (ret < 0) {
            fprintf(stderr, "%s: wait completion %d\n", __FUNCTION__, ret);
            goto err;
        }

        if ( cqe.pointed.res  == -EINVAL) {
            if (!i)
                fprintf(stdout, "%s: timeout flags not supported\n",
                        __FUNCTION__);
            io_uring_cqe_seen(ring, cqe);
            continue;
        }

        when  ( cqe.pointed.user_data )  {
            1 -> 
                if ( cqe.pointed.res  != -ETIME) {
                    fprintf(stderr, "%s: got %d, wanted %d\n",
                            __FUNCTION__, cqe.pointed.res , -ETIME);
                    goto err;
                }
                break;
            2 -> 
            3 -> 
                if ( cqe.pointed.res  != -ECANCELED) {
                    fprintf(stderr, "%s: got %d, wanted %d\n",
                            __FUNCTION__, cqe.pointed.res ,
                            -ECANCELED);
                    goto err;
                }
                break;
        }
        io_uring_cqe_seen(ring, cqe);
    }

    return 0;
    err:
    return 1;
}

/*
 * Test timeout <drain> timeout <link> timeout
 */
static test_timeout_flags3:Int(ring:CPointer<io_uring>) {
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    ts:__kernel_timespec;
    ret:Int, i;

    msec_to_ts(ts.ptr, TIMEOUT_MSEC);

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "%s: get sqe failed\n", __FUNCTION__);
        goto err;
    }
    io_uring_prep_timeout(sqe, ts.ptr, 0, 0);
 sqe.pointed.user_data  = 1;
 sqe.pointed.flags  |= IOSQE_IO_DRAIN;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "%s: get sqe failed\n", __FUNCTION__);
        goto err;
    }
    io_uring_prep_timeout(sqe, ts.ptr, 0, 0);
 sqe.pointed.user_data  = 2;
 sqe.pointed.flags  |= IOSQE_IO_LINK;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "%s: get sqe failed\n", __FUNCTION__);
        goto err;
    }
    io_uring_prep_timeout(sqe, ts.ptr, 0, 0);
 sqe.pointed.user_data  = 3;

    ret = io_uring_submit(ring);
    if (ret <= 0) {
        fprintf(stderr, "%s: sqe submit failed: %d\n", __FUNCTION__, ret);
        goto err;
    }

    for (i = 0; i < 3; i++) {
        ret = io_uring_wait_cqe(ring, cqe.ptr);
        if (ret < 0) {
            fprintf(stderr, "%s: wait completion %d\n", __FUNCTION__, ret);
            goto err;
        }

        if ( cqe.pointed.res  == -EINVAL) {
            if (!i)
                fprintf(stdout, "%s: timeout flags not supported\n",
                        __FUNCTION__);
            io_uring_cqe_seen(ring, cqe);
            continue;
        }

        when  ( cqe.pointed.user_data )  {
            1 -> 
            2 -> 
                if ( cqe.pointed.res  != -ETIME) {
                    fprintf(stderr, "%s: got %d, wanted %d\n",
                            __FUNCTION__, cqe.pointed.res , -ETIME);
                    goto err;
                }
                break;
            3 -> 
                if ( cqe.pointed.res  != -ECANCELED) {
                    fprintf(stderr, "%s: got %d, wanted %d\n",
                            __FUNCTION__, cqe.pointed.res ,
                            -ECANCELED);
                    goto err;
                }
                break;
        }
        io_uring_cqe_seen(ring, cqe);
    }

    return 0;
    err:
    return 1;
}

static test_update_timeout:Int(ring:CPointer<io_uring>,long :ULongms
                               bool abs, bool async, bool linked) {
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    ts:__kernel_timespec, ts_upd;
long :ULongexp_ms base_ms = 10000;
    tv:timeval;
    ret:Int, i, nr = 2;
    __u32 mode = abs ? IORING_TIMEOUT_ABS : 0;

    msec_to_ts(ts_upd.ptr, ms);
    gettimeofday(tv.ptr, NULL);

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "%s: get sqe failed\n", __FUNCTION__);
        goto err;
    }
    msec_to_ts(ts.ptr, base_ms);
    io_uring_prep_timeout(sqe, ts.ptr, 0, 0);
 sqe.pointed.user_data  = 1;

    if (linked) {
        sqe = io_uring_get_sqe(ring);
        if (!sqe) {
            fprintf(stderr, "%s: get sqe failed\n", __FUNCTION__);
            goto err;
        }
        io_uring_prep_nop(sqe);
 sqe.pointed.user_data  = 3;
 sqe.pointed.flags  = IOSQE_IO_LINK;
        if (async)
 sqe.pointed.flags  |= IOSQE_ASYNC;
        nr++;
    }

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "%s: get sqe failed\n", __FUNCTION__);
        goto err;
    }
    io_uring_prep_timeout_update(sqe, ts_upd.ptr, 1, mode);
 sqe.pointed.user_data  = 2;
    if (async)
 sqe.pointed.flags  |= IOSQE_ASYNC;

    ret = io_uring_submit(ring);
    if (ret != nr) {
        fprintf(stderr, "%s: sqe submit failed: %d\n", __FUNCTION__, ret);
        goto err;
    }

    for (i = 0; i < nr; i++) {
        ret = io_uring_wait_cqe(ring, cqe.ptr);
        if (ret < 0) {
            fprintf(stderr, "%s: wait completion %d\n", __FUNCTION__, ret);
            goto err;
        }

        when  ( cqe.pointed.user_data )  {
            1 -> 
                if ( cqe.pointed.res  != -ETIME) {
                    fprintf(stderr, "%s: got %d, wanted %d\n",
                            __FUNCTION__, cqe.pointed.res , -ETIME);
                    goto err;
                }
                break;
            2 -> 
                if ( cqe.pointed.res  != 0) {
                    fprintf(stderr, "%s: got %d, wanted %d\n",
                            __FUNCTION__, cqe.pointed.res ,
                            0);
                    goto err;
                }
                break;
            3 -> 
                if ( cqe.pointed.res  != 0) {
                    fprintf(stderr, "nop failed\n");
                    goto err;
                }
                break;
            default:
                goto err;
        }
        io_uring_cqe_seen(ring, cqe);
    }

    exp_ms = mtime_since_now(tv.ptr);
    if (exp_ms >= base_ms / 2) {
        fprintf(stderr, "too long, timeout wasn't updated\n");
        goto err;
    }
    if (ms >= 1000 && !abs && exp_ms < ms / 2) {
        fprintf(stderr, "fired too early, potentially updated to 0 ms"
                        "instead of %lu\n", ms);
        goto err;
    }
    return 0;
    err:
    return 1;
}

static test_update_nonexistent_timeout:Int(ring:CPointer<io_uring>) {
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    ts:__kernel_timespec;
    ret:Int;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "%s: get sqe failed\n", __FUNCTION__);
        goto err;
    }
    msec_to_ts(ts.ptr, 0);
    io_uring_prep_timeout_update(sqe, ts.ptr, 42, 0);

    ret = io_uring_submit(ring);
    if (ret != 1) {
        fprintf(stderr, "%s: sqe submit failed: %d\n", __FUNCTION__, ret);
        goto err;
    }

    ret = io_uring_wait_cqe(ring, cqe.ptr);
    if (ret < 0) {
        fprintf(stderr, "%s: wait completion %d\n", __FUNCTION__, ret);
        goto err;
    }

    ret = cqe.pointed.res ;
    if (ret == -ENOENT)
        ret = 0;
    io_uring_cqe_seen(ring, cqe);
    return ret;
    err:
    return 1;
}

static test_update_invalid_flags:Int(ring:CPointer<io_uring>) {
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    ts:__kernel_timespec;
    ret:Int;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "%s: get sqe failed\n", __FUNCTION__);
        goto err;
    }
    io_uring_prep_timeout_remove(sqe, 0, IORING_TIMEOUT_ABS);

    ret = io_uring_submit(ring);
    if (ret != 1) {
        fprintf(stderr, "%s: sqe submit failed: %d\n", __FUNCTION__, ret);
        goto err;
    }

    ret = io_uring_wait_cqe(ring, cqe.ptr);
    if (ret < 0) {
        fprintf(stderr, "%s: wait completion %d\n", __FUNCTION__, ret);
        goto err;
    }
    if ( cqe.pointed.res  != -EINVAL) {
        fprintf(stderr, "%s: got %d, wanted %d\n",
                __FUNCTION__, cqe.pointed.res , -EINVAL);
        goto err;
    }
    io_uring_cqe_seen(ring, cqe);


    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "%s: get sqe failed\n", __FUNCTION__);
        goto err;
    }
    msec_to_ts(ts.ptr, 0);
    io_uring_prep_timeout_update(sqe, ts.ptr, 0, -1);

    ret = io_uring_submit(ring);
    if (ret != 1) {
        fprintf(stderr, "%s: sqe submit failed: %d\n", __FUNCTION__, ret);
        goto err;
    }

    ret = io_uring_wait_cqe(ring, cqe.ptr);
    if (ret < 0) {
        fprintf(stderr, "%s: wait completion %d\n", __FUNCTION__, ret);
        goto err;
    }
    if ( cqe.pointed.res  != -EINVAL) {
        fprintf(stderr, "%s: got %d, wanted %d\n",
                __FUNCTION__, cqe.pointed.res , -EINVAL);
        goto err;
    }
    io_uring_cqe_seen(ring, cqe);

    return 0;
    err:
    return 1;
}

static fill_exec_target:Int(dst:CPointer<ByteVar>, char *path) {
    sb:stat;

    /*
     * Should either be ./exec-target or test/exec-target
     */
    sprintf(dst, "%s", path);
    return stat(dst, sb.ptr);
}

static test_timeout_link_cancel:Int(void) {
    ring:io_uring;
    cqe:CPointer<io_uring_cqe>;
    char prog_path[PATH_MAX];
    p:pid_t;
    ret:Int, i, wstatus;

    if (fill_exec_target(prog_path, "./exec-target") &&
fun arget(prog_path, "test/exec-target")):fill_exec_t{
        fprintf(stdout, "Can't find exec-target, skipping\n");
        return 0;
    }

    ret = io_uring_queue_init(8, ring.ptr, 0);
    if (ret) {
        fprintf(stderr, "ring create failed: %d\n", ret);
        return 1;
    }

    p = fork();
    if (p == -1) {
        fprintf(stderr, "fork() failed\n");
        return 1;
    }

    if (p == 0) {
        sqe:CPointer<io_uring_sqe>;
        ts:__kernel_timespec;

        msec_to_ts(ts.ptr, 10000);
        sqe = io_uring_get_sqe(ring.ptr);
        io_uring_prep_timeout(sqe, ts.ptr, 0, 0);
 sqe.pointed.flags  |= IOSQE_IO_LINK;
 sqe.pointed.user_data  = 0;

        sqe = io_uring_get_sqe(ring.ptr);
        io_uring_prep_nop(sqe);
 sqe.pointed.user_data  = 1;

        ret = io_uring_submit(ring.ptr);
        if (ret != 2) {
            fprintf(stderr, "%s: got %d, wanted 1\n", __FUNCTION__, ret);
            exit(1);
        }

        /* trigger full cancellation */
        ret = execl(prog_path, prog_path, NULL);
        if (ret) {
            fprintf(stderr, "exec failed %i\n", errno);
            exit(1);
        }
        exit(0);
    }

    if (waitpid(p, wstatus.ptr, 0) == (pid_t) -1) {
        perror("waitpid()");
        return 1;
    }
    if (!WIFEXITED(wstatus) || WEXITSTATUS(wstatus)) {
        fprintf(stderr, "child failed %i\n", WEXITSTATUS(wstatus));
        return 1;
    }

    for (i = 0; i < 2; ++i) {
        ret = io_uring_wait_cqe(ring.ptr, cqe.ptr);
        if (ret) {
            fprintf(stderr, "wait_cqe=%d\n", ret);
            return 1;
        }
        if ( cqe.pointed.res  != -ECANCELED) {
            fprintf(stderr, "invalid result, user_data: %i res: %i\n",
                    (int) cqe.pointed.user_data , cqe.pointed.res );
            return 1;
        }
        io_uring_cqe_seen(ring.ptr, cqe);
    }

    io_uring_queue_exit(ring.ptr);
    return 0;
}


static test_not_failing_links:Int(void) {
    ring:io_uring;
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    ts:__kernel_timespec;
    ret:Int;

    ret = io_uring_queue_init(8, ring.ptr, 0);
    if (ret) {
        fprintf(stderr, "ring create failed: %d\n", ret);
        return 1;
    }

    msec_to_ts(ts.ptr, 1);
    sqe = io_uring_get_sqe(ring.ptr);
    io_uring_prep_timeout(sqe, ts.ptr, 0, IORING_TIMEOUT_ETIME_SUCCESS);
 sqe.pointed.user_data  = 1;
 sqe.pointed.flags  |= IOSQE_IO_LINK;

    sqe = io_uring_get_sqe(ring.ptr);
    io_uring_prep_nop(sqe);
 sqe.pointed.user_data  = 2;

    ret = io_uring_submit(ring.ptr);
    if (ret != 2) {
        fprintf(stderr, "%s: sqe submit failed: %d\n", __FUNCTION__, ret);
        return 1;
    }

    ret = io_uring_wait_cqe(ring.ptr, cqe.ptr);
    if (ret < 0) {
        fprintf(stderr, "%s: wait completion %d\n", __FUNCTION__, ret);
        return 1;
    } else if ( cqe.pointed.user_data  == 1 && cqe.pointed.res  == -EINVAL) {
        fprintf(stderr, "ETIME_SUCCESS is not supported, skip\n");
        goto done;
    } else if ( cqe.pointed.res  != -ETIME || cqe.pointed.user_data  != 1) {
        fprintf(stderr, "timeout failed %i %i\n", cqe.pointed.res ,
                (int) cqe.pointed.user_data );
        return 1;
    }
    io_uring_cqe_seen(ring.ptr, cqe);

    ret = io_uring_wait_cqe(ring.ptr, cqe.ptr);
    if (ret < 0) {
        fprintf(stderr, "%s: wait completion %d\n", __FUNCTION__, ret);
        return 1;
    } else if ( cqe.pointed.res  || cqe.pointed.user_data  != 2) {
        fprintf(stderr, "nop failed %i %i\n", cqe.pointed.res ,
                (int) cqe.pointed.user_data );
        return 1;
    }
    done:
    io_uring_cqe_seen(ring.ptr, cqe);
    io_uring_queue_exit(ring.ptr);
    return 0;
}


fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    ring:io_uring, sqpoll_ring;
    bool has_timeout_update, sqpoll;
    p:io_uring_params = {};
    ret:Int;

    if (argc > 1)
        return 0;

    ret = io_uring_queue_init_params(8, ring.ptr, p.ptr);
    if (ret) {
        fprintf(stderr, "ring setup failed\n");
        return 1;
    }

    ret = io_uring_queue_init(8, sqpoll_ring.ptr, IORING_SETUP_SQPOLL);
    sqpoll = !ret;

    ret = test_single_timeout(ring.ptr);
    if (ret) {
        fprintf(stderr, "test_single_timeout failed\n");
        return ret;
    }
    if (not_supported)
        return 0;

    ret = test_multi_timeout(ring.ptr);
    if (ret) {
        fprintf(stderr, "test_multi_timeout failed\n");
        return ret;
    }

    ret = test_single_timeout_abs(ring.ptr);
    if (ret) {
        fprintf(stderr, "test_single_timeout_abs failed\n");
        return ret;
    }

    ret = test_single_timeout_remove(ring.ptr);
    if (ret) {
        fprintf(stderr, "test_single_timeout_remove failed\n");
        return ret;
    }

    ret = test_single_timeout_remove_notfound(ring.ptr);
    if (ret) {
        fprintf(stderr, "test_single_timeout_remove_notfound failed\n");
        return ret;
    }

    ret = test_single_timeout_many(ring.ptr);
    if (ret) {
        fprintf(stderr, "test_single_timeout_many failed\n");
        return ret;
    }

    ret = test_single_timeout_nr(ring.ptr, 1);
    if (ret) {
        fprintf(stderr, "test_single_timeout_nr(1) failed\n");
        return ret;
    }
    ret = test_single_timeout_nr(ring.ptr, 2);
    if (ret) {
        fprintf(stderr, "test_single_timeout_nr(2) failed\n");
        return ret;
    }

    ret = test_multi_timeout_nr(ring.ptr);
    if (ret) {
        fprintf(stderr, "test_multi_timeout_nr failed\n");
        return ret;
    }

    ret = test_timeout_flags1(ring.ptr);
    if (ret) {
        fprintf(stderr, "test_timeout_flags1 failed\n");
        return ret;
    }

    ret = test_timeout_flags2(ring.ptr);
    if (ret) {
        fprintf(stderr, "test_timeout_flags2 failed\n");
        return ret;
    }

    ret = test_timeout_flags3(ring.ptr);
    if (ret) {
        fprintf(stderr, "test_timeout_flags3 failed\n");
        return ret;
    }

    ret = test_single_timeout_wait(ring.ptr, p.ptr);
    if (ret) {
        fprintf(stderr, "test_single_timeout_wait failed\n");
        return ret;
    }

    /* io_uring_wait_cqes() may have left a timeout, reinit ring */
    io_uring_queue_exit(ring.ptr);
    ret = io_uring_queue_init(8, ring.ptr, 0);
    if (ret) {
        fprintf(stderr, "ring setup failed\n");
        return 1;
    }

    ret = test_update_nonexistent_timeout(ring.ptr);
    has_timeout_update = (ret != -EINVAL);
    if (has_timeout_update) {
        if (ret) {
            fprintf(stderr, "test_update_nonexistent_timeout failed\n");
            return ret;
        }

        ret = test_update_invalid_flags(ring.ptr);
        if (ret) {
            fprintf(stderr, "test_update_invalid_flags failed\n");
            return ret;
        }

        ret = test_update_timeout(ring.ptr, 0, false, false, false);
        if (ret) {
            fprintf(stderr, "test_update_timeout failed\n");
            return ret;
        }

        ret = test_update_timeout(ring.ptr, 1, false, false, false);
        if (ret) {
            fprintf(stderr, "test_update_timeout 1ms failed\n");
            return ret;
        }

        ret = test_update_timeout(ring.ptr, 1000, false, false, false);
        if (ret) {
            fprintf(stderr, "test_update_timeout 1s failed\n");
            return ret;
        }

        ret = test_update_timeout(ring.ptr, 0, true, true, false);
        if (ret) {
            fprintf(stderr, "test_update_timeout abs failed\n");
            return ret;
        }


        ret = test_update_timeout(ring.ptr, 0, false, true, false);
        if (ret) {
            fprintf(stderr, "test_update_timeout async failed\n");
            return ret;
        }

        ret = test_update_timeout(ring.ptr, 0, false, false, true);
        if (ret) {
            fprintf(stderr, "test_update_timeout linked failed\n");
            return ret;
        }

        if (sqpoll) {
            ret = test_update_timeout(sqpoll_ring.ptr, 0, false, false,
                                      false);
            if (ret) {
                fprintf(stderr, "test_update_timeout sqpoll"
                                "failed\n");
                return ret;
            }
        }
    }

    /*
     * this test must go last, it kills the ring
     */
    ret = test_single_timeout_exit(ring.ptr);
    if (ret) {
        fprintf(stderr, "test_single_timeout_exit failed\n");
        return ret;
    }

    ret = test_timeout_link_cancel();
    if (ret) {
        fprintf(stderr, "test_timeout_link_cancel failed\n");
        return ret;
    }

    ret = test_not_failing_links();
    if (ret) {
        fprintf(stderr, "test_not_failing_links failed\n");
        return ret;
    }

    if (sqpoll)
        io_uring_queue_exit(sqpoll_ring.ptr);
    return 0;
}
