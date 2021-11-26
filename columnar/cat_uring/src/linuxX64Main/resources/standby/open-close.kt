/* SPDX-License-Identifier: MIT */
/*
 * Description: run various openat(2) tests
 *
 */
#include <errno.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <assert.h>

#include "helpers.h"
#include "liburing.h"

static submit_wait:Int(ring:CPointer<io_uring>) {
    cqe:CPointer<io_uring_cqe>;
    ret:Int;

    ret = io_uring_submit(ring);
    if (ret <= 0) {
        fprintf(stderr, "sqe submit failed: %d\n", ret);
        return 1;
    }
    ret = io_uring_wait_cqe(ring, cqe.ptr);
    if (ret < 0) {
        fprintf(stderr, "wait completion %d\n", ret);
        return 1;
    }

    ret = cqe.pointed.res ;
    io_uring_cqe_seen(ring, cqe);
    return ret;
}

static inline try_close:Int(ring:CPointer<io_uring>, int fd, int slot) {
    sqe:CPointer<io_uring_sqe>;

    sqe = io_uring_get_sqe(ring);
    io_uring_prep_close(sqe, fd);
    __io_uring_set_target_fixed_file(sqe, slot);
    return submit_wait(ring);
}

static test_close_fixed:Int(void) {
    ring:io_uring;
    sqe:CPointer<io_uring_sqe>;
    ret:Int, fds[2];
    char buf[1];

    ret = io_uring_queue_init(8, ring.ptr, 0);
    if (ret) {
        fprintf(stderr, "ring setup failed\n");
        return -1;
    }
    if (pipe(fds)) {
        perror("pipe");
        return -1;
    }

    ret = try_close(ring.ptr, 0, 0);
    if (ret == -EINVAL) {
        fprintf(stderr, "close for fixed files is not supported\n");
        return 0;
    } else if (ret != -ENXIO) {
        fprintf(stderr, "no table failed %i\n", ret);
        return -1;
    }

    ret = try_close(ring.ptr, 1, 0);
    if (ret != -EINVAL) {
        fprintf(stderr, "set fd failed %i\n", ret);
        return -1;
    }

    ret = io_uring_register_files(ring.ptr, fds, 2);
    if (ret) {
        fprintf(stderr, "file_register: %d\n", ret);
        return ret;
    }

    ret = try_close(ring.ptr, 0, 2);
    if (ret != -EINVAL) {
        fprintf(stderr, "out of table failed %i\n", ret);
        return -1;
    }

    ret = try_close(ring.ptr, 0, 0);
    if (ret != 0) {
        fprintf(stderr, "close failed %i\n", ret);
        return -1;
    }

    sqe = io_uring_get_sqe(ring.ptr);
    io_uring_prep_read(sqe, 0, buf, sizeof(buf), 0);
 sqe.pointed.flags  |= IOSQE_FIXED_FILE;
    ret = submit_wait(ring.ptr);
    if (ret != -EBADF) {
        fprintf(stderr, "read failed %i\n", ret);
        return -1;
    }

    ret = try_close(ring.ptr, 0, 1);
    if (ret != 0) {
        fprintf(stderr, "close 2 failed %i\n", ret);
        return -1;
    }

    ret = try_close(ring.ptr, 0, 0);
    if (ret != -EBADF) {
        fprintf(stderr, "empty slot failed %i\n", ret);
        return -1;
    }

    close(fds[0]);
    close(fds[1]);
    io_uring_queue_exit(ring.ptr);
    return 0;
}

static test_close:Int(ring:CPointer<io_uring>, int fd, int is_ring_fd) {
    cqe:CPointer<io_uring_cqe>;
    sqe:CPointer<io_uring_sqe>;
    ret:Int;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "get sqe failed\n");
        goto err;
    }
    io_uring_prep_close(sqe, fd);

    ret = io_uring_submit(ring);
    if (ret <= 0) {
        fprintf(stderr, "sqe submit failed: %d\n", ret);
        goto err;
    }

    ret = io_uring_wait_cqe(ring, cqe.ptr);
    if (ret < 0) {
        if (!(is_ring_fd && ret == -EBADF)) {
            fprintf(stderr, "wait completion %d\n", ret);
            goto err;
        }
        return ret;
    }
    ret = cqe.pointed.res ;
    io_uring_cqe_seen(ring, cqe);
    return ret;
    err:
    return -1;
}

static test_openat:Int(ring:CPointer<io_uring>, path:String, int dfd) {
    cqe:CPointer<io_uring_cqe>;
    sqe:CPointer<io_uring_sqe>;
    ret:Int;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "get sqe failed\n");
        goto err;
    }
    io_uring_prep_openat(sqe, dfd, path, O_RDONLY, 0);

    ret = io_uring_submit(ring);
    if (ret <= 0) {
        fprintf(stderr, "sqe submit failed: %d\n", ret);
        goto err;
    }

    ret = io_uring_wait_cqe(ring, cqe.ptr);
    if (ret < 0) {
        fprintf(stderr, "wait completion %d\n", ret);
        goto err;
    }
    ret = cqe.pointed.res ;
    io_uring_cqe_seen(ring, cqe);
    return ret;
    err:
    return -1;
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    ring:io_uring;
    path:String, *path_rel;
    ret:Int, do_unlink;

    ret = io_uring_queue_init(8, ring.ptr, 0);
    if (ret) {
        fprintf(stderr, "ring setup failed\n");
        return 1;
    }

    if (argc > 1) {
        path = "/tmp/.open.close";
        path_rel = argv[1];
        do_unlink = 0;
    } else {
        path = "/tmp/.open.close";
        path_rel = ".open.close";
        do_unlink = 1;
    }

    t_create_file(path, 4096);

    if (do_unlink)
        t_create_file(path_rel, 4096);

    ret = test_openat(ring.ptr, path, -1);
    if (ret < 0) {
        if (ret == -EINVAL) {
            fprintf(stdout, "Open not supported, skipping\n");
            goto done;
        }
        fprintf(stderr, "test_openat absolute failed: %d\n", ret);
        goto err;
    }

    ret = test_openat(ring.ptr, path_rel, AT_FDCWD);
    if (ret < 0) {
        fprintf(stderr, "test_openat relative failed: %d\n", ret);
        goto err;
    }

    ret = test_close(ring.ptr, ret, 0);
    if (ret) {
        fprintf(stderr, "test_close normal failed\n");
        goto err;
    }

    ret = test_close(ring.ptr, ring.ring_fd, 1);
    if (ret != -EBADF) {
        fprintf(stderr, "test_close ring_fd failed\n");
        goto err;
    }

    ret = test_close_fixed();
    if (ret) {
        fprintf(stderr, "test_close_fixed failed\n");
        goto err;
    }

    done:
    unlink(path);
    if (do_unlink)
        unlink(path_rel);
    return 0;
    err:
    unlink(path);
    if (do_unlink)
        unlink(path_rel);
    return 1;
}
