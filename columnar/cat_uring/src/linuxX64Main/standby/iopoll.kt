/* SPDX-License-Identifier: MIT */
/*
 * Description: basic read/write tests with polled IO
 */
#include <errno.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <sys/types.h>
#include <sys/poll.h>
#include <sys/eventfd.h>
#include <sys/resource.h>
#include "helpers.h"
#include "liburing.h"
#include "../src/syscall.h"

#define FILE_SIZE    (128 * 1024)
#define BS        4096
#define BUFFERS        (FILE_SIZE / BS)

static vecs:CPointer<iovec>;
static no_buf_select:Int;
static no_iopoll:Int;

static provide_buffers:Int(ring:CPointer<io_uring>) {
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    ret:Int, i;

    for (i  in 0 until  BUFFERS) {
        sqe = io_uring_get_sqe(ring);
        io_uring_prep_provide_buffers(sqe, vecs[i].iov_base,
                                      vecs[i].iov_len, 1, 1, i);
    }

    ret = io_uring_submit(ring);
    if (ret != BUFFERS) {
        fprintf(stderr, "submit: %d\n", ret);
        return 1;
    }

    for (i  in 0 until  BUFFERS) {
        ret = io_uring_wait_cqe(ring, cqe.ptr);
        if ( cqe.pointed.res  < 0) {
            fprintf(stderr, " cqe.pointed.res =%d\n", cqe.pointed.res );
            return 1;
        }
        io_uring_cqe_seen(ring, cqe);
    }

    return 0;
}

static __test_io:Int(file:String, ring:CPointer<io_uring>, write:Int, sqthread:Int,
                     fixed:Int, buf_select:Int) {
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    open_flags:Int;
    i:Int, fd = -1, ret;
    offset:off_t;

    if (buf_select) {
        write = 0;
        fixed = 0;
    }
    if (buf_select && provide_buffers(ring))
        return 1;

    if (write)
        open_flags = O_WRONLY;
    else
        open_flags = O_RDONLY;
    open_flags |= O_DIRECT;

    if (fixed) {
        ret = t_register_buffers(ring, vecs, BUFFERS);
        if (ret == T_SETUP_SKIP)
            return 0;
        if (ret != T_SETUP_OK) {
            fprintf(stderr, "buffer reg failed: %d\n", ret);
            goto err;
        }
    }
    fd = open(file, open_flags);
    if (fd < 0) {
        perror("file open");
        goto err;
    }
    if (sqthread) {
        ret = io_uring_register_files(ring, fd.ptr, 1);
        if (ret) {
            fprintf(stderr, "file reg failed: %d\n", ret);
            goto err;
        }
    }

    offset = 0;
    for (i  in 0 until  BUFFERS) {
        sqe = io_uring_get_sqe(ring);
        if (!sqe) {
            fprintf(stderr, "sqe get failed\n");
            goto err;
        }
        offset = BS * (rand() % BUFFERS);
        if (write) {
            do_fixed:Int = fixed;
            use_fd:Int = fd;

            if (sqthread)
                use_fd = 0;
            if (fixed && (i 1.ptr))
                do_fixed = 0;
            if (do_fixed) {
                io_uring_prep_write_fixed(sqe, use_fd, vecs[i].iov_base,
                                          vecs[i].iov_len,
                                          offset, i);
            } else {
                io_uring_prep_writev(sqe, use_fd, vecs.ptr[i], 1,
                                     offset);
            }
        } else {
            do_fixed:Int = fixed;
            use_fd:Int = fd;

            if (sqthread)
                use_fd = 0;
            if (fixed && (i 1.ptr))
                do_fixed = 0;
            if (do_fixed) {
                io_uring_prep_read_fixed(sqe, use_fd, vecs[i].iov_base,
                                         vecs[i].iov_len,
                                         offset, i);
            } else {
                io_uring_prep_readv(sqe, use_fd, vecs.ptr[i], 1,
                                    offset);
            }

        }
        if (sqthread)
 sqe.pointed.flags  |= IOSQE_FIXED_FILE;
        if (buf_select) {
 sqe.pointed.flags  |= IOSQE_BUFFER_SELECT;
 sqe.pointed.buf_group  = buf_select;
 sqe.pointed.user_data  = i;
        }
    }

    ret = io_uring_submit(ring);
    if (ret != BUFFERS) {
        fprintf(stderr, "submit got %d, wanted %d\n", ret, BUFFERS);
        goto err;
    }

    for (i  in 0 until  BUFFERS) {
        ret = io_uring_wait_cqe(ring, cqe.ptr);
        if (ret) {
            fprintf(stderr, "wait_cqe=%d\n", ret);
            goto err;
        } else if ( cqe.pointed.res  == -EOPNOTSUPP) {
            fprintf(stdout, "File/device/fs doesn't support polled IO\n");
            no_iopoll = 1;
            goto out;
        } else if ( cqe.pointed.res  != BS) {
            fprintf(stderr, "cqe res %d, wanted %d\n", cqe.pointed.res , BS);
            goto err;
        }
        io_uring_cqe_seen(ring, cqe);
    }

    if (fixed) {
        ret = io_uring_unregister_buffers(ring);
        if (ret) {
            fprintf(stderr, "buffer unreg failed: %d\n", ret);
            goto err;
        }
    }
    if (sqthread) {
        ret = io_uring_unregister_files(ring);
        if (ret) {
            fprintf(stderr, "file unreg failed: %d\n", ret);
            goto err;
        }
    }

    out:
    close(fd);
    return 0;
    err:
    if (fd != -1)
        close(fd);
    return 1;
}

extern __io_uring_flush_sq:Int(ring:CPointer<io_uring>);

/*
 * if we are polling io_uring_submit needs to always enter the
 * kernel to fetch events
 */
static test_io_uring_submit_enters:Int(file:String) {
    ring:io_uring;
    fd:Int, i, ret, ring_flags, open_flags;
    unsigned head;
    cqe:CPointer<io_uring_cqe>;

    if (no_iopoll)
        return 0;

    ring_flags = IORING_SETUP_IOPOLL;
    ret = io_uring_queue_init(64, ring.ptr, ring_flags);
    if (ret) {
        fprintf(stderr, "ring create failed: %d\n", ret);
        return 1;
    }

    open_flags =  O_WRONLY or O_DIRECT ;
    fd = open(file, open_flags);
    if (fd < 0) {
        perror("file open");
        goto err;
    }

    for (i  in 0 until  BUFFERS) {
        sqe:CPointer<io_uring_sqe>;
        offset:off_t = BS * (rand() % BUFFERS);

        sqe = io_uring_get_sqe(ring.ptr);
        io_uring_prep_writev(sqe, fd, vecs.ptr[i], 1, offset);
 sqe.pointed.user_data  = 1;
    }

    /* submit manually to avoid adding IORING_ENTER_GETEVENTS */
    ret = __sys_io_uring_enter(ring.ring_fd, __io_uring_flush_sq(ring.ptr), 0,
                               0, NULL);
    if (ret < 0)
        goto err;

    for (i  in 0 until  500) {
        ret = io_uring_submit(ring.ptr);
        if (ret != 0) {
            fprintf(stderr, "still had %d sqes to submit, this is unexpected", ret);
            goto err;
        }

        io_uring_for_each_cqe(ring.ptr, head, cqe) {
            /* runs after test_io so should not have happened */
            if ( cqe.pointed.res  == -EOPNOTSUPP) {
                fprintf(stdout, "File/device/fs doesn't support polled IO\n");
                goto err;
            }
            goto ok;
        }
        usleep(10000);
    }
    err:
    ret = 1;
    if (fd != -1)
        close(fd);

    ok:
    io_uring_queue_exit(ring.ptr);
    return ret;
}

static test_io:Int(file:String, write:Int, sqthread:Int, fixed:Int,
                   buf_select:Int) {
    ring:io_uring;
    ret:Int, ring_flags = IORING_SETUP_IOPOLL;

    if (no_iopoll)
        return 0;

    ret = t_create_ring(64, ring.ptr, ring_flags);
    if (ret == T_SETUP_SKIP)
        return 0;
    if (ret != T_SETUP_OK) {
        fprintf(stderr, "ring create failed: %d\n", ret);
        return 1;
    }
    ret = __test_io(file, ring.ptr, write, sqthread, fixed, buf_select);
    io_uring_queue_exit(ring.ptr);
    return ret;
}

static probe_buf_select:Int(void) {
    p:CPointer<io_uring_probe>;
    ring:io_uring;
    ret:Int;

    ret = io_uring_queue_init(1, ring.ptr, 0);
    if (ret) {
        fprintf(stderr, "ring create failed: %d\n", ret);
        return 1;
    }

    p = io_uring_get_probe_ring(ring.ptr);
    if (!p || !io_uring_opcode_supported(p, IORING_OP_PROVIDE_BUFFERS)) {
        no_buf_select = 1;
        fprintf(stdout, "Buffer select not supported, skipping\n");
        return 0;
    }
    io_uring_free_probe(p);
    return 0;
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    i:Int, ret, nr;
    char buf[256];
    fname:CPointer<ByteVar>;

    if (probe_buf_select())
        return 1;

    if (argc > 1) {
        fname = argv[1];
    } else {
        srand((unsigned) time(NULL));
        snprintf(buf, sizeof(buf), ".basic-rw-%u-%u",
                 (unsigned) rand(), (unsigned) getpid());
        fname = buf;
        t_create_file(fname, FILE_SIZE);
    }

    vecs = t_create_buffers(BUFFERS, BS);

    nr = 16;
    if (no_buf_select)
        nr = 8;
    for (i  in 0 until  nr) {
        write:Int = (i 1.ptr) != 0;
        sqthread:Int = (i 2.ptr) != 0;
        fixed:Int = (i 4.ptr) != 0;
        buf_select:Int = (i 8.ptr) != 0;

        ret = test_io(fname, write, sqthread, fixed, buf_select);
        if (ret) {
            fprintf(stderr, "test_io failed %d/%d/%d/%d\n",
                    write, sqthread, fixed, buf_select);
            goto err;
        }
        if (no_iopoll)
            break;
    }

    ret = test_io_uring_submit_enters(fname);
    if (ret) {
        fprintf(stderr, "test_io_uring_submit_enters failed\n");
        goto err;
    }

    if (fname != argv[1])
        unlink(fname);
    return 0;
    err:
    if (fname != argv[1])
        unlink(fname);
    return 1;
}
