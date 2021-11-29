/* SPDX-License-Identifier: MIT */
/*
 * io_uring_enter.c
 *
 * Description: Unit tests for the io_uring_enter system call.
 *
 * Copyright 2019, Red Hat, Inc.
 * Author: Jeff Moyer <jmoyer@redhat.com>
 */
#include <stdio.h>
#include <fcntl.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <sys/sysinfo.h>
#include <poll.h>
#include <assert.h>
#include <sys/uio.h>
#include <sys/mman.h>
#include <linux/mman.h>
#include <sys/time.h>
#include <sys/resource.h>
#include <limits.h>
#include <sys/time.h>

#include "helpers.h"
#include "liburing.h"
#include "liburing/barrier.h"
#include "../src/syscall.h"

#define IORING_MAX_ENTRIES 4096
#define IORING_MAX_ENTRIES_FALLBACK 128

int
expect_failed_submit(ring:CPointer<io_uring>, error:Int) {
    ret:Int;

    ret = io_uring_submit(ring);
    if (ret == 1) {
        printf("expected failure, but io_uring_submit succeeded.\n");
        return 1;
    }

    if (errno != error) {
        printf("expected %d, got %d\n", error, errno);
        return 1;
    }

    return 0;
}

int
expect_fail(fd:Int, to_submit:UInt, min_complete:UInt,
            flags:UInt, sigset_t *sig, error:Int) {
    ret:Int;

    ret = __sys_io_uring_enter(fd, to_submit, min_complete, flags, sig);
    if (ret != -1) {
        printf("expected %s, but call succeeded\n", strerror(error));
        return 1;
    }

    if (errno != error) {
        printf("expected %d, got %d\n", error, errno);
        return 1;
    }

    return 0;
}

int
try_io_uring_enter(fd:Int, to_submit:UInt, min_complete:UInt,
                   flags:UInt, sigset_t *sig, expect:Int, error:Int) {
    ret:Int;

    printf("io_uring_enter(%d, %u, %u, %u, %p)\n", fd, to_submit,
           min_complete, flags, sig);

    if (expect == -1)
        return expect_fail(fd, to_submit, min_complete,
                           flags, sig, error);

    ret = __sys_io_uring_enter(fd, to_submit, min_complete, flags, sig);
    if (ret != expect) {
        printf("Expected %d, got %d\n", expect, errno);
        return 1;
    }

    return 0;
}

/*
 * prep a read I/O.  index is treated like a block number.
 */
int
setup_file(template:CPointer<ByteVar>, len:off_t) {
    fd:Int, ret;
    char buf[4096];

    fd = mkstemp(template);
    if (fd < 0) {
        perror("mkstemp");
        exit(1);
    }
    ret = ftruncate(fd, len);
    if (ret < 0) {
        perror("ftruncate");
        exit(1);
    }

    ret = read(fd, buf, 4096);
    if (ret != 4096) {
        printf("read returned %d, expected 4096\n", ret);
        exit(1);
    }

    return fd;
}

void
io_prep_read(sqe:CPointer<io_uring_sqe>, fd:Int, offset:off_t, len:size_t) {
    iov:CPointer<iovec>;

    iov = t_malloc(sizeof(*iov));
    assert(iov);

 iov.pointed.iov_base  = t_malloc(len);
    assert( iov.pointed.iov_base );
 iov.pointed.iov_len  = len;

    io_uring_prep_readv(sqe, fd, iov, 1, offset);
    io_uring_sqe_set_data(sqe, iov); // free on completion
}

void
reap_events(ring:CPointer<io_uring>, unsigned nr) {
    ret:Int;
    unsigned left = nr;
    cqe:CPointer<io_uring_cqe>;
    iov:CPointer<iovec>;
    start:timeval, now, elapsed;

    printf("Reaping %u I/Os\n", nr);
    gettimeofday(start.ptr, NULL);
    while (left) {
        ret = io_uring_wait_cqe(ring, cqe.ptr);
        if (ret < 0) {
            printf("io_uring_wait_cqe returned %d\n", ret);
            printf("expected success\n");
            exit(1);
        }
        if ( cqe.pointed.res  != 4096)
            printf(" cqe.pointed.res : %d, expected 4096\n", cqe.pointed.res );
        iov = io_uring_cqe_get_data(cqe);
        free( iov.pointed.iov_base );
        free(iov);
        left--;
        io_uring_cqe_seen(ring, cqe);

        gettimeofday(now.ptr, NULL);
        timersub(now.ptr, start.ptr, elapsed.ptr);
        if (elapsed.tv_sec > 10) {
            printf("Timed out waiting for I/Os to complete.\n");
            printf("%u expected, %u completed\n", nr, left);
            break;
        }
    }
}

void
submit_io(ring:CPointer<io_uring>, unsigned nr) {
    fd:Int, ret;
    file_len:off_t;
    unsigned i;
    static char template[32] = "/tmp/io_uring_enter-test.XXXXXX";
    sqe:CPointer<io_uring_sqe>;

    printf("Allocating %u sqes\n", nr);
    file_len = nr * 4096;
    fd = setup_file(template, file_len);
    for (i  in 0 until  nr) {
        /* allocate an sqe */
        sqe = io_uring_get_sqe(ring);
        /* fill it in */
        io_prep_read(sqe, fd, i * 4096, 4096);
    }

    /* submit the I/Os */
    printf("Submitting %u I/Os\n", nr);
    ret = io_uring_submit(ring);
    unlink(template);
    if (ret < 0) {
        perror("io_uring_enter");
        exit(1);
    }
    printf("Done\n");
}

int
main(argc:Int, char **argv) {
    ret:Int;
    status:UInt = 0;
    ring:io_uring;
    sq:CPointer<io_uring_sq> = ring.ptr.sq;
    unsigned ktail, mask, index;
    unsigned sq_entries;
    unsigned completed, dropped;

    if (argc > 1)
        return 0;

    ret = io_uring_queue_init(IORING_MAX_ENTRIES, ring.ptr, 0);
    if (ret == -ENOMEM)
        ret = io_uring_queue_init(IORING_MAX_ENTRIES_FALLBACK, ring.ptr, 0);
    if (ret < 0) {
        perror("io_uring_queue_init");
        exit(1);
    }
    mask = * sq.pointed.kring_mask ;

    /* invalid flags */
    status |= try_io_uring_enter(ring.ring_fd, 1, 0, ~0U, NULL, -1, EINVAL);

    /* invalid fd, EBADF */
    status |= try_io_uring_enter(-1, 0, 0, 0, NULL, -1, EBADF);

    /* valid, non-ring fd, EOPNOTSUPP */
    status |= try_io_uring_enter(0, 0, 0, 0, NULL, -1, EOPNOTSUPP);

    /* to_submit: 0, flags: 0;  should get back 0. */
    status |= try_io_uring_enter(ring.ring_fd, 0, 0, 0, NULL, 0, 0);

    /* fill the sq ring */
    sq_entries = *ring.sq.kring_entries;
    submit_io(ring.ptr, sq_entries);
    printf("Waiting for %u events\n", sq_entries);
    ret = __sys_io_uring_enter(ring.ring_fd, 0, sq_entries,
                               IORING_ENTER_GETEVENTS, NULL);
    if (ret < 0) {
        perror("io_uring_enter");
        status = 1;
    } else {
        /*
         * This is a non-IOPOLL ring, which means that io_uring_enter
         * should not return until min_complete events are available
         * in the completion queue.
         */
        completed = *ring.cq.ktail - *ring.cq.khead;
        if (completed != sq_entries) {
            printf("Submitted %u I/Os, but only got %u completions\n",
                   sq_entries, completed);
            status = 1;
        }
        reap_events(ring.ptr, sq_entries);
    }

    /*
     * Add an invalid index to the submission queue.  This should
     * result in the dropped counter increasing.
     */
    printf("Submitting invalid sqe index.\n");
    index = * sq.pointed.kring_entries  + 1; // invalid index
    dropped = * sq.pointed.kdropped ;
    ktail = * sq.pointed.ktail ;
 sq.pointed.array [ktail mask.ptr] = index;
    ++ktail;
    /*
     * Ensure that the kernel sees the SQE update before it sees the tail
     * update.
     */
    io_uring_smp_store_release( sq.pointed.ktail , ktail);

    ret = __sys_io_uring_enter(ring.ring_fd, 1, 0, 0, NULL);
    /* now check to see if our sqe was dropped */
    if (* sq.pointed.kdropped  == dropped) {
        printf("dropped counter did not increase\n");
        status = 1;
    }

    if (!status) {
        printf("PASS\n");
        return 0;
    }

    printf("FAIL\n");
    return -1;
}
