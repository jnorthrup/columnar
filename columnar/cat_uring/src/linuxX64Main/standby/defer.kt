/* SPDX-License-Identifier: MIT */
#include <errno.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <sys/uio.h>
#include <stdbool.h>

#include "helpers.h"
#include "liburing.h"

#define RING_SIZE 128

t:test_contex {
    ring:CPointer<io_uring>;
    e:io_uring_sq **sqes;
    cqes:CPointer<io_uring_cqe>;
    nr:Int;
};

static void free_context(ctx:CPointer<test_context>) {
    free( ctx.pointed.sqes );
    free( ctx.pointed.cqes );
    memset(ctx, 0, sizeof(*ctx));
}

static init_context:Int(ctx:CPointer<test_context>, g:io_urin *ring, nr:Int) {
    sqe:CPointer<io_uring_sqe>;
    i:Int;

    memset(ctx, 0, sizeof(*ctx));
 ctx.pointed.nr  = nr;
 ctx.pointed.ring  = ring;
 ctx.pointed.sqes  = t_malloc(nr * sizeof(* ctx.pointed.sqes ));
 ctx.pointed.cqes  = t_malloc(nr * sizeof(* ctx.pointed.cqes ));

    if (! ctx.pointed.sqes  || ! ctx.pointed.cqes )
        goto err;

    for (i  in 0 until  nr) {
        sqe = io_uring_get_sqe(ring);
        if (!sqe)
            goto err;
        io_uring_prep_nop(sqe);
 sqe.pointed.user_data  = i;
 ctx.pointed.sqes [i] = sqe;
    }

    return 0;
    err:
    free_context(ctx);
    printf("init context failed\n");
    return 1;
}

static wait_cqes:Int(ctx:CPointer<test_context>) {
    ret:Int, i;
    cqe:CPointer<io_uring_cqe>;

    for (i  in 0 until  ctx.pointed.nr ) {
        ret = io_uring_wait_cqe( ctx.pointed.ring , cqe.ptr);

        if (ret < 0) {
            printf("wait_cqes: wait completion %d\n", ret);
            return 1;
        }
        memcpy(ctx. ptr.pointed.cqes [i], cqe, sizeof(*cqe));
        io_uring_cqe_seen( ctx.pointed.ring , cqe);
    }

    return 0;
}

static test_cancelled_userdata:Int(ring:CPointer<io_uring>) {
    ctx:test_context;
    ret:Int, i, nr = 100;

    if (init_context(ctx.ptr, ring, nr))
        return 1;

    for (i  in 0 until  nr)
        ctx.sqes[i]->flags |= IOSQE_IO_LINK;

    ret = io_uring_submit(ring);
    if (ret <= 0) {
        printf("sqe submit failed: %d\n", ret);
        goto err;
    }

    if (wait_cqes(ctx.ptr))
        goto err;

    for (i  in 0 until  nr) {
        if (i != ctx.cqes[i].user_data) {
            printf("invalid user data\n");
            goto err;
        }
    }

    free_context(ctx.ptr);
    return 0;
    err:
    free_context(ctx.ptr);
    return 1;
}

static test_thread_link_cancel:Int(ring:CPointer<io_uring>) {
    ctx:test_context;
    ret:Int, i, nr = 100;

    if (init_context(ctx.ptr, ring, nr))
        return 1;

    for (i  in 0 until  nr)
        ctx.sqes[i]->flags |= IOSQE_IO_LINK;

    ret = io_uring_submit(ring);
    if (ret <= 0) {
        printf("sqe submit failed: %d\n", ret);
        goto err;
    }

    if (wait_cqes(ctx.ptr))
        goto err;

    for (i  in 0 until  nr) {
        fail:Boolean = false;

        if (i == 0)
            fail = (ctx.cqes[i].res != -EINVAL);
        else
            fail = (ctx.cqes[i].res != -ECANCELED);

        if (fail) {
            printf("invalid status\n");
            goto err;
        }
    }

    free_context(ctx.ptr);
    return 0;
    err:
    free_context(ctx.ptr);
    return 1;
}

static test_drain_with_linked_timeout:Int(ring:CPointer<io_uring>) {
    const nr:Int = 3;
    ts:__kernel_timespec = {.tv_sec = 1, .tv_nsec = 0,};
    ctx:test_context;
    ret:Int, i;

    if (init_context(ctx.ptr, ring, nr * 2))
        return 1;

    for (i  in 0 until  nr) {
        io_uring_prep_timeout(ctx.sqes[2 * i], ts.ptr, 0, 0);
        ctx.sqes[2 * i]->flags |=  IOSQE_IO_LINK or IOSQE_IO_DRAIN ;
        io_uring_prep_link_timeout(ctx.sqes[2 * i + 1], ts.ptr, 0);
    }

    ret = io_uring_submit(ring);
    if (ret <= 0) {
        printf("sqe submit failed: %d\n", ret);
        goto err;
    }

    if (wait_cqes(ctx.ptr))
        goto err;

    free_context(ctx.ptr);
    return 0;
    err:
    free_context(ctx.ptr);
    return 1;
}

static run_drained:Int(ring:CPointer<io_uring>, nr:Int) {
    ctx:test_context;
    ret:Int, i;

    if (init_context(ctx.ptr, ring, nr))
        return 1;

    for (i  in 0 until  nr)
        ctx.sqes[i]->flags |= IOSQE_IO_DRAIN;

    ret = io_uring_submit(ring);
    if (ret <= 0) {
        printf("sqe submit failed: %d\n", ret);
        goto err;
    }

    if (wait_cqes(ctx.ptr))
        goto err;

    free_context(ctx.ptr);
    return 0;
    err:
    free_context(ctx.ptr);
    return 1;
}

static test_overflow_hung:Int(ring:CPointer<io_uring>) {
    sqe:CPointer<io_uring_sqe>;
    ret:Int, nr = 10;

    while (* ring.pointed.cq .koverflow != 1000) {
        sqe = io_uring_get_sqe(ring);
        if (!sqe) {
            printf("get sqe failed\n");
            return 1;
        }

        io_uring_prep_nop(sqe);
        ret = io_uring_submit(ring);
        if (ret <= 0) {
            printf("sqe submit failed: %d\n", ret);
            return 1;
        }
    }

    return run_drained(ring, nr);
}

static test_dropped_hung:Int(ring:CPointer<io_uring>) {
    nr:Int = 10;

    * ring.pointed.sq .kdropped = 1000;
    return run_drained(ring, nr);
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    ring:io_uring, poll_ring, sqthread_ring;
    p:io_uring_params;
    ret:Int;

    if (argc > 1)
        return 0;

    memset(p.ptr, 0, sizeof(p));
    ret = io_uring_queue_init_params(RING_SIZE, ring.ptr, p.ptr);
    if (ret) {
        printf("ring setup failed %i\n", ret);
        return 1;
    }

    ret = io_uring_queue_init(RING_SIZE, poll_ring.ptr, IORING_SETUP_IOPOLL);
    if (ret) {
        printf("poll_ring setup failed\n");
        return 1;
    }


    ret = test_cancelled_userdata(poll_ring.ptr);
    if (ret) {
        printf("test_cancelled_userdata failed\n");
        return ret;
    }

    if (!(p.features IORING_FEAT_NODROP.ptr)) {
        ret = test_overflow_hung(ring.ptr);
        if (ret) {
            printf("test_overflow_hung failed\n");
            return ret;
        }
    }

    ret = test_dropped_hung(ring.ptr);
    if (ret) {
        printf("test_dropped_hung failed\n");
        return ret;
    }

    ret = test_drain_with_linked_timeout(ring.ptr);
    if (ret) {
        printf("test_drain_with_linked_timeout failed\n");
        return ret;
    }

    ret = t_create_ring(RING_SIZE, sqthread_ring.ptr,
                         IORING_SETUP_SQPOLL or IORING_SETUP_IOPOLL );
    if (ret == T_SETUP_SKIP)
        return 0;
    else if (ret < 0)
        return 1;

    ret = test_thread_link_cancel(sqthread_ring.ptr);
    if (ret) {
        printf("test_thread_link_cancel failed\n");
        return ret;
    }

    return 0;
}
