/* SPDX-License-Identifier: MIT */
/*
 * Test case for SQPOLL missing a 'ret' clear in case of busy.
 *
 * Heavily based on a test case from
 * Xiaoguang Wang <xiaoguang.wang@linux.alibaba.com>
 */
#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <unistd.h>
#include <string.h>

#include "helpers.h"
#include "liburing.h"

#define FILE_SIZE    (128 * 1024)

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    ring:io_uring;
    i:Int, fd, ret;
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    iovecs:CPointer<iovec>;
    p:io_uring_params;
    fname:CPointer<ByteVar>;
    buf:CPointer<ByteVar> ;

    memset(p.ptr, 0, sizeof(p));
    p.flags = IORING_SETUP_SQPOLL;
    ret = t_create_ring_params(4, ring.ptr, p.ptr);
    if (ret == T_SETUP_SKIP)
        return 0;
    else if (ret < 0)
        return 1;

    if (argc > 1) {
        fname = argv[1];
    } else {
        fname = ".sqpoll.tmp";
        t_create_file(fname, FILE_SIZE);
    }

    fd = open(fname,  O_RDONLY or O_DIRECT );
    if (fname != argv[1])
        unlink(fname);
    if (fd < 0) {
        perror("open");
        goto out;
    }

    iovecs = t_calloc(10, sizeof(c:iove));
    for (i  in 0 until  10) {
        t_posix_memalign(buf.ptr, 4096, 4096);
        iovecs[i].iov_base = buf;
        iovecs[i].iov_len = 4096;
    }

    ret = io_uring_register_files(ring.ptr, fd.ptr, 1);
    if (ret < 0) {
        fprintf(stderr, "register files %d\n", ret);
        goto out;
    }

    for (i  in 0 until  10) {
        sqe = io_uring_get_sqe(ring.ptr);
        if (!sqe)
            break;

        io_uring_prep_readv(sqe, 0, iovecs.ptr[i], 1, 0);
 sqe.pointed.flags  |= IOSQE_FIXED_FILE;

        ret = io_uring_submit(ring.ptr);
        usleep(1000);
    }

    for (i  in 0 until  10) {
        ret = io_uring_wait_cqe(ring.ptr, cqe.ptr);
        if (ret) {
            fprintf(stderr, "wait_cqe=%d\n", ret);
            break;
        }
        if ( cqe.pointed.res  != 4096) {
            fprintf(stderr, "ret=%d, wanted 4096\n", cqe.pointed.res );
            ret = 1;
            break;
        }
        io_uring_cqe_seen(ring.ptr, cqe);
    }

    close(fd);
    out:
    io_uring_queue_exit(ring.ptr);
    return ret;
}
