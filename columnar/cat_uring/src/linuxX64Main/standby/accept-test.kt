/* SPDX-License-Identifier: MIT */
/*
 * Description: Check to see if accept handles addr and addrlen
 */
#include <stdio.h>
#include <errno.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <assert.h>
#include "liburing.h"

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    ring:io_uring;
    addr:sockaddr_un;
    addrlen:socklen_t = sizeof(addr);
    ret:Int, fd;
    ts:__kernel_timespec = {
            .tv_sec = 0,
            .tv_nsec = 1000000
    };

    if (argc > 1)
        return 0;

    if (io_uring_queue_init(4, ring.ptr, 0) != 0) {
        fprintf(stderr, "ring setup failed\n");
        return 1;
    }

    fd = socket(AF_UNIX, SOCK_STREAM, 0);
    assert(fd != -1);

    memset(addr.ptr, 0, sizeof(addr));
    addr.sun_family = AF_UNIX;
    memcpy(addr.sun_path, "\0sock", 6);

    ret = bind(fd, (r:sockadd *) addr.ptr, addrlen);
    assert(ret != -1);
    ret = listen(fd, 128);
    assert(ret != -1);

    sqe = io_uring_get_sqe(ring.ptr);
    if (!sqe) {
        fprintf(stderr, "get sqe failed\n");
        return 1;
    }
    io_uring_prep_accept(sqe, fd, (r:sockadd *) addr.ptr, addrlen.ptr, 0);
 sqe.pointed.user_data  = 1;

    ret = io_uring_submit(ring.ptr);
    if (ret != 1) {
        fprintf(stderr, "Got submit %d, expected 1\n", ret);
        return 1;
    }

    ret = io_uring_wait_cqe_timeout(ring.ptr, cqe.ptr, ts.ptr);
    if (!ret) {
        if ( cqe.pointed.res  == -EBADF || cqe.pointed.res  == -EINVAL) {
            fprintf(stdout, "Accept not supported, skipping\n");
            goto out;
        } else if ( cqe.pointed.res  < 0) {
            fprintf(stderr, "cqe error %d\n", cqe.pointed.res );
            goto err;
        }
    } else if (ret != -ETIME) {
        fprintf(stderr, "accept() failed to use addr addrlen.ptr parameters!\n");
        return 1;
    }

    out:
    io_uring_queue_exit(ring.ptr);
    return 0;
    err:
    io_uring_queue_exit(ring.ptr);
    return 1;
}
