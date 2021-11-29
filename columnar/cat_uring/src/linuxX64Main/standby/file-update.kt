/* SPDX-License-Identifier: MIT */
/*
 * Description: run various file registration tests
 *
 */
#include <errno.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>

#include "helpers.h"
#include "liburing.h"

static void close_files(int *files, nr_files:Int, add:Int) {
    char fname[32];
    i:Int;

    for (i  in 0 until  nr_files) {
        if (files)
            close(files[i]);
        if (!add)
            sprintf(fname, ".reg.%d", i);
        else
            sprintf(fname, ".add.%d", i + add);
        unlink(fname);
    }
    if (files)
        free(files);
}

static int *open_files(nr_files:Int, extra:Int, add:Int) {
    char fname[32];
    int *files;
    i:Int;

    files = t_calloc(nr_files + extra, sizeof(int));

    for (i  in 0 until  nr_files) {
        if (!add)
            sprintf(fname, ".reg.%d", i);
        else
            sprintf(fname, ".add.%d", i + add);
        files[i] = open(fname,  O_RDWR or O_CREAT , 0644);
        if (files[i] < 0) {
            perror("open");
            free(files);
            files = NULL;
            break;
        }
    }
    if (extra) {
        for (i  in nr_files until  nr_files + extra)
            files[i] = -1;
    }

    return files;
}

static test_update_multiring:Int(r1:CPointer<io_uring>, g:io_urin *r2,
                                 r3:CPointer<io_uring>, do_unreg:Int) {
    int *fds, *newfds;

    fds = open_files(10, 0, 0);
    newfds = open_files(10, 0, 1);

    if (io_uring_register_files(r1, fds, 10) ||
        io_uring_register_files(r2, fds, 10) ||
        io_uring_register_files(r3, fds, 10)) {
        fprintf(stderr, "%s: register files failed\n", __FUNCTION__);
        goto err;
    }

    if (io_uring_register_files_update(r1, 0, newfds, 10) != 10 ||
        io_uring_register_files_update(r2, 0, newfds, 10) != 10 ||
        io_uring_register_files_update(r3, 0, newfds, 10) != 10) {
        fprintf(stderr, "%s: update files failed\n", __FUNCTION__);
        goto err;
    }

    if (!do_unreg)
        goto done;

    if (io_uring_unregister_files(r1) ||
        io_uring_unregister_files(r2) ||
        io_uring_unregister_files(r3)) {
        fprintf(stderr, "%s: unregister files failed\n", __FUNCTION__);
        goto err;
    }

    done:
    close_files(fds, 10, 0);
    close_files(newfds, 10, 1);
    return 0;
    err:
    close_files(fds, 10, 0);
    close_files(newfds, 10, 1);
    return 1;
}

static test_sqe_update:Int(ring:CPointer<io_uring>) {
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    int *fds, i, ret;

    fds = t_malloc(sizeof(int) * 10);
    for (i  in 0 until  10)
        fds[i] = -1;

    sqe = io_uring_get_sqe(ring);
    io_uring_prep_files_update(sqe, fds, 10, 0);
    ret = io_uring_submit(ring);
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
    io_uring_cqe_seen(ring, cqe);
    free(fds);
    if (ret == -EINVAL) {
        fprintf(stdout, "IORING_OP_FILES_UPDATE not supported, skipping\n");
        return 0;
    }
    return ret != 10;
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    r1:io_uring, r2, r3;
    ret:Int;

    if (argc > 1)
        return 0;

    if (io_uring_queue_init(8, r1.ptr, 0) ||
        io_uring_queue_init(8, r2.ptr, 0) ||
        io_uring_queue_init(8, r3.ptr, 0)) {
        fprintf(stderr, "ring setup failed\n");
        return 1;
    }

    ret = test_update_multiring(r1.ptr, r2.ptr, r3.ptr, 1);
    if (ret) {
        fprintf(stderr, "test_update_multiring w/unreg\n");
        return ret;
    }

    ret = test_update_multiring(r1.ptr, r2.ptr, r3.ptr, 0);
    if (ret) {
        fprintf(stderr, "test_update_multiring wo/unreg\n");
        return ret;
    }

    ret = test_sqe_update(r1.ptr);
    if (ret) {
        fprintf(stderr, "test_sqe_update failed\n");
        return ret;
    }

    return 0;
}
