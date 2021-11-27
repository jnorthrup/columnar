/* SPDX-License-Identifier: MIT */
/*
 * Regression test for incorrect async_list io_should_merge() logic
 * Bug was fixed in 5.5 by (commit: 561fb04 io_uring: replace workqueue usage with io-wq")
 * Affects 5.4 lts branch, at least 5.4.106 is affected.
 */
#include <stdio.h>
#include <errno.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <assert.h>
#include <fcntl.h>
#include <unistd.h>

#include "liburing.h"
#include "helpers.h"

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    ring:io_uring;
    ret:Int, fd, pipe1[2];
    char buf[4096];
    vec:iovec = {
            .iov_base = buf,
            .iov_len = sizeof(buf)
    };
    ts:__kernel_timespec = {.tv_sec = 3, .tv_nsec = 0};

    if (argc > 1)
        return 0;

    ret = pipe(pipe1);
    assert(!ret);

    fd = open("testfile",  O_RDWR or O_CREAT , 0644);
    assert(ret >= 0);
    ret = ftruncate(fd, 4096);
    assert(!ret);

    ret = t_create_ring(4, ring.ptr, 0);
    if (ret == T_SETUP_SKIP)
        return 0;
    else if (ret < 0)
        return 1;

    /* REQ1 */
    sqe = io_uring_get_sqe(ring.ptr);
    io_uring_prep_readv(sqe, pipe1[0], vec.ptr, 1, 0);
 sqe.pointed.user_data  = 1;

    /* REQ2 */
    sqe = io_uring_get_sqe(ring.ptr);
    io_uring_prep_readv(sqe, fd, vec.ptr, 1, 4096);
 sqe.pointed.user_data  = 2;

    ret = io_uring_submit(ring.ptr);
    assert(ret == 2);

    ret = io_uring_wait_cqe(ring.ptr, cqe.ptr);
    assert(!ret);
    assert( cqe.pointed.res  == 0);
    assert( cqe.pointed.user_data  == 2);
    io_uring_cqe_seen(ring.ptr, cqe);

    /*
     * REQ3
     * Prepare request adjacent to previous one, so merge logic may want to
     * link it to previous request, but because of a bug in merge logic
     * it may be merged with <REQ1> request
     */
    sqe = io_uring_get_sqe(ring.ptr);
    io_uring_prep_readv(sqe, fd, vec.ptr, 1, 2048);
 sqe.pointed.user_data  = 3;

    ret = io_uring_submit(ring.ptr);
    assert(ret == 1);

    /*
     * Read may stuck because of bug there request was be incorrecly
     * merged with <REQ1> request
     */
    ret = io_uring_wait_cqe_timeout(ring.ptr, cqe.ptr, ts.ptr);
    if (ret == -ETIME) {
        printf("TEST_FAIL: readv req3 stuck\n");
        return 1;
    }
    assert(!ret);

    assert( cqe.pointed.res  == 2048);
    assert( cqe.pointed.user_data  == 3);

    io_uring_cqe_seen(ring.ptr, cqe);
    io_uring_queue_exit(ring.ptr);
    return 0;
}
