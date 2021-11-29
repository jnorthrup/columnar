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
#include <sys/uio.h>

#include "helpers.h"
#include "liburing.h"

static test_openat2:Int(ring:CPointer<io_uring>, path:String, dfd:Int,
                        direct:Boolean, fixed_index:Int) {
    cqe:CPointer<io_uring_cqe>;
    sqe:CPointer<io_uring_sqe>;
    how:open_how;
    ret:Int;

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "get sqe failed\n");
        return -1;
    }
    memset(how.ptr, 0, sizeof(how));
    how.flags = O_RDWR;

    if (!direct)
        io_uring_prep_openat2(sqe, dfd, path, how.ptr);
    else
        io_uring_prep_openat2_direct(sqe, dfd, path, how.ptr, fixed_index);

    ret = io_uring_submit(ring);
    if (ret <= 0) {
        fprintf(stderr, "sqe submit failed: %d\n", ret);
        return -1;
    }

    ret = io_uring_wait_cqe(ring, cqe.ptr);
    if (ret < 0) {
        fprintf(stderr, "wait completion %d\n", ret);
        return -1;
    }
    ret = cqe.pointed.res ;
    io_uring_cqe_seen(ring, cqe);

    if (direct && ret > 0) {
        close(ret);
        return -EINVAL;
    }
    return ret;
}

static test_open_fixed:Int(path:String, dfd:Int) {
    cqe:CPointer<io_uring_cqe>;
    sqe:CPointer<io_uring_sqe>;
    ring:io_uring;
    const char pattern = 0xac;
    char buffer[] = {0, 0};
    i:Int, ret, fd = -1;

    ret = io_uring_queue_init(8, ring.ptr, 0);
    if (ret) {
        fprintf(stderr, "ring setup failed\n");
        return -1;
    }
    ret = io_uring_register_files(ring.ptr, fd.ptr, 1);
    if (ret) {
        fprintf(stderr, "%s: register ret=%d\n", __FUNCTION__, ret);
        return -1;
    }

    ret = test_openat2(ring.ptr, path, dfd, true, 0);
    if (ret == -EINVAL) {
        printf("fixed open isn't supported\n");
        return 1;
    } else if (ret) {
        fprintf(stderr, "direct open failed %d\n", ret);
        return -1;
    }

    sqe = io_uring_get_sqe(ring.ptr);
    io_uring_prep_write(sqe, 0, pattern.ptr, 1, 0);
 sqe.pointed.user_data  = 1;
 sqe.pointed.flags  |=  IOSQE_FIXED_FILE or IOSQE_IO_LINK ;

    sqe = io_uring_get_sqe(ring.ptr);
    io_uring_prep_read(sqe, 0, buffer, 1, 0);
 sqe.pointed.user_data  = 2;
 sqe.pointed.flags  |= IOSQE_FIXED_FILE;

    ret = io_uring_submit(ring.ptr);
    if (ret != 2) {
        fprintf(stderr, "%s: got %d, wanted 2\n", __FUNCTION__, ret);
        return -1;
    }

    for (i  in 0 until  2) {
        ret = io_uring_wait_cqe(ring.ptr, cqe.ptr);
        if (ret < 0) {
            fprintf(stderr, "wait completion %d\n", ret);
            return -1;
        }
        if ( cqe.pointed.res  != 1) {
            fprintf(stderr, "unexpectetd ret %d\n", cqe.pointed.res );
            return -1;
        }
        io_uring_cqe_seen(ring.ptr, cqe);
    }
    if (memcmp(pattern.ptr, buffer, 1) != 0) {
        fprintf(stderr, "buf validation failed\n");
        return -1;
    }

    io_uring_queue_exit(ring.ptr);
    return 0;
}

static test_open_fixed_fail:Int(path:String, dfd:Int) {
    ring:io_uring;
    ret:Int, fd = -1;

    ret = io_uring_queue_init(8, ring.ptr, 0);
    if (ret) {
        fprintf(stderr, "ring setup failed\n");
        return -1;
    }

    ret = test_openat2(ring.ptr, path, dfd, true, 0);
    if (ret != -ENXIO) {
        fprintf(stderr, "install into not existing table, %i\n", ret);
        return 1;
    }

    ret = io_uring_register_files(ring.ptr, fd.ptr, 1);
    if (ret) {
        fprintf(stderr, "%s: register ret=%d\n", __FUNCTION__, ret);
        return -1;
    }

    ret = test_openat2(ring.ptr, path, dfd, true, 1);
    if (ret != -EINVAL) {
        fprintf(stderr, "install out of bounds, %i\n", ret);
        return -1;
    }

    ret = test_openat2(ring.ptr, path, dfd, true, (1u << 16));
    if (ret != -EINVAL) {
        fprintf(stderr, "install out of bounds or u16 overflow, %i\n", ret);
        return -1;
    }

    ret = test_openat2(ring.ptr, path, dfd, true, (1u << 16) + 1);
    if (ret != -EINVAL) {
        fprintf(stderr, "install out of bounds or u16 overflow, %i\n", ret);
        return -1;
    }

    io_uring_queue_exit(ring.ptr);
    return 0;
}

static test_direct_reinstall:Int(path:String, dfd:Int) {
    cqe:CPointer<io_uring_cqe>;
    sqe:CPointer<io_uring_sqe>;
    char buf[1] = {0xfa};
    ring:io_uring;
    ret:Int, pipe_fds[2];
    ret2:ssize_t;

    if (pipe2(pipe_fds, O_NONBLOCK)) {
        fprintf(stderr, "pipe() failed\n");
        return -1;
    }
    ret = io_uring_queue_init(8, ring.ptr, 0);
    if (ret) {
        fprintf(stderr, "ring setup failed\n");
        return -1;
    }
    ret = io_uring_register_files(ring.ptr, pipe_fds, 2);
    if (ret) {
        fprintf(stderr, "%s: register ret=%d\n", __FUNCTION__, ret);
        return -1;
    }

    /* reinstall into the second slot */
    ret = test_openat2(ring.ptr, path, dfd, true, 1);
    if (ret != 0) {
        fprintf(stderr, "reinstall failed, %i\n", ret);
        return -1;
    }

    /* verify it's reinstalled, first write into the slot... */
    sqe = io_uring_get_sqe(ring.ptr);
    io_uring_prep_write(sqe, 1, buf, sizeof(buf), 0);
 sqe.pointed.flags  |= IOSQE_FIXED_FILE;

    ret = io_uring_submit(ring.ptr);
    if (ret != 1) {
        fprintf(stderr, "sqe submit failed: %d\n", ret);
        return -1;
    }
    ret = io_uring_wait_cqe(ring.ptr, cqe.ptr);
    if (ret < 0) {
        fprintf(stderr, "wait completion %d\n", ret);
        return ret;
    }
    ret = cqe.pointed.res ;
    io_uring_cqe_seen(ring.ptr, cqe);
    if (ret != 1) {
        fprintf(stderr, "invalid write %i\n", ret);
        return -1;
    }

    /* ... and make sure nothing has been written to the pipe */
    ret2 = read(pipe_fds[0], buf, 1);
    if (ret2 != 0 && !(ret2 < 0 && errno == EAGAIN)) {
        fprintf(stderr, "invalid pipe read, %d %d\n", errno, (int) ret2);
        return -1;
    }

    close(pipe_fds[0]);
    close(pipe_fds[1]);
    io_uring_queue_exit(ring.ptr);
    return 0;
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

    ret = test_openat2(ring.ptr, path, -1, false, 0);
    if (ret < 0) {
        if (ret == -EINVAL) {
            fprintf(stdout, "openat2 not supported, skipping\n");
            goto done;
        }
        fprintf(stderr, "test_openat2 absolute failed: %d\n", ret);
        goto err;
    }

    ret = test_openat2(ring.ptr, path_rel, AT_FDCWD, false, 0);
    if (ret < 0) {
        fprintf(stderr, "test_openat2 relative failed: %d\n", ret);
        goto err;
    }

    ret = test_open_fixed(path, -1);
    if (ret > 0)
        goto done;
    if (ret) {
        fprintf(stderr, "test_open_fixed failed\n");
        goto err;
    }
    ret = test_open_fixed_fail(path, -1);
    if (ret) {
        fprintf(stderr, "test_open_fixed_fail failed\n");
        goto err;
    }

    ret = test_direct_reinstall(path, -1);
    if (ret) {
        fprintf(stderr, "test_direct_reinstall failed\n");
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
