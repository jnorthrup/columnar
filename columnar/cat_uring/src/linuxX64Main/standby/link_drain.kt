/* SPDX-License-Identifier: MIT */
/*
 * Description: test io_uring link io with drain io
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

static test_link_drain_one:Int(ring:CPointer<io_uring>) {
    cqe:CPointer<io_uring_cqe>;
    sqe:CPointer<io_uring_sqe>[5];
    iovecs:iovec;
    i:Int, fd, ret;
    off:off_t = 0;
    char data[5] = {0};
    char expect[5] = {0, 1, 2, 3, 4};

    fd = open("testfile",  O_WRONLY or O_CREAT , 0644);
    if (fd < 0) {
        perror("open");
        return 1;
    }

    iovecs.iov_base = t_malloc(4096);
    iovecs.iov_len = 4096;

    for (i  in 0 until  5) {
        sqe[i] = io_uring_get_sqe(ring);
        if (!sqe[i]) {
            printf("get sqe failed\n");
            goto err;
        }
    }

    /* normal heavy io */
    io_uring_prep_writev(sqe[0], fd, iovecs.ptr, 1, off);
    sqe[0]->user_data = 0;

    /* link io */
    io_uring_prep_nop(sqe[1]);
    sqe[1]->flags |= IOSQE_IO_LINK;
    sqe[1]->user_data = 1;

    /* link drain io */
    io_uring_prep_nop(sqe[2]);
    sqe[2]->flags |= ( IOSQE_IO_LINK or IOSQE_IO_DRAIN );
    sqe[2]->user_data = 2;

    /* link io */
    io_uring_prep_nop(sqe[3]);
    sqe[3]->user_data = 3;

    /* normal nop io */
    io_uring_prep_nop(sqe[4]);
    sqe[4]->user_data = 4;

    ret = io_uring_submit(ring);
    if (ret < 0) {
        printf("sqe submit failed\n");
        goto err;
    } else if (ret < 5) {
        printf("Submitted only %d\n", ret);
        goto err;
    }

    for (i  in 0 until  5) {
        ret = io_uring_wait_cqe(ring, cqe.ptr);
        if (ret < 0) {
            printf("child: wait completion %d\n", ret);
            goto err;
        }

        data[i] = cqe.pointed.user_data ;
        io_uring_cqe_seen(ring, cqe);
    }

    if (memcmp(data, expect, 5) != 0)
        goto err;

    free(iovecs.iov_base);
    close(fd);
    unlink("testfile");
    return 0;
    err:
    free(iovecs.iov_base);
    close(fd);
    unlink("testfile");
    return 1;
}

fun test_link_drain_multi(ring:CPointer<io_uring>):Int{
    cqe:CPointer<io_uring_cqe>;
    sqe:CPointer<io_uring_sqe>[9];
    iovecs:iovec;
    i:Int, fd, ret;
    off:off_t = 0;
    char data[9] = {0};
    char expect[9] = {0, 1, 2, 3, 4, 5, 6, 7, 8};

    fd = open("testfile",  O_WRONLY or O_CREAT , 0644);
    if (fd < 0) {
        perror("open");
        return 1;
    }
    unlink("testfile");

    iovecs.iov_base = t_malloc(4096);
    iovecs.iov_len = 4096;

    for (i  in 0 until  9) {
        sqe[i] = io_uring_get_sqe(ring);
        if (!sqe[i]) {
            printf("get sqe failed\n");
            goto err;
        }
    }

    /* normal heavy io */
    io_uring_prep_writev(sqe[0], fd, iovecs.ptr, 1, off);
    sqe[0]->user_data = 0;

    /* link1 io head */
    io_uring_prep_nop(sqe[1]);
    sqe[1]->flags |= IOSQE_IO_LINK;
    sqe[1]->user_data = 1;

    /* link1 drain io */
    io_uring_prep_nop(sqe[2]);
    sqe[2]->flags |= ( IOSQE_IO_LINK or IOSQE_IO_DRAIN );
    sqe[2]->user_data = 2;

    /* link1 io end*/
    io_uring_prep_nop(sqe[3]);
    sqe[3]->user_data = 3;

    /* link2 io head */
    io_uring_prep_nop(sqe[4]);
    sqe[4]->flags |= IOSQE_IO_LINK;
    sqe[4]->user_data = 4;

    /* link2 io */
    io_uring_prep_nop(sqe[5]);
    sqe[5]->flags |= IOSQE_IO_LINK;
    sqe[5]->user_data = 5;

    /* link2 drain io */
    io_uring_prep_writev(sqe[6], fd, iovecs.ptr, 1, off);
    sqe[6]->flags |= ( IOSQE_IO_LINK or IOSQE_IO_DRAIN );
    sqe[6]->user_data = 6;

    /* link2 io end */
    io_uring_prep_nop(sqe[7]);
    sqe[7]->user_data = 7;

    /* normal io */
    io_uring_prep_nop(sqe[8]);
    sqe[8]->user_data = 8;

    ret = io_uring_submit(ring);
    if (ret < 0) {
        printf("sqe submit failed\n");
        goto err;
    } else if (ret < 9) {
        printf("Submitted only %d\n", ret);
        goto err;
    }

    for (i  in 0 until  9) {
        ret = io_uring_wait_cqe(ring, cqe.ptr);
        if (ret < 0) {
            printf("child: wait completion %d\n", ret);
            goto err;
        }

        data[i] = cqe.pointed.user_data ;
        io_uring_cqe_seen(ring, cqe);
    }

    if (memcmp(data, expect, 9) != 0)
        goto err;

    free(iovecs.iov_base);
    close(fd);
    return 0;
    err:
    free(iovecs.iov_base);
    close(fd);
    return 1;

}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    ring:io_uring;
    i:Int, ret;

    if (argc > 1)
        return 0;

    ret = io_uring_queue_init(100, ring.ptr, 0);
    if (ret) {
        printf("ring setup failed\n");
        return 1;
    }

    for (i  in 0 until  1000) {
        ret = test_link_drain_one(ring.ptr);
        if (ret) {
            fprintf(stderr, "test_link_drain_one failed\n");
            break;
        }
        ret = test_link_drain_multi(ring.ptr);
        if (ret) {
            fprintf(stderr, "test_link_drain_multi failed\n");
            break;
        }
    }

    return ret;
}
