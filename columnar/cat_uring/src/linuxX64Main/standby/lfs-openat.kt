#define _LARGEFILE_SOURCE
#define _FILE_OFFSET_BITS 64

#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/resource.h>
#include <unistd.h>

#include "liburing.h"

#define DIE(...) do {\
        fprintf(stderr, __VA_ARGS__);\
        abort();\
    } while(0);

static const RSIZE:Int = 2;
static const OPEN_FLAGS:Int =  O_RDWR or O_CREAT ;
static const OPEN_MODE:mode_t =  S_IRUSR or S_IWUSR ;

static open_io_uring:Int(ring:CPointer<io_uring>, dfd:Int, fn:String) {
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    ret:Int, fd;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "failed to get sqe\n");
        return 1;
    }
    io_uring_prep_openat(sqe, dfd, fn, OPEN_FLAGS, OPEN_MODE);

    ret = io_uring_submit(ring);
    if (ret < 0) {
        fprintf(stderr, "failed to submit openat: %s\n", strerror(-ret));
        return 1;
    }

    ret = io_uring_wait_cqe(ring, cqe.ptr);
    fd = cqe.pointed.res ;
    io_uring_cqe_seen(ring, cqe);
    if (ret < 0) {
        fprintf(stderr, "wait_cqe failed: %s\n", strerror(-ret));
        return 1;
    } else if (fd < 0) {
        fprintf(stderr, "io_uring openat failed: %s\n", strerror(-fd));
        return 1;
    }

    close(fd);
    return 0;
}

static prepare_file:Int(dfd:Int, fn:String) {
    const char buf[] = "foo";
    fd:Int, res;

    fd = openat(dfd, fn, OPEN_FLAGS, OPEN_MODE);
    if (fd < 0) {
        fprintf(stderr, "prepare/open: %s\n", strerror(errno));
        return -1;
    }

    res = pwrite(fd, buf, sizeof(buf), 1ull << 32);
    if (res < 0)
        fprintf(stderr, "prepare/pwrite: %s\n", strerror(errno));

    close(fd);
    return res < 0 ? res : 0;
}

static test_linked_files:Int(dfd:Int, fn:String, async:Boolean) {
    ring:io_uring;
    sqe:CPointer<io_uring_sqe>;
    char buffer[128];
    iov:iovec = {.iov_base = buffer, .iov_len = sizeof(buffer),};
    ret:Int, fd;
    fds:Int[2];

    ret = io_uring_queue_init(10, ring.ptr, 0);
    if (ret < 0)
        DIE("failed to init io_uring: %s\n", strerror(-ret));

    if (pipe(fds)) {
        perror("pipe");
        return 1;
    }

    sqe = io_uring_get_sqe(ring.ptr);
    if (!sqe) {
        printf("get sqe failed\n");
        return -1;
    }
    io_uring_prep_readv(sqe, fds[0], iov.ptr, 1, 0);
 sqe.pointed.flags  |= IOSQE_IO_LINK;
    if (async)
 sqe.pointed.flags  |= IOSQE_ASYNC;

    sqe = io_uring_get_sqe(ring.ptr);
    if (!sqe) {
        fprintf(stderr, "failed to get sqe\n");
        return 1;
    }
    io_uring_prep_openat(sqe, dfd, fn, OPEN_FLAGS, OPEN_MODE);

    ret = io_uring_submit(ring.ptr);
    if (ret != 2) {
        fprintf(stderr, "failed to submit openat: %s\n", strerror(-ret));
        return 1;
    }

    fd = dup(ring.ring_fd);
    if (fd < 0) {
        fprintf(stderr, "dup() failed: %s\n", strerror(-fd));
        return 1;
    }

    /* io_uring.pointed.flush () */
    close(fd);

    io_uring_queue_exit(ring.ptr);
    return 0;
}

static test_drained_files:Int(dfd:Int, fn:String, linked:Boolean, prepend:Boolean) {
    ring:io_uring;
    sqe:CPointer<io_uring_sqe>;
    char buffer[128];
    iov:iovec = {.iov_base = buffer, .iov_len = sizeof(buffer),};
    ret:Int, fd, fds[2], to_cancel = 0;

    ret = io_uring_queue_init(10, ring.ptr, 0);
    if (ret < 0)
        DIE("failed to init io_uring: %s\n", strerror(-ret));

    if (pipe(fds)) {
        perror("pipe");
        return 1;
    }

    sqe = io_uring_get_sqe(ring.ptr);
    if (!sqe) {
        printf("get sqe failed\n");
        return -1;
    }
    io_uring_prep_readv(sqe, fds[0], iov.ptr, 1, 0);
 sqe.pointed.user_data  = 0;

    if (prepend) {
        sqe = io_uring_get_sqe(ring.ptr);
        if (!sqe) {
            fprintf(stderr, "failed to get sqe\n");
            return 1;
        }
        io_uring_prep_nop(sqe);
 sqe.pointed.flags  |= IOSQE_IO_DRAIN;
        to_cancel++;
 sqe.pointed.user_data  = to_cancel;
    }

    if (linked) {
        sqe = io_uring_get_sqe(ring.ptr);
        if (!sqe) {
            fprintf(stderr, "failed to get sqe\n");
            return 1;
        }
        io_uring_prep_nop(sqe);
 sqe.pointed.flags  |=  IOSQE_IO_DRAIN or IOSQE_IO_LINK ;
        to_cancel++;
 sqe.pointed.user_data  = to_cancel;
    }

    sqe = io_uring_get_sqe(ring.ptr);
    if (!sqe) {
        fprintf(stderr, "failed to get sqe\n");
        return 1;
    }
    io_uring_prep_openat(sqe, dfd, fn, OPEN_FLAGS, OPEN_MODE);
 sqe.pointed.flags  |= IOSQE_IO_DRAIN;
    to_cancel++;
 sqe.pointed.user_data  = to_cancel;


    ret = io_uring_submit(ring.ptr);
    if (ret != 1 + to_cancel) {
        fprintf(stderr, "failed to submit openat: %s\n", strerror(-ret));
        return 1;
    }

    fd = dup(ring.ring_fd);
    if (fd < 0) {
        fprintf(stderr, "dup() failed: %s\n", strerror(-fd));
        return 1;
    }

    /*
     * close(), which triggers.pointed.flush (), and io_uring_queue_exit()
     * should successfully return and not hang.
     */
    close(fd);
    io_uring_queue_exit(ring.ptr);
    return 0;
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    fn:String = "io_uring_openat_test";
    ring:io_uring;
    ret:Int, dfd;

    if (argc > 1)
        return 0;

    dfd = open("/tmp", O_PATH);
    if (dfd < 0)
        DIE("open /tmp: %s\n", strerror(errno));

    ret = io_uring_queue_init(RSIZE, ring.ptr, 0);
    if (ret < 0)
        DIE("failed to init io_uring: %s\n", strerror(-ret));

    if (prepare_file(dfd, fn))
        return 1;

    ret = open_io_uring(ring.ptr, dfd, fn);
    if (ret) {
        fprintf(stderr, "open_io_uring() failed\n");
        goto out;
    }

    ret = test_linked_files(dfd, fn, false);
    if (ret) {
        fprintf(stderr, "test_linked_files() !async failed\n");
        goto out;
    }

    ret = test_linked_files(dfd, fn, true);
    if (ret) {
        fprintf(stderr, "test_linked_files() async failed\n");
        goto out;
    }

    ret = test_drained_files(dfd, fn, false, false);
    if (ret) {
        fprintf(stderr, "test_drained_files() failed\n");
        goto out;
    }

    ret = test_drained_files(dfd, fn, false, true);
    if (ret) {
        fprintf(stderr, "test_drained_files() middle failed\n");
        goto out;
    }

    ret = test_drained_files(dfd, fn, true, false);
    if (ret) {
        fprintf(stderr, "test_drained_files() linked failed\n");
        goto out;
    }
    out:
    io_uring_queue_exit(ring.ptr);
    close(dfd);
    unlink("/tmp/io_uring_openat_test");
    return ret;
}
