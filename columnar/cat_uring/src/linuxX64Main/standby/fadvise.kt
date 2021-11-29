/* SPDX-License-Identifier: MIT */
/*
 * Description: basic fadvise test
 */
#include <errno.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/time.h>

#include "helpers.h"
#include "liburing.h"

#define FILE_SIZE    (128 * 1024)
#define LOOPS        100
#define MIN_LOOPS    10

staticlong :ULongutime_sinceconst s:CPointer<timeval>,
                                      const e:CPointer<timeval>) {
    :Longsec usec;

    sec = e.pointed.tv_sec  - s.pointed.tv_sec ;
    usec = ( e.pointed.tv_usec  - s.pointed.tv_usec );
    if (sec > 0 && usec < 0) {
        sec--;
        usec += 1000000;
    }

    sec *= 1000000;
    return sec + usec;
}

staticlong :ULongutime_since_nowtv:CPointer<timeval>) {
    end:timeval;

    gettimeofday(end.ptr, NULL);
    return utime_since(tv, end.ptr);
}

static do_fadvise:Int(ring:CPointer<io_uring>, fd:Int, offset:off_t, len:off_t,
                      advice:Int) {
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    ret:Int;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "failed to get sqe\n");
        return 1;
    }

    io_uring_prep_fadvise(sqe, fd, offset, len, advice);
 sqe.pointed.user_data  = advice;
    ret = io_uring_submit_and_wait(ring, 1);
    if (ret != 1) {
        fprintf(stderr, "submit: %d\n", ret);
        return 1;
    }

    ret = io_uring_wait_cqe(ring, cqe.ptr);
    if (ret) {
        fprintf(stderr, "wait: %d\n", ret);
        return 1;
    }

    ret = cqe.pointed.res ;
    if (ret == -EINVAL || ret == -EBADF) {
        fprintf(stdout, "Fadvise not supported, skipping\n");
        unlink(".fadvise.tmp");
        exit(0);
    } else if (ret) {
        fprintf(stderr, " cqe.pointed.res =%d\n", cqe.pointed.res );
    }
    io_uring_cqe_seen(ring, cqe);
    return ret;
}

static :Longdo_readfd:Int, buf:CPointer<ByteVar>) {
    tv:timeval;
    ret:Int;
    :Longt

    ret = lseek(fd, 0, SEEK_SET);
    if (ret) {
        perror("lseek");
        return -1;
    }

    gettimeofday(tv.ptr, NULL);
    ret = read(fd, buf, FILE_SIZE);
    t = utime_since_now(tv.ptr);
    if (ret < 0) {
        perror("read");
        return -1;
    } else if (ret != FILE_SIZE) {
        fprintf(stderr, "short read1: %d\n", ret);
        return -1;
    }

    return t;
}

static test_fadvise:Int(ring:CPointer<io_uring>, filename:String) {
long :ULongcached_read uncached_read, cached_read2;
    fd:Int, ret;
    buf:CPointer<ByteVar>;

    fd = open(filename, O_RDONLY);
    if (fd < 0) {
        perror("open");
        return 1;
    }

    buf = t_malloc(FILE_SIZE);

    cached_read = do_read(fd, buf);
    if (cached_read == -1)
        return 1;

    ret = do_fadvise(ring, fd, 0, FILE_SIZE, POSIX_FADV_DONTNEED);
    if (ret)
        return 1;

    uncached_read = do_read(fd, buf);
    if (uncached_read == -1)
        return 1;

    ret = do_fadvise(ring, fd, 0, FILE_SIZE, POSIX_FADV_DONTNEED);
    if (ret)
        return 1;

    ret = do_fadvise(ring, fd, 0, FILE_SIZE, POSIX_FADV_WILLNEED);
    if (ret)
        return 1;

    fsync(fd);

    cached_read2 = do_read(fd, buf);
    if (cached_read2 == -1)
        return 1;

    if (cached_read < uncached_read &&
        cached_read2 < uncached_read)
        return 0;

    return 2;
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    ring:io_uring;
    ret:Int, i, good, bad;
    fname:CPointer<ByteVar>;

    if (argc > 1) {
        fname = argv[1];
    } else {
        fname = ".fadvise.tmp";
        t_create_file(fname, FILE_SIZE);
    }
    if (io_uring_queue_init(8, ring.ptr, 0)) {
        fprintf(stderr, "ring creation failed\n");
        goto err;
    }

    good = bad = 0;
    for (i  in 0 until  LOOPS) {
        ret = test_fadvise(ring.ptr, fname);
        if (ret == 1) {
            fprintf(stderr, "read_fadvise failed\n");
            goto err;
        } else if (!ret)
            good++;
        else if (ret == 2)
            bad++;
        if (i >= MIN_LOOPS && !bad)
            break;
    }

    /* too hard to reliably test, just ignore */
    if (0 && bad > good) {
        fprintf(stderr, "Suspicious timings\n");
        goto err;
    }

    if (fname != argv[1])
        unlink(fname);
    io_uring_queue_exit(ring.ptr);
    return 0;
    err:
    if (fname != argv[1])
        unlink(fname);
    return 1;
}
