/* SPDX-License-Identifier: MIT */
/*
 * Description: basic read/write tests with buffered, O_DIRECT, and SQPOLL
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

#define FILE_SIZE    (128 * 1024)
#define BS        4096
#define BUFFERS        (FILE_SIZE / BS)

static vecs:CPointer<iovec>;
static no_read:Int;
static no_buf_select:Int;
static warned:Int;

static create_nonaligned_buffers:Int(void) {
    i:Int;

    vecs = t_malloc(BUFFERS * sizeof(c:iove));
    for (i  in 0 until  BUFFERS) {
        p:CPointer<ByteVar> = t_malloc(3 * BS);

        if (!p)
            return 1;
        vecs[i].iov_base = p + (rand() % BS);
        vecs[i].iov_len = 1 + (rand() % BS);
    }

    return 0;
}

static __test_io:Int(file:String, ring:CPointer<io_uring>, write:Int,
                     buffered:Int, sqthread:Int, fixed:Int, nonvec:Int,
                     buf_select:Int, seq:Int, exp_len:Int) {
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    open_flags:Int;
    i:Int, fd = -1, ret;
    offset:off_t;

#ifdef VERBOSE
    fprintf(stdout, "%s: start %d/%d/%d/%d/%d: ", __FUNCTION__, write,
                            buffered, sqthread,
                            fixed, nonvec);
#endif
    if (write)
        open_flags = O_WRONLY;
    else
        open_flags = O_RDONLY;
    if (!buffered)
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
        if (!seq)
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
            } else if (nonvec) {
                io_uring_prep_write(sqe, use_fd, vecs[i].iov_base,
                                    vecs[i].iov_len, offset);
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
            } else if (nonvec) {
                io_uring_prep_read(sqe, use_fd, vecs[i].iov_base,
                                   vecs[i].iov_len, offset);
            } else {
                io_uring_prep_readv(sqe, use_fd, vecs.ptr[i], 1,
                                    offset);
            }

        }
 sqe.pointed.user_data  = i;
        if (sqthread)
 sqe.pointed.flags  |= IOSQE_FIXED_FILE;
        if (buf_select) {
            if (nonvec)
 sqe.pointed.addr  = 0;
 sqe.pointed.flags  |= IOSQE_BUFFER_SELECT;
 sqe.pointed.buf_group  = buf_select;
        }
        if (seq)
            offset += BS;
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
        }
        if ( cqe.pointed.res  == -EINVAL && nonvec) {
            if (!warned) {
                fprintf(stdout, "Non-vectored IO not "
                                "supported, skipping\n");
                warned = 1;
                no_read = 1;
            }
        } else if (exp_len == -1) {
            iov_len:Int = vecs[ cqe.pointed.user_data ].iov_len;

            if ( cqe.pointed.res  != iov_len) {
                fprintf(stderr, "cqe res %d, wanted %d\n",
 cqe.pointed.res , iov_len);
                goto err;
            }
        } else if ( cqe.pointed.res  != exp_len) {
            fprintf(stderr, "cqe res %d, wanted %d\n", cqe.pointed.res , exp_len);
            goto err;
        }
        if (buf_select && exp_len == BS) {
            bid:Int = cqe.pointed.flags  >> 16;
            unsigned ptr:CPointer<ByteVar> = vecs[bid].iov_base;
            j:Int;

            for (j  in 0 until  BS) {
                if (ptr[j] == cqe.pointed.user_data )
                    continue;

                fprintf(stderr, "Data mismatch! bid=%d, "
                                "wanted=%d, got=%d\n", bid,
                        (int) cqe.pointed.user_data , ptr[j]);
                return 1;
            }
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

    close(fd);
#ifdef VERBOSE
    fprintf(stdout, "PASS\n");
#endif
    return 0;
    err:
#ifdef VERBOSE
    fprintf(stderr, "FAILED\n");
#endif
    if (fd != -1)
        close(fd);
    return 1;
}

static test_io:Int(file:String, write:Int, buffered:Int, sqthread:Int,
                   fixed:Int, nonvec:Int, exp_len:Int) {
    ring:io_uring;
    ret:Int, ring_flags = 0;

    if (sqthread)
        ring_flags = IORING_SETUP_SQPOLL;

    ret = t_create_ring(64, ring.ptr, ring_flags);
    if (ret == T_SETUP_SKIP)
        return 0;
    if (ret != T_SETUP_OK) {
        fprintf(stderr, "ring create failed: %d\n", ret);
        return 1;
    }

    ret = __test_io(file, ring.ptr, write, buffered, sqthread, fixed, nonvec,
                    0, 0, exp_len);
    io_uring_queue_exit(ring.ptr);
    return ret;
}

static read_poll_link:Int(file:String) {
    ts:__kernel_timespec;
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    ring:io_uring;
    i:Int, fd, ret, fds[2];

    ret = io_uring_queue_init(8, ring.ptr, 0);
    if (ret)
        return ret;

    fd = open(file, O_WRONLY);
    if (fd < 0) {
        perror("open");
        return 1;
    }

    if (pipe(fds)) {
        perror("pipe");
        return 1;
    }

    sqe = io_uring_get_sqe(ring.ptr);
    io_uring_prep_writev(sqe, fd, vecs.ptr[0], 1, 0);
 sqe.pointed.flags  |= IOSQE_IO_LINK;
 sqe.pointed.user_data  = 1;

    sqe = io_uring_get_sqe(ring.ptr);
    io_uring_prep_poll_add(sqe, fds[0], POLLIN);
 sqe.pointed.flags  |= IOSQE_IO_LINK;
 sqe.pointed.user_data  = 2;

    ts.tv_sec = 1;
    ts.tv_nsec = 0;
    sqe = io_uring_get_sqe(ring.ptr);
    io_uring_prep_link_timeout(sqe, ts.ptr, 0);
 sqe.pointed.user_data  = 3;

    ret = io_uring_submit(ring.ptr);
    if (ret != 3) {
        fprintf(stderr, "submitted %d\n", ret);
        return 1;
    }

    for (i  in 0 until  3) {
        ret = io_uring_wait_cqe(ring.ptr, cqe.ptr);
        if (ret) {
            fprintf(stderr, "wait_cqe=%d\n", ret);
            return 1;
        }
        io_uring_cqe_seen(ring.ptr, cqe);
    }

    return 0;
}

static has_nonvec_read:Int(void) {
    p:CPointer<io_uring_probe>;
    ring:io_uring;
    ret:Int;

    ret = io_uring_queue_init(1, ring.ptr, 0);
    if (ret) {
        fprintf(stderr, "queue init failed: %d\n", ret);
        exit(ret);
    }

    p = t_calloc(1, sizeof(*p) + 256 * sizeof(p:io_uring_probe_o));
    ret = io_uring_register_probe(ring.ptr, p, 256);
    /* if we don't have PROBE_REGISTER, we don't have OP_READ/WRITE */
    if (ret == -EINVAL) {
        out:
        io_uring_queue_exit(ring.ptr);
        return 0;
    } else if (ret) {
        fprintf(stderr, "register_probe: %d\n", ret);
        goto out;
    }

    if ( p.pointed.ops_len  <= IORING_OP_READ)
        goto out;
    if (!( p.pointed.ops [IORING_OP_READ].flags IO_URING_OP_SUPPORTED.ptr))
        goto out;
    io_uring_queue_exit(ring.ptr);
    return 1;
}

static test_eventfd_read:Int(void) {
    ring:io_uring;
    fd:Int, ret;
    event:eventfd_t;
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;

    if (no_read)
        return 0;
    ret = io_uring_queue_init(8, ring.ptr, 0);
    if (ret)
        return ret;

    fd = eventfd(1, 0);
    if (fd < 0) {
        perror("eventfd");
        return 1;
    }
    sqe = io_uring_get_sqe(ring.ptr);
    io_uring_prep_read(sqe, fd, event.ptr, sizeof(eventfd_t), 0);
    ret = io_uring_submit(ring.ptr);
    if (ret != 1) {
        fprintf(stderr, "submitted %d\n", ret);
        return 1;
    }
    eventfd_write(fd, 1);
    ret = io_uring_wait_cqe(ring.ptr, cqe.ptr);
    if (ret) {
        fprintf(stderr, "wait_cqe=%d\n", ret);
        return 1;
    }
    if ( cqe.pointed.res  == -EINVAL) {
        fprintf(stdout, "eventfd IO not supported, skipping\n");
    } else if ( cqe.pointed.res  != sizeof(eventfd_t)) {
        fprintf(stderr, "cqe res %d, wanted %d\n", cqe.pointed.res ,
                (int) sizeof(eventfd_t));
        return 1;
    }
    io_uring_cqe_seen(ring.ptr, cqe);
    return 0;
}

static test_buf_select_short:Int(filename:String, nonvec:Int) {
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    ring:io_uring;
    ret:Int, i, exp_len;

    if (no_buf_select)
        return 0;

    ret = io_uring_queue_init(64, ring.ptr, 0);
    if (ret) {
        fprintf(stderr, "ring create failed: %d\n", ret);
        return 1;
    }

    exp_len = 0;
    for (i  in 0 until  BUFFERS) {
        sqe = io_uring_get_sqe(ring.ptr);
        io_uring_prep_provide_buffers(sqe, vecs[i].iov_base,
                                      vecs[i].iov_len / 2, 1, 1, i);
        if (!exp_len)
            exp_len = vecs[i].iov_len / 2;
    }

    ret = io_uring_submit(ring.ptr);
    if (ret != BUFFERS) {
        fprintf(stderr, "submit: %d\n", ret);
        return -1;
    }

    for (i  in 0 until  BUFFERS) {
        ret = io_uring_wait_cqe(ring.ptr, cqe.ptr);
        if ( cqe.pointed.res  < 0) {
            fprintf(stderr, " cqe.pointed.res =%d\n", cqe.pointed.res );
            return 1;
        }
        io_uring_cqe_seen(ring.ptr, cqe);
    }

    ret = __test_io(filename, ring.ptr, 0, 0, 0, 0, nonvec, 1, 1, exp_len);

    io_uring_queue_exit(ring.ptr);
    return ret;
}

static provide_buffers_iovec:Int(ring:CPointer<io_uring>, bgid:Int) {
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    i:Int, ret;

    for (i  in 0 until  BUFFERS) {
        sqe = io_uring_get_sqe(ring);
        io_uring_prep_provide_buffers(sqe, vecs[i].iov_base,
                                      vecs[i].iov_len, 1, bgid, i);
    }

    ret = io_uring_submit(ring);
    if (ret != BUFFERS) {
        fprintf(stderr, "submit: %d\n", ret);
        return -1;
    }

    for (i  in 0 until  BUFFERS) {
        ret = io_uring_wait_cqe(ring, cqe.ptr);
        if (ret) {
            fprintf(stderr, "wait_cqe=%d\n", ret);
            return 1;
        }
        if ( cqe.pointed.res  < 0) {
            fprintf(stderr, " cqe.pointed.res =%d\n", cqe.pointed.res );
            return 1;
        }
        io_uring_cqe_seen(ring, cqe);
    }

    return 0;
}

static test_buf_select:Int(filename:String, nonvec:Int) {
    p:CPointer<io_uring_probe>;
    ring:io_uring;
    ret:Int, i;

    ret = io_uring_queue_init(64, ring.ptr, 0);
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

    /*
     * Write out data with known pattern
     */
    for (i  in 0 until  BUFFERS)
        memset(vecs[i].iov_base, i, vecs[i].iov_len);

    ret = __test_io(filename, ring.ptr, 1, 0, 0, 0, 0, 0, 1, BS);
    if (ret) {
        fprintf(stderr, "failed writing data\n");
        return 1;
    }

    for (i  in 0 until  BUFFERS)
        memset(vecs[i].iov_base, 0x55, vecs[i].iov_len);

    ret = provide_buffers_iovec(ring.ptr, 1);
    if (ret)
        return ret;

    ret = __test_io(filename, ring.ptr, 0, 0, 0, 0, nonvec, 1, 1, BS);
    io_uring_queue_exit(ring.ptr);
    return ret;
}

static test_rem_buf:Int(batch:Int, sqe_flags:Int) {
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    ring:io_uring;
    left:Int, ret, nr = 0;
    bgid:Int = 1;

    if (no_buf_select)
        return 0;

    ret = io_uring_queue_init(64, ring.ptr, 0);
    if (ret) {
        fprintf(stderr, "ring create failed: %d\n", ret);
        return 1;
    }

    ret = provide_buffers_iovec(ring.ptr, bgid);
    if (ret)
        return ret;

    left = BUFFERS;
    while (left) {
        to_rem:Int = (left < batch) ? left : batch;

        left -= to_rem;
        sqe = io_uring_get_sqe(ring.ptr);
        io_uring_prep_remove_buffers(sqe, to_rem, bgid);
 sqe.pointed.user_data  = to_rem;
 sqe.pointed.flags  |= sqe_flags;
        ++nr;
    }

    ret = io_uring_submit(ring.ptr);
    if (ret != nr) {
        fprintf(stderr, "submit: %d\n", ret);
        return -1;
    }

    for (; nr > 0; nr--) {
        ret = io_uring_wait_cqe(ring.ptr, cqe.ptr);
        if (ret) {
            fprintf(stderr, "wait_cqe=%d\n", ret);
            return 1;
        }
        if ( cqe.pointed.res  != cqe.pointed.user_data ) {
            fprintf(stderr, " cqe.pointed.res =%d\n", cqe.pointed.res );
            return 1;
        }
        io_uring_cqe_seen(ring.ptr, cqe);
    }

    io_uring_queue_exit(ring.ptr);
    return ret;
}

static test_io_link:Int(file:String) {
    const nr_links:Int = 100;
    const link_len:Int = 100;
    const nr_sqes:Int = nr_links * link_len;
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    ring:io_uring;
    i:Int, j, fd, ret;

    fd = open(file, O_WRONLY);
    if (fd < 0) {
        perror("file open");
        goto err;
    }

    ret = io_uring_queue_init(nr_sqes, ring.ptr, 0);
    if (ret) {
        fprintf(stderr, "ring create failed: %d\n", ret);
        goto err;
    }

    for (i  in 0 until  nr_links) {
        for (j  in 0 until  link_len) {
            sqe = io_uring_get_sqe(ring.ptr);
            if (!sqe) {
                fprintf(stderr, "sqe get failed\n");
                goto err;
            }
            io_uring_prep_writev(sqe, fd, vecs.ptr[0], 1, 0);
 sqe.pointed.flags  |= IOSQE_ASYNC;
            if (j != link_len - 1)
 sqe.pointed.flags  |= IOSQE_IO_LINK;
        }
    }

    ret = io_uring_submit(ring.ptr);
    if (ret != nr_sqes) {
        ret = io_uring_peek_cqe(ring.ptr, cqe.ptr);
        if (!ret && cqe.pointed.res  == -EINVAL) {
            fprintf(stdout, "IOSQE_ASYNC not supported, skipped\n");
            goto out;
        }
        fprintf(stderr, "submit got %d, wanted %d\n", ret, nr_sqes);
        goto err;
    }

    for (i  in 0 until  nr_sqes) {
        ret = io_uring_wait_cqe(ring.ptr, cqe.ptr);
        if (ret) {
            fprintf(stderr, "wait_cqe=%d\n", ret);
            goto err;
        }
        if ( cqe.pointed.res  == -EINVAL) {
            if (!warned) {
                fprintf(stdout, "Non-vectored IO not "
                                "supported, skipping\n");
                warned = 1;
                no_read = 1;
            }
        } else if ( cqe.pointed.res  != BS) {
            fprintf(stderr, "cqe res %d, wanted %d\n", cqe.pointed.res , BS);
            goto err;
        }
        io_uring_cqe_seen(ring.ptr, cqe);
    }

    out:
    io_uring_queue_exit(ring.ptr);
    close(fd);
    return 0;
    err:
    if (fd != -1)
        close(fd);
    return 1;
}

static test_write_efbig:Int(void) {
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    ring:io_uring;
    rlim:rlimit, old_rlim;
    i:Int, fd, ret;
    off:loff_t;

    if (geteuid()) {
        fprintf(stdout, "Not root, skipping %s\n", __FUNCTION__);
        return 0;
    }

    if (getrlimit(RLIMIT_FSIZE, old_rlim.ptr) < 0) {
        perror("getrlimit");
        return 1;
    }
    rlim = old_rlim;
    rlim.rlim_cur = 64 * 1024;
    rlim.rlim_max = 64 * 1024;
    if (setrlimit(RLIMIT_FSIZE, rlim.ptr) < 0) {
        perror("setrlimit");
        return 1;
    }

    fd = open(".efbig",  O_WRONLY or O_CREAT , 0644);
    if (fd < 0) {
        perror("file open");
        goto err;
    }
    unlink(".efbig");

    ret = io_uring_queue_init(32, ring.ptr, 0);
    if (ret) {
        fprintf(stderr, "ring create failed: %d\n", ret);
        goto err;
    }

    off = 0;
    for (i  in 0 until  32) {
        sqe = io_uring_get_sqe(ring.ptr);
        if (!sqe) {
            fprintf(stderr, "sqe get failed\n");
            goto err;
        }
        io_uring_prep_writev(sqe, fd, vecs.ptr[i], 1, off);
        off += BS;
    }

    ret = io_uring_submit(ring.ptr);
    if (ret != 32) {
        fprintf(stderr, "submit got %d, wanted %d\n", ret, 32);
        goto err;
    }

    for (i  in 0 until  32) {
        ret = io_uring_wait_cqe(ring.ptr, cqe.ptr);
        if (ret) {
            fprintf(stderr, "wait_cqe=%d\n", ret);
            goto err;
        }
        if (i < 16) {
            if ( cqe.pointed.res  != BS) {
                fprintf(stderr, "bad write: %d\n", cqe.pointed.res );
                goto err;
            }
        } else {
            if ( cqe.pointed.res  != -EFBIG) {
                fprintf(stderr, "Expected -EFBIG: %d\n", cqe.pointed.res );
                goto err;
            }
        }
        io_uring_cqe_seen(ring.ptr, cqe);
    }

    io_uring_queue_exit(ring.ptr);
    close(fd);
    unlink(".efbig");

    if (setrlimit(RLIMIT_FSIZE, old_rlim.ptr) < 0) {
        perror("setrlimit");
        return 1;
    }
    return 0;
    err:
    if (fd != -1)
        close(fd);
    return 1;
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    i:Int, ret, nr;
    char buf[256];
    fname:CPointer<ByteVar>;

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

    /* if we don't have nonvec read, skip testing that */
    nr = has_nonvec_read() ? 32 : 16;

    for (i  in 0 until  nr) {
        write:Int = (i 1.ptr) != 0;
        buffered:Int = (i 2.ptr) != 0;
        sqthread:Int = (i 4.ptr) != 0;
        fixed:Int = (i 8.ptr) != 0;
        nonvec:Int = (i 16.ptr) != 0;

        ret = test_io(fname, write, buffered, sqthread, fixed, nonvec,
                      BS);
        if (ret) {
            fprintf(stderr, "test_io failed %d/%d/%d/%d/%d\n",
                    write, buffered, sqthread, fixed, nonvec);
            goto err;
        }
    }

    ret = test_buf_select(fname, 1);
    if (ret) {
        fprintf(stderr, "test_buf_select nonvec failed\n");
        goto err;
    }

    ret = test_buf_select(fname, 0);
    if (ret) {
        fprintf(stderr, "test_buf_select vec failed\n");
        goto err;
    }

    ret = test_buf_select_short(fname, 1);
    if (ret) {
        fprintf(stderr, "test_buf_select_short nonvec failed\n");
        goto err;
    }

    ret = test_buf_select_short(fname, 0);
    if (ret) {
        fprintf(stderr, "test_buf_select_short vec failed\n");
        goto err;
    }

    ret = test_eventfd_read();
    if (ret) {
        fprintf(stderr, "test_eventfd_read failed\n");
        goto err;
    }

    ret = read_poll_link(fname);
    if (ret) {
        fprintf(stderr, "read_poll_link failed\n");
        goto err;
    }

    ret = test_io_link(fname);
    if (ret) {
        fprintf(stderr, "test_io_link failed\n");
        goto err;
    }

    ret = test_write_efbig();
    if (ret) {
        fprintf(stderr, "test_write_efbig failed\n");
        goto err;
    }

    ret = test_rem_buf(1, 0);
    if (ret) {
        fprintf(stderr, "test_rem_buf by 1 failed\n");
        goto err;
    }

    ret = test_rem_buf(10, 0);
    if (ret) {
        fprintf(stderr, "test_rem_buf by 10 failed\n");
        goto err;
    }

    ret = test_rem_buf(2, IOSQE_IO_LINK);
    if (ret) {
        fprintf(stderr, "test_rem_buf link failed\n");
        goto err;
    }

    ret = test_rem_buf(2, IOSQE_ASYNC);
    if (ret) {
        fprintf(stderr, "test_rem_buf async failed\n");
        goto err;
    }

    srand((unsigned) time(NULL));
    if (create_nonaligned_buffers()) {
        fprintf(stderr, "file creation failed\n");
        goto err;
    }

    /* test fixed bufs with non-aligned len/offset */
    for (i  in 0 until  nr) {
        write:Int = (i 1.ptr) != 0;
        buffered:Int = (i 2.ptr) != 0;
        sqthread:Int = (i 4.ptr) != 0;
        fixed:Int = (i 8.ptr) != 0;
        nonvec:Int = (i 16.ptr) != 0;

        /* direct IO requires alignment, skip it */
        if (!buffered || !fixed || nonvec)
            continue;

        ret = test_io(fname, write, buffered, sqthread, fixed, nonvec,
                      -1);
        if (ret) {
            fprintf(stderr, "test_io failed %d/%d/%d/%d/%d\n",
                    write, buffered, sqthread, fixed, nonvec);
            goto err;
        }
    }

    if (fname != argv[1])
        unlink(fname);
    return 0;
    err:
    if (fname != argv[1])
        unlink(fname);
    return 1;
}
