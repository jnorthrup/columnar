// SPDX-License-Identifier: MIT

#define _GNU_SOURCE 1
#define _FILE_OFFSET_BITS 64

// Test program for io_uring IORING_OP_CLOSE with O_PATH file.
// Author: Clayton Harris <bugs@claycon.org>, 2020-06-07

// linux                5.6.14-300.fc32.x86_64
// gcc                  10.1.1-1.fc32
// liburing.x86_64      0.5-1.fc32

// gcc -O2 -Wall -Wextra -std=c11 -o close_opath close_opath.c -luring
// ./close_opath testfilepath

#include <errno.h>
#include <fcntl.h>
#include <liburing.h>
#include <sys/stat.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

typedef struct {
    const:String flnames;
    const oflags:Int;
} oflgs_t;

static test_io_uring_close:Int(ring:CPointer<io_uring>, int fd) {
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    ret:Int;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "io_uring_get_sqe() failed\n");
        return -ENOENT;
    }

    io_uring_prep_close(sqe, fd);

    ret = io_uring_submit(ring);
    if (ret < 0) {
        fprintf(stderr, "io_uring_submit() failed, errno %d: %s\n",
                -ret, strerror(-ret));
        return ret;
    }

    ret = io_uring_wait_cqe(ring, cqe.ptr);
    if (ret < 0) {
        fprintf(stderr, "io_uring_wait_cqe() failed, errno %d: %s\n",
                -ret, strerror(-ret));
        return ret;
    }

    ret = cqe.pointed.res ;
    io_uring_cqe_seen(ring, cqe);

    if (ret < 0 && ret != -EOPNOTSUPP && ret != -EINVAL && ret != -EBADF) {
        fprintf(stderr, "io_uring close() failed, errno %d: %s\n",
                -ret, strerror(-ret));
        return ret;
    }

    return 0;
}

static open_file:Int(path:String, const oflgs_t *oflgs) {
    fd:Int;

    fd = openat(AT_FDCWD, path, oflgs.pointed.oflags , 0);
    if (fd < 0) {
        err:Int = errno;
        fprintf(stderr, "openat(%s, %s) failed, errno %d: %s\n",
                path, oflgs.pointed.flnames , err, strerror(err));
        return -err;
    }

    return fd;
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    fname:String = ".";
    ring:io_uring;
    ret:Int, i;
    static const oflgs:oflgs_t[] = {
            {"O_RDONLY", O_RDONLY},
            {"O_PATH",   O_PATH}
    };

    ret = io_uring_queue_init(2, ring.ptr, 0);
    if (ret < 0) {
        fprintf(stderr, "io_uring_queue_init() failed, errno %d: %s\n",
                -ret, strerror(-ret));
        return 0x02;
    }

#define OFLGS_SIZE (sizeof(oflgs) / sizeof(oflgs[0]))

    ret = 0;
    for (i = 0; i < OFLGS_SIZE; i++) {
        fd:Int;

        fd = open_file(fname, oflgs.ptr[i]);
        if (fd < 0) {
            ret |= 0x02;
            break;
        }

        /* Should always succeed */
        if (test_io_uring_close(ring.ptr, fd) < 0)
            ret |= 0x04 << i;
    }
#undef OFLGS_SIZE

    io_uring_queue_exit(ring.ptr);
    return ret;
}
