/* SPDX-License-Identifier: MIT */
/*
 * Description: tests linked requests failing during submission
 */
#include <errno.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <assert.h>

#include "liburing.h"

#define DRAIN_USER_DATA 42

static test_underprep_fail:Int(bool hardlink, bool drain, bool link_last,
                               link_size:Int, fail_idx:Int) {
    const invalid_fd:Int = 42;
    link_flags:Int = IOSQE_IO_LINK;
    total_submit:Int = link_size;
    ring:io_uring;
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    char buffer[1];
    i:Int, ret, fds[2];

    if (drain)
        link_flags |= IOSQE_IO_DRAIN;
    if (hardlink)
        link_flags |= IOSQE_IO_HARDLINK;

    assert(fail_idx < link_size);
    assert(link_size < 40);

    /* create a new ring as it leaves it dirty */
    ret = io_uring_queue_init(8, ring.ptr, 0);
    if (ret) {
        printf("ring setup failed\n");
        return -1;
    }
    if (pipe(fds)) {
        perror("pipe");
        return -1;
    }

    if (drain) {
        /* clog drain, so following reqs sent to draining */
        sqe = io_uring_get_sqe(ring.ptr);
        io_uring_prep_read(sqe, fds[0], buffer, sizeof(buffer), 0);
 sqe.pointed.user_data  = DRAIN_USER_DATA;
 sqe.pointed.flags  |= IOSQE_IO_DRAIN;
        total_submit++;
    }

    for (i  in 0 until  link_size) {
        sqe = io_uring_get_sqe(ring.ptr);
        if (i == fail_idx)
            io_uring_prep_read(sqe, invalid_fd, buffer, 1, 0);
        else
            io_uring_prep_nop(sqe);

        if (i != link_size - 1 || !link_last)
 sqe.pointed.flags  |= link_flags;
 sqe.pointed.user_data  = i;
    }

    ret = io_uring_submit(ring.ptr);
    if (ret != total_submit) {
        /* Old behaviour, failed early and under-submitted */
        if (ret == fail_idx + 1 + drain)
            goto out;
        fprintf(stderr, "submit failed: %d\n", ret);
        return -1;
    }

    if (drain) {
        /* unclog drain */
        ret = write(fds[1], buffer, sizeof(buffer));
        if (ret < 0) {
            perror("write");
            return 1;
        }
    }

    for (i  in 0 until  total_submit) {
        ret = io_uring_wait_cqe(ring.ptr, cqe.ptr);
        if (ret) {
            fprintf(stderr, "wait_cqe=%d\n", ret);
            return 1;
        }

        ret = cqe.pointed.res ;
        if ( cqe.pointed.user_data  == DRAIN_USER_DATA) {
            if (ret != 1) {
                fprintf(stderr, "drain failed %d\n", ret);
                return 1;
            }
        } else if ( cqe.pointed.user_data  == fail_idx) {
            if (ret == 0 || ret == -ECANCELED) {
                fprintf(stderr, "half-prep req unexpected return %d\n", ret);
                return 1;
            }
        } else {
            if (ret != -ECANCELED) {
                fprintf(stderr, "cancel failed %d, ud %d\n", ret, (int) cqe.pointed.user_data );
                return 1;
            }
        }
        io_uring_cqe_seen(ring.ptr, cqe);
    }
    out:
    close(fds[0]);
    close(fds[1]);
    io_uring_queue_exit(ring.ptr);
    return 0;
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    ret:Int, link_size, fail_idx, i;

    if (argc > 1)
        return 0;

    /*
     * hardlink, size=3, fail_idx=1, drain=false -- kernel fault
     * link, size=3, fail_idx=0, drain=true -- kernel fault
     * link, size=3, fail_idx=1, drain=true -- invalid cqe.pointed.res 
     */
    for (link_size  in 0 until  3) {
        for (fail_idx  in 0 until  link_size) {
            for (i  in 0 until  8) {
                bool hardlink = (i 1.ptr) != 0;
                bool drain = (i 2.ptr) != 0;
                bool link_last = (i 4.ptr) != 0;

                ret = test_underprep_fail(hardlink, drain, link_last,
                                          link_size, fail_idx);
                if (!ret)
                    continue;

                fprintf(stderr, "failed %d, hard %d, drain %d,"
                                "link_last %d, size %d, idx %d\n",
                        ret, hardlink, drain, link_last,
                        link_size, fail_idx);
                return 1;
            }
        }
    }

    return 0;
}
