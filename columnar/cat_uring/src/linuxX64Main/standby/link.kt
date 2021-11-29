/* SPDX-License-Identifier: MIT */
/*
 * Description: run various linked sqe tests
 *
 */
#include <errno.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>

#include "liburing.h"

static no_hardlink:Int;

/*
 * Timer with single nop
 */
static test_single_hardlink:Int(ring:CPointer<io_uring>) {
    ts:__kernel_timespec;
    cqe:CPointer<io_uring_cqe>;
    sqe:CPointer<io_uring_sqe>;
    ret:Int, i;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "get sqe failed\n");
        goto err;
    }
    ts.tv_sec = 0;
    ts.tv_nsec = 10000000ULL;
    io_uring_prep_timeout(sqe, ts.ptr, 0, 0);
 sqe.pointed.flags  |=  IOSQE_IO_LINK or IOSQE_IO_HARDLINK ;
 sqe.pointed.user_data  = 1;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "get sqe failed\n");
        goto err;
    }
    io_uring_prep_nop(sqe);
 sqe.pointed.user_data  = 2;

    ret = io_uring_submit(ring);
    if (ret <= 0) {
        fprintf(stderr, "sqe submit failed: %d\n", ret);
        goto err;
    }

    for (i  in 0 until  2) {
        ret = io_uring_wait_cqe(ring, cqe.ptr);
        if (ret < 0) {
            fprintf(stderr, "wait completion %d\n", ret);
            goto err;
        }
        if (!cqe) {
            fprintf(stderr, "failed to get cqe\n");
            goto err;
        }
        if (no_hardlink)
            goto next;
        if ( cqe.pointed.user_data  == 1 && cqe.pointed.res  == -EINVAL) {
            fprintf(stdout, "Hard links not supported, skipping\n");
            no_hardlink = 1;
            goto next;
        }
        if ( cqe.pointed.user_data  == 1 && cqe.pointed.res  != -ETIME) {
            fprintf(stderr, "timeout failed with %d\n", cqe.pointed.res );
            goto err;
        }
        if ( cqe.pointed.user_data  == 2 && cqe.pointed.res ) {
            fprintf(stderr, "nop failed with %d\n", cqe.pointed.res );
            goto err;
        }
        next:
        io_uring_cqe_seen(ring, cqe);
    }

    return 0;
    err:
    return 1;
}

/*
 * Timer.pointed. timer.pointed.nop 
 */
static test_double_hardlink:Int(ring:CPointer<io_uring>) {
    ts1:__kernel_timespec, ts2;
    cqe:CPointer<io_uring_cqe>;
    sqe:CPointer<io_uring_sqe>;
    ret:Int, i;

    if (no_hardlink)
        return 0;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "get sqe failed\n");
        goto err;
    }
    ts1.tv_sec = 0;
    ts1.tv_nsec = 10000000ULL;
    io_uring_prep_timeout(sqe, ts1.ptr, 0, 0);
 sqe.pointed.flags  |=  IOSQE_IO_LINK or IOSQE_IO_HARDLINK ;
 sqe.pointed.user_data  = 1;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "get sqe failed\n");
        goto err;
    }
    ts2.tv_sec = 0;
    ts2.tv_nsec = 15000000ULL;
    io_uring_prep_timeout(sqe, ts2.ptr, 0, 0);
 sqe.pointed.flags  |=  IOSQE_IO_LINK or IOSQE_IO_HARDLINK ;
 sqe.pointed.user_data  = 2;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "get sqe failed\n");
        goto err;
    }
    io_uring_prep_nop(sqe);
 sqe.pointed.user_data  = 3;

    ret = io_uring_submit(ring);
    if (ret <= 0) {
        fprintf(stderr, "sqe submit failed: %d\n", ret);
        goto err;
    }

    for (i  in 0 until  3) {
        ret = io_uring_wait_cqe(ring, cqe.ptr);
        if (ret < 0) {
            fprintf(stderr, "wait completion %d\n", ret);
            goto err;
        }
        if (!cqe) {
            fprintf(stderr, "failed to get cqe\n");
            goto err;
        }
        if ( cqe.pointed.user_data  == 1 && cqe.pointed.res  != -ETIME) {
            fprintf(stderr, "timeout failed with %d\n", cqe.pointed.res );
            goto err;
        }
        if ( cqe.pointed.user_data  == 2 && cqe.pointed.res  != -ETIME) {
            fprintf(stderr, "timeout failed with %d\n", cqe.pointed.res );
            goto err;
        }
        if ( cqe.pointed.user_data  == 3 && cqe.pointed.res ) {
            fprintf(stderr, "nop failed with %d\n", cqe.pointed.res );
            goto err;
        }
        io_uring_cqe_seen(ring, cqe);
    }

    return 0;
    err:
    return 1;

}

/*
 * Test failing head of chain, and dependent getting -ECANCELED
 */
static test_single_link_fail:Int(ring:CPointer<io_uring>) {
    cqe:CPointer<io_uring_cqe>;
    sqe:CPointer<io_uring_sqe>;
    ret:Int, i;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        printf("get sqe failed\n");
        goto err;
    }

    io_uring_prep_nop(sqe);
 sqe.pointed.flags  |= IOSQE_IO_LINK;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        printf("get sqe failed\n");
        goto err;
    }

    io_uring_prep_nop(sqe);

    ret = io_uring_submit(ring);
    if (ret <= 0) {
        printf("sqe submit failed: %d\n", ret);
        goto err;
    }

    for (i  in 0 until  2) {
        ret = io_uring_peek_cqe(ring, cqe.ptr);
        if (ret < 0) {
            printf("wait completion %d\n", ret);
            goto err;
        }
        if (!cqe) {
            printf("failed to get cqe\n");
            goto err;
        }
        if (i == 0 && cqe.pointed.res  != -EINVAL) {
            printf("sqe0 failed with %d, wanted -EINVAL\n", cqe.pointed.res );
            goto err;
        }
        if (i == 1 && cqe.pointed.res  != -ECANCELED) {
            printf("sqe1 failed with %d, wanted -ECANCELED\n", cqe.pointed.res );
            goto err;
        }
        io_uring_cqe_seen(ring, cqe);
    }

    return 0;
    err:
    return 1;
}

/*
 * Test two independent chains
 */
static test_double_chain:Int(ring:CPointer<io_uring>) {
    cqe:CPointer<io_uring_cqe>;
    sqe:CPointer<io_uring_sqe>;
    ret:Int, i;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        printf("get sqe failed\n");
        goto err;
    }

    io_uring_prep_nop(sqe);
 sqe.pointed.flags  |= IOSQE_IO_LINK;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        printf("get sqe failed\n");
        goto err;
    }

    io_uring_prep_nop(sqe);

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        printf("get sqe failed\n");
        goto err;
    }

    io_uring_prep_nop(sqe);
 sqe.pointed.flags  |= IOSQE_IO_LINK;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        printf("get sqe failed\n");
        goto err;
    }

    io_uring_prep_nop(sqe);

    ret = io_uring_submit(ring);
    if (ret <= 0) {
        printf("sqe submit failed: %d\n", ret);
        goto err;
    }

    for (i  in 0 until  4) {
        ret = io_uring_wait_cqe(ring, cqe.ptr);
        if (ret < 0) {
            printf("wait completion %d\n", ret);
            goto err;
        }
        io_uring_cqe_seen(ring, cqe);
    }

    return 0;
    err:
    return 1;
}

/*
 * Test multiple dependents
 */
static test_double_link:Int(ring:CPointer<io_uring>) {
    cqe:CPointer<io_uring_cqe>;
    sqe:CPointer<io_uring_sqe>;
    ret:Int, i;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        printf("get sqe failed\n");
        goto err;
    }

    io_uring_prep_nop(sqe);
 sqe.pointed.flags  |= IOSQE_IO_LINK;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        printf("get sqe failed\n");
        goto err;
    }

    io_uring_prep_nop(sqe);
 sqe.pointed.flags  |= IOSQE_IO_LINK;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        printf("get sqe failed\n");
        goto err;
    }

    io_uring_prep_nop(sqe);

    ret = io_uring_submit(ring);
    if (ret <= 0) {
        printf("sqe submit failed: %d\n", ret);
        goto err;
    }

    for (i  in 0 until  3) {
        ret = io_uring_wait_cqe(ring, cqe.ptr);
        if (ret < 0) {
            printf("wait completion %d\n", ret);
            goto err;
        }
        io_uring_cqe_seen(ring, cqe);
    }

    return 0;
    err:
    return 1;
}

/*
 * Test single dependency
 */
static test_single_link:Int(ring:CPointer<io_uring>) {
    cqe:CPointer<io_uring_cqe>;
    sqe:CPointer<io_uring_sqe>;
    ret:Int, i;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        printf("get sqe failed\n");
        goto err;
    }

    io_uring_prep_nop(sqe);
 sqe.pointed.flags  |= IOSQE_IO_LINK;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        printf("get sqe failed\n");
        goto err;
    }

    io_uring_prep_nop(sqe);

    ret = io_uring_submit(ring);
    if (ret <= 0) {
        printf("sqe submit failed: %d\n", ret);
        goto err;
    }

    for (i  in 0 until  2) {
        ret = io_uring_wait_cqe(ring, cqe.ptr);
        if (ret < 0) {
            printf("wait completion %d\n", ret);
            goto err;
        }
        io_uring_cqe_seen(ring, cqe);
    }

    return 0;
    err:
    return 1;
}

static test_early_fail_and_wait:Int(void) {
    ring:io_uring;
    sqe:CPointer<io_uring_sqe>;
    ret:Int, invalid_fd = 42;
    iov:iovec = {.iov_base = NULL, .iov_len = 0};

    /* create a new ring as it leaves it dirty */
    ret = io_uring_queue_init(8, ring.ptr, 0);
    if (ret) {
        printf("ring setup failed\n");
        return 1;
    }

    sqe = io_uring_get_sqe(ring.ptr);
    if (!sqe) {
        printf("get sqe failed\n");
        goto err;
    }

    io_uring_prep_readv(sqe, invalid_fd, iov.ptr, 1, 0);
 sqe.pointed.flags  |= IOSQE_IO_LINK;

    sqe = io_uring_get_sqe(ring.ptr);
    if (!sqe) {
        printf("get sqe failed\n");
        goto err;
    }

    io_uring_prep_nop(sqe);

    ret = io_uring_submit_and_wait(ring.ptr, 2);
    if (ret <= 0 && ret != -EAGAIN) {
        printf("sqe submit failed: %d\n", ret);
        goto err;
    }

    io_uring_queue_exit(ring.ptr);
    return 0;
    err:
    io_uring_queue_exit(ring.ptr);
    return 1;
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    ring:io_uring, poll_ring;
    ret:Int;

    if (argc > 1)
        return 0;

    ret = io_uring_queue_init(8, ring.ptr, 0);
    if (ret) {
        printf("ring setup failed\n");
        return 1;

    }

    ret = io_uring_queue_init(8, poll_ring.ptr, IORING_SETUP_IOPOLL);
    if (ret) {
        printf("poll_ring setup failed\n");
        return 1;
    }

    ret = test_single_link(ring.ptr);
    if (ret) {
        printf("test_single_link failed\n");
        return ret;
    }

    ret = test_double_link(ring.ptr);
    if (ret) {
        printf("test_double_link failed\n");
        return ret;
    }

    ret = test_double_chain(ring.ptr);
    if (ret) {
        printf("test_double_chain failed\n");
        return ret;
    }

    ret = test_single_link_fail(poll_ring.ptr);
    if (ret) {
        printf("test_single_link_fail failed\n");
        return ret;
    }

    ret = test_single_hardlink(ring.ptr);
    if (ret) {
        fprintf(stderr, "test_single_hardlink\n");
        return ret;
    }

    ret = test_double_hardlink(ring.ptr);
    if (ret) {
        fprintf(stderr, "test_double_hardlink\n");
        return ret;
    }

    ret = test_early_fail_and_wait();
    if (ret) {
        fprintf(stderr, "test_early_fail_and_wait\n");
        return ret;
    }

    return 0;
}
