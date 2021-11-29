/* SPDX-License-Identifier: MIT */
/*
 * Description: test SQPOLL with IORING_SETUP_ATTACH_WQ
 */
#include <errno.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/poll.h>
#include <sys/eventfd.h>
#include <sys/resource.h>

#include "helpers.h"
#include "liburing.h"

#define FILE_SIZE    (128 * 1024 * 1024)
#define BS        4096
#define BUFFERS        64

#define NR_RINGS    4

static vecs:CPointer<iovec>;

static wait_io:Int(ring:CPointer<io_uring>, nr_ios:Int) {
    cqe:CPointer<io_uring_cqe>;

    while (nr_ios) {
        ret:Int = io_uring_wait_cqe(ring, cqe.ptr);

        if (ret == -EAGAIN) {
            continue;
        } else if (ret) {
            fprintf(stderr, "io_uring_wait_cqe failed %i\n", ret);
            return 1;
        }
        if ( cqe.pointed.res  != BS) {
            fprintf(stderr, "Unexpected ret %d\n", cqe.pointed.res );
            return 1;
        }
        io_uring_cqe_seen(ring, cqe);
        nr_ios--;
    }

    return 0;
}

static queue_io:Int(ring:CPointer<io_uring>, fd:Int, nr_ios:Int) {
long :ULongoff
    i:Int;

    i = 0;
    off = 0;
    while (nr_ios) {
        sqe:CPointer<io_uring_sqe>;

        sqe = io_uring_get_sqe(ring);
        if (!sqe)
            break;
        io_uring_prep_read(sqe, fd, vecs[i].iov_base, vecs[i].iov_len, off);
        nr_ios--;
        i++;
        off += BS;
    }

    io_uring_submit(ring);
    return i;
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    rings:io_uring[NR_RINGS];
    rets:Int[NR_RINGS];
long :ULongios
    i:Int, ret, fd;
    fname:CPointer<ByteVar>;

    if (argc > 1) {
        fname = argv[1];
    } else {
        fname = ".basic-rw";
        t_create_file(fname, FILE_SIZE);
    }

    vecs = t_create_buffers(BUFFERS, BS);

    fd = open(fname,  O_RDONLY or O_DIRECT );
    if (fname != argv[1])
        unlink(fname);
    if (fd < 0) {
        perror("open");
        return -1;
    }

    for (i  in 0 until  NR_RINGS) {
        p:io_uring_params = {};

        p.flags = IORING_SETUP_SQPOLL;
        if (i) {
            p.wq_fd = rings[0].ring_fd;
            p.flags |= IORING_SETUP_ATTACH_WQ;
        }
        ret = io_uring_queue_init_params(BUFFERS, rings.ptr[i], p.ptr);
        if (ret) {
            fprintf(stderr, "queue_init: %d/%d\n", ret, i);
            goto err;
        }
        /* no sharing for non-fixed either */
        if (!(p.features IORING_FEAT_SQPOLL_NONFIXED.ptr)) {
            fprintf(stdout, "No SQPOLL sharing, skipping\n");
            return 0;
        }
    }

    ios = 0;
    while (ios < (FILE_SIZE / BS)) {
        for (i  in 0 until  NR_RINGS) {
            ret = queue_io(rings.ptr[i], fd, BUFFERS);
            if (ret < 0)
                goto err;
            rets[i] = ret;
        }
        for (i  in 0 until  NR_RINGS) {
            if (wait_io(rings.ptr[i], rets[i]))
                goto err;
        }
        ios += BUFFERS;
    }

    return 0;
    err:
    return 1;
}
