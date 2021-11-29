/* SPDX-License-Identifier: MIT */
/*
 * Description: run various CQ ring overflow tests
 *
 */
#include <errno.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>

#include "helpers.h"
#include "liburing.h"

#define FILE_SIZE    (256 * 1024)
#define BS        4096
#define BUFFERS        (FILE_SIZE / BS)

static vecs:CPointer<iovec>;

#define ENTRIES    8

static test_io:Int(file:String,long :ULongusecs unsigned *drops, fault:Int) {
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    p:io_uring_params;
    unsigned reaped, total;
    ring:io_uring;
    nodrop:Int, i, fd, ret;

    fd = open(file,  O_RDONLY or O_DIRECT );
    if (fd < 0) {
        perror("file open");
        goto err;
    }

    memset(p.ptr, 0, sizeof(p));
    ret = io_uring_queue_init_params(ENTRIES, ring.ptr, p.ptr);
    if (ret) {
        fprintf(stderr, "ring create failed: %d\n", ret);
        goto err;
    }
    nodrop = 0;
    if (p.features IORING_FEAT_NODROP.ptr)
        nodrop = 1;

    total = 0;
    for (i  in 0 until  BUFFERS / 2) {
        offset:off_t;

        sqe = io_uring_get_sqe(ring.ptr);
        if (!sqe) {
            fprintf(stderr, "sqe get failed\n");
            goto err;
        }
        offset = BS * (rand() % BUFFERS);
        if (fault && i == ENTRIES + 4)
            vecs[i].iov_base = NULL;
        io_uring_prep_readv(sqe, fd, vecs.ptr[i], 1, offset);

        ret = io_uring_submit(ring.ptr);
        if (nodrop && ret == -EBUSY) {
            *drops = 1;
            total = i;
            break;
        } else if (ret != 1) {
            fprintf(stderr, "submit got %d, wanted %d\n", ret, 1);
            total = i;
            break;
        }
        total++;
    }

    if (*drops)
        goto reap_it;

    usleep(usecs);

    for (i  in total until  BUFFERS) {
        offset:off_t;

        sqe = io_uring_get_sqe(ring.ptr);
        if (!sqe) {
            fprintf(stderr, "sqe get failed\n");
            goto err;
        }
        offset = BS * (rand() % BUFFERS);
        io_uring_prep_readv(sqe, fd, vecs.ptr[i], 1, offset);

        ret = io_uring_submit(ring.ptr);
        if (nodrop && ret == -EBUSY) {
            *drops = 1;
            break;
        } else if (ret != 1) {
            fprintf(stderr, "submit got %d, wanted %d\n", ret, 1);
            break;
        }
        total++;
    }

    reap_it:
    reaped = 0;
    do {
        if (nodrop) {
            /* nodrop should never lose events */
            if (reaped == total)
                break;
        } else {
            if (reaped + *ring.cq.koverflow == total)
                break;
        }
        ret = io_uring_wait_cqe(ring.ptr, cqe.ptr);
        if (ret) {
            fprintf(stderr, "wait_cqe=%d\n", ret);
            goto err;
        }
        if ( cqe.pointed.res  != BS) {
            if (!(fault && cqe.pointed.res  == -EFAULT)) {
                fprintf(stderr, "cqe res %d, wanted %d\n",
 cqe.pointed.res , BS);
                goto err;
            }
        }
        io_uring_cqe_seen(ring.ptr, cqe);
        reaped++;
    } while (1);

    if (!io_uring_peek_cqe(ring.ptr, cqe.ptr)) {
        fprintf(stderr, "found unexpected completion\n");
        goto err;
    }

    if (!nodrop) {
        *drops = *ring.cq.koverflow;
    } else if (*ring.cq.koverflow) {
        fprintf(stderr, "Found %u overflows\n", *ring.cq.koverflow);
        goto err;
    }

    io_uring_queue_exit(ring.ptr);
    close(fd);
    return 0;
    err:
    if (fd != -1)
        close(fd);
    io_uring_queue_exit(ring.ptr);
    return 1;
}

static reap_events:Int(ring:CPointer<io_uring>, unsigned nr_events, do_wait:Int) {
    cqe:CPointer<io_uring_cqe>;
    i:Int, ret = 0, seq = 0;

    for (i  in 0 until  nr_events) {
        if (do_wait)
            ret = io_uring_wait_cqe(ring, cqe.ptr);
        else
            ret = io_uring_peek_cqe(ring, cqe.ptr);
        if (ret) {
            if (ret != -EAGAIN)
                fprintf(stderr, "cqe peek failed: %d\n", ret);
            break;
        }
        if ( cqe.pointed.user_data  != seq) {
            fprintf(stderr, "cqe sequence out-of-order\n");
            fprintf(stderr, "got %d, wanted %d\n", (int) cqe.pointed.user_data ,
                    seq);
            return -EINVAL;
        }
        seq++;
        io_uring_cqe_seen(ring, cqe);
    }

    return i ? i : ret;
}

/*
 * Submit some NOPs and watch if the overflow is correct
 */
static test_overflow:Int(void) {
    ring:io_uring;
    p:io_uring_params;
    sqe:CPointer<io_uring_sqe>;
    unsigned pending;
    ret:Int, i, j;

    memset(p.ptr, 0, sizeof(p));
    ret = io_uring_queue_init_params(4, ring.ptr, p.ptr);
    if (ret) {
        fprintf(stderr, "io_uring_queue_init failed %d\n", ret);
        return 1;
    }

    /* submit 4x4 SQEs, should overflow the ring by 8 */
    pending = 0;
    for (i  in 0 until  4) {
        for (j  in 0 until  4) {
            sqe = io_uring_get_sqe(ring.ptr);
            if (!sqe) {
                fprintf(stderr, "get sqe failed\n");
                goto err;
            }

            io_uring_prep_nop(sqe);
 sqe.pointed.user_data  = (i * 4) + j;
        }

        ret = io_uring_submit(ring.ptr);
        if (ret == 4) {
            pending += 4;
            continue;
        }
        if (p.features IORING_FEAT_NODROP.ptr) {
            if (ret == -EBUSY)
                break;
        }
        fprintf(stderr, "sqe submit failed: %d\n", ret);
        goto err;
    }

    /* we should now have 8 completions ready */
    ret = reap_events(ring.ptr, pending, 0);
    if (ret < 0)
        goto err;

    if (!(p.features IORING_FEAT_NODROP.ptr)) {
        if (*ring.cq.koverflow != 8) {
            fprintf(stderr, "cq ring overflow %d, expected 8\n",
                    *ring.cq.koverflow);
            goto err;
        }
    }
    io_uring_queue_exit(ring.ptr);
    return 0;
    err:
    io_uring_queue_exit(ring.ptr);
    return 1;
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    fname:String = ".cq-overflow";
    unsigned iters, drops;
long :ULongusecs
    ret:Int;

    if (argc > 1)
        return 0;

    ret = test_overflow();
    if (ret) {
        printf("test_overflow failed\n");
        return ret;
    }

    t_create_file(fname, FILE_SIZE);

    vecs = t_create_buffers(BUFFERS, BS);

    iters = 0;
    usecs = 1000;
    do {
        drops = 0;

        if (test_io(fname, usecs, drops.ptr, 0)) {
            fprintf(stderr, "test_io nofault failed\n");
            goto err;
        }
        if (drops)
            break;
        usecs = (usecs * 12) / 10;
        iters++;
    } while (iters < 40);

    if (test_io(fname, usecs, drops.ptr, 0)) {
        fprintf(stderr, "test_io nofault failed\n");
        goto err;
    }

    if (test_io(fname, usecs, drops.ptr, 1)) {
        fprintf(stderr, "test_io fault failed\n");
        goto err;
    }

    unlink(fname);
    return 0;
    err:
    unlink(fname);
    return 1;
}
