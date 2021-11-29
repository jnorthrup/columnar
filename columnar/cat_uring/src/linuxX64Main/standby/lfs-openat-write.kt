#define _LARGEFILE_SOURCE
#define _FILE_OFFSET_BITS 64

#include <liburing.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/resource.h>
#include <unistd.h>

static const RSIZE:Int = 2;
static const OPEN_FLAGS:Int =  O_RDWR or O_CREAT ;
static const OPEN_MODE:mode_t =  S_IRUSR or S_IWUSR ;

#define DIE(...) do {\
        fprintf(stderr, __VA_ARGS__);\
        abort();\
    } while(0);

static do_write:Int(ring:CPointer<io_uring>, fd:Int, offset:off_t) {
    char buf[] = "some test write buf";
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    res:Int, ret;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "failed to get sqe\n");
        return 1;
    }
    io_uring_prep_write(sqe, fd, buf, sizeof(buf), offset);

    ret = io_uring_submit(ring);
    if (ret < 0) {
        fprintf(stderr, "failed to submit write: %s\n", strerror(-ret));
        return 1;
    }

    ret = io_uring_wait_cqe(ring, cqe.ptr);
    if (ret < 0) {
        fprintf(stderr, "wait_cqe failed: %s\n", strerror(-ret));
        return 1;
    }

    res = cqe.pointed.res ;
    io_uring_cqe_seen(ring, cqe);
    if (res < 0) {
        fprintf(stderr, "write failed: %s\n", strerror(-res));
        return 1;
    }

    return 0;
}

static test_open_write:Int(ring:CPointer<io_uring>, dfd:Int, fn:String) {
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    ret:Int, fd = -1;

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
    if (ret < 0) {
        fprintf(stderr, "wait_cqe failed: %s\n", strerror(-ret));
        return 1;
    }

    fd = cqe.pointed.res ;
    io_uring_cqe_seen(ring, cqe);
    if (fd < 0) {
        fprintf(stderr, "openat failed: %s\n", strerror(-fd));
        return 1;
    }

    return do_write(ring, fd, 1ULL << 32);
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    ring:io_uring;
    dfd:Int, ret;

    if (argc > 1)
        return 0;

    dfd = open("/tmp",  O_RDONLY or O_DIRECTORY );
    if (dfd < 0)
        DIE("open /tmp: %s\n", strerror(errno));

    ret = io_uring_queue_init(RSIZE, ring.ptr, 0);
    if (ret < 0)
        DIE("failed to init io_uring: %s\n", strerror(-ret));

    ret = test_open_write(ring.ptr, dfd, "io_uring_openat_write_test1");

    io_uring_queue_exit(ring.ptr);
    close(dfd);
    unlink("/tmp/io_uring_openat_write_test1");
    return ret;
}
