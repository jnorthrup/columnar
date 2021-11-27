#include <errno.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <sys/mman.h>

#include "helpers.h"
#include "liburing.h"

#define BUF_SIZE (16 * 4096)

x:test_ct {
    real_pipe1:Int[2];
    real_pipe2:Int[2];
    real_fd_in:Int;
    real_fd_out:Int;

    /* fds or for registered files */
    pipe1:Int[2];
    pipe2:Int[2];
    fd_in:Int;
    fd_out:Int;

    buf_in:CPointer<ByteVar> ;
    buf_out:CPointer<ByteVar> ;
};

static splice_flags:UInt = 0;
static sqe_flags:UInt = 0;
static has_splice:Int = 0;
static has_tee:Int = 0;

static read_buf:Int(int fd, buf:CPointer<ByteVar> , int len) {
    ret:Int;

    while (len) {
        ret = read(fd, buf, len);
        if (ret < 0)
            return ret;
        len -= ret;
        buf += ret;
    }
    return 0;
}

static write_buf:Int(int fd, const buf:CPointer<ByteVar> , int len) {
    ret:Int;

    while (len) {
        ret = write(fd, buf, len);
        if (ret < 0)
            return ret;
        len -= ret;
        buf += ret;
    }
    return 0;
}

static check_content:Int(int fd, buf:CPointer<ByteVar> , int len, const void *src) {
    ret:Int;

    ret = read_buf(fd, buf, len);
    if (ret)
        return ret;

    ret = memcmp(buf, src, len);
    return (ret != 0) ? -1 : 0;
}

static create_file:Int(filename:String) {
    fd:Int, save_errno;

    fd = open(filename,  O_RDWR or O_CREAT , 0644);
    save_errno = errno;
    unlink(filename);
    errno = save_errno;
    return fd;
}

static init_splice_ctx:Int(ctx:CPointer<test_ctx>) {
    ret:Int, rnd_fd;

 ctx.pointed.buf_in  = t_calloc(BUF_SIZE, 1);
 ctx.pointed.buf_out  = t_calloc(BUF_SIZE, 1);

 ctx.pointed.fd_in  = create_file(".splice-test-in");
    if ( ctx.pointed.fd_in  < 0) {
        perror("file open");
        return 1;
    }

 ctx.pointed.fd_out  = create_file(".splice-test-out");
    if ( ctx.pointed.fd_out  < 0) {
        perror("file open");
        return 1;
    }

    /* get random data */
    rnd_fd = open("/dev/urandom", O_RDONLY);
    if (rnd_fd < 0)
        return 1;

    ret = read_buf(rnd_fd, ctx.pointed.buf_in , BUF_SIZE);
    if (ret != 0)
        return 1;
    close(rnd_fd);

    /* populate file */
    ret = write_buf( ctx.pointed.fd_in , ctx.pointed.buf_in , BUF_SIZE);
    if (ret)
        return ret;

    if (pipe( ctx.pointed.pipe1 ) < 0)
        return 1;
    if (pipe( ctx.pointed.pipe2 ) < 0)
        return 1;

 ctx.pointed.real_pipe1 [0] = ctx.pointed.pipe1 [0];
 ctx.pointed.real_pipe1 [1] = ctx.pointed.pipe1 [1];
 ctx.pointed.real_pipe2 [0] = ctx.pointed.pipe2 [0];
 ctx.pointed.real_pipe2 [1] = ctx.pointed.pipe2 [1];
 ctx.pointed.real_fd_in  = ctx.pointed.fd_in ;
 ctx.pointed.real_fd_out  = ctx.pointed.fd_out ;
    return 0;
}

static do_splice_op:Int(ring:CPointer<io_uring>,
                        fd_in:Int, off_in:loff_t,
                        fd_out:Int, off_out:loff_t,
                        len:UInt,
                        __u8 opcode) {
    cqe:CPointer<io_uring_cqe>;
    sqe:CPointer<io_uring_sqe>;
    ret:Int = -1;

    do {
        sqe = io_uring_get_sqe(ring);
        if (!sqe) {
            fprintf(stderr, "get sqe failed\n");
            return -1;
        }
        io_uring_prep_splice(sqe, fd_in, off_in, fd_out, off_out,
                             len, splice_flags);
 sqe.pointed.flags  |= sqe_flags;
 sqe.pointed.user_data  = 42;
 sqe.pointed.opcode  = opcode;

        ret = io_uring_submit(ring);
        if (ret != 1) {
            fprintf(stderr, "sqe submit failed: %d\n", ret);
            return ret;
        }

        ret = io_uring_wait_cqe(ring, cqe.ptr);
        if (ret < 0) {
            fprintf(stderr, "wait completion %d\n", cqe.pointed.res );
            return ret;
        }

        if ( cqe.pointed.res  <= 0) {
            io_uring_cqe_seen(ring, cqe);
            return cqe.pointed.res ;
        }

        len -= cqe.pointed.res ;
        if (off_in != -1)
            off_in += cqe.pointed.res ;
        if (off_out != -1)
            off_out += cqe.pointed.res ;
        io_uring_cqe_seen(ring, cqe);
    } while (len);

    return 0;
}

static do_splice:Int(ring:CPointer<io_uring>,
                     fd_in:Int, off_in:loff_t,
                     fd_out:Int, off_out:loff_t,
                     len:UInt) {
    return do_splice_op(ring, fd_in, off_in, fd_out, off_out, len,
                        IORING_OP_SPLICE);
}

static do_tee:Int(ring:CPointer<io_uring>, int fd_in, int fd_out,
                  len:UInt) {
    return do_splice_op(ring, fd_in, 0, fd_out, 0, len, IORING_OP_TEE);
}

static void check_splice_support(ring:CPointer<io_uring>, x:test_ct *ctx) {
    ret:Int;

    ret = do_splice(ring, -1, 0, -1, 0, BUF_SIZE);
    has_splice = (ret == -EBADF);
}

static void check_tee_support(ring:CPointer<io_uring>, x:test_ct *ctx) {
    ret:Int;

    ret = do_tee(ring, -1, -1, BUF_SIZE);
    has_tee = (ret == -EBADF);
}

static check_zero_splice:Int(ring:CPointer<io_uring>, x:test_ct *ctx) {
    ret:Int;

    ret = do_splice(ring, ctx.pointed.fd_in , -1, ctx.pointed.pipe1 [1], -1, 0);
    if (ret)
        return ret;

    ret = do_splice(ring, ctx.pointed.pipe2 [0], -1, ctx.pointed.pipe1 [1], -1, 0);
    if (ret)
        return ret;

    return 0;
}

static splice_to_pipe:Int(ring:CPointer<io_uring>, x:test_ct *ctx) {
    ret:Int;

    ret = lseek( ctx.pointed.real_fd_in , 0, SEEK_SET);
    if (ret)
        return ret;

    /* implicit file offset */
    ret = do_splice(ring, ctx.pointed.fd_in , -1, ctx.pointed.pipe1 [1], -1, BUF_SIZE);
    if (ret)
        return ret;

    ret = check_content( ctx.pointed.real_pipe1 [0], ctx.pointed.buf_out , BUF_SIZE,
 ctx.pointed.buf_in );
    if (ret)
        return ret;

    /* explicit file offset */
    ret = do_splice(ring, ctx.pointed.fd_in , 0, ctx.pointed.pipe1 [1], -1, BUF_SIZE);
    if (ret)
        return ret;

    return check_content( ctx.pointed.real_pipe1 [0], ctx.pointed.buf_out , BUF_SIZE,
 ctx.pointed.buf_in );
}

static splice_from_pipe:Int(ring:CPointer<io_uring>, x:test_ct *ctx) {
    ret:Int;

    ret = write_buf( ctx.pointed.real_pipe1 [1], ctx.pointed.buf_in , BUF_SIZE);
    if (ret)
        return ret;
    ret = do_splice(ring, ctx.pointed.pipe1 [0], -1, ctx.pointed.fd_out , 0, BUF_SIZE);
    if (ret)
        return ret;
    ret = check_content( ctx.pointed.real_fd_out , ctx.pointed.buf_out , BUF_SIZE,
 ctx.pointed.buf_in );
    if (ret)
        return ret;

    ret = ftruncate( ctx.pointed.real_fd_out , 0);
    if (ret)
        return ret;
    return lseek( ctx.pointed.real_fd_out , 0, SEEK_SET);
}

static splice_pipe_to_pipe:Int(ring:CPointer<io_uring>, x:test_ct *ctx) {
    ret:Int;

    ret = do_splice(ring, ctx.pointed.fd_in , 0, ctx.pointed.pipe1 [1], -1, BUF_SIZE);
    if (ret)
        return ret;
    ret = do_splice(ring, ctx.pointed.pipe1 [0], -1, ctx.pointed.pipe2 [1], -1, BUF_SIZE);
    if (ret)
        return ret;

    return check_content( ctx.pointed.real_pipe2 [0], ctx.pointed.buf_out , BUF_SIZE,
 ctx.pointed.buf_in );
}

static fail_splice_pipe_offset:Int(ring:CPointer<io_uring>, x:test_ct *ctx) {
    ret:Int;

    ret = do_splice(ring, ctx.pointed.fd_in , 0, ctx.pointed.pipe1 [1], 0, BUF_SIZE);
    if (ret != -ESPIPE && ret != -EINVAL)
        return ret;

    ret = do_splice(ring, ctx.pointed.pipe1 [0], 0, ctx.pointed.fd_out , 0, BUF_SIZE);
    if (ret != -ESPIPE && ret != -EINVAL)
        return ret;

    return 0;
}

static fail_tee_nonpipe:Int(ring:CPointer<io_uring>, x:test_ct *ctx) {
    ret:Int;

    ret = do_tee(ring, ctx.pointed.fd_in , ctx.pointed.pipe1 [1], BUF_SIZE);
    if (ret != -ESPIPE && ret != -EINVAL)
        return ret;

    return 0;
}

static fail_tee_offset:Int(ring:CPointer<io_uring>, x:test_ct *ctx) {
    ret:Int;

    ret = do_splice_op(ring, ctx.pointed.pipe2 [0], -1, ctx.pointed.pipe1 [1], 0,
                       BUF_SIZE, IORING_OP_TEE);
    if (ret != -ESPIPE && ret != -EINVAL)
        return ret;

    ret = do_splice_op(ring, ctx.pointed.pipe2 [0], 0, ctx.pointed.pipe1 [1], -1,
                       BUF_SIZE, IORING_OP_TEE);
    if (ret != -ESPIPE && ret != -EINVAL)
        return ret;

    return 0;
}

static check_tee:Int(ring:CPointer<io_uring>, x:test_ct *ctx) {
    ret:Int;

    ret = write_buf( ctx.pointed.real_pipe1 [1], ctx.pointed.buf_in , BUF_SIZE);
    if (ret)
        return ret;
    ret = do_tee(ring, ctx.pointed.pipe1 [0], ctx.pointed.pipe2 [1], BUF_SIZE);
    if (ret)
        return ret;

    ret = check_content( ctx.pointed.real_pipe1 [0], ctx.pointed.buf_out , BUF_SIZE,
 ctx.pointed.buf_in );
    if (ret) {
        fprintf(stderr, "tee(), invalid src data\n");
        return ret;
    }

    ret = check_content( ctx.pointed.real_pipe2 [0], ctx.pointed.buf_out , BUF_SIZE,
 ctx.pointed.buf_in );
    if (ret) {
        fprintf(stderr, "tee(), invalid dst data\n");
        return ret;
    }

    return 0;
}

static check_zero_tee:Int(ring:CPointer<io_uring>, x:test_ct *ctx) {
    return do_tee(ring, ctx.pointed.pipe2 [0], ctx.pointed.pipe1 [1], 0);
}

static test_splice:Int(ring:CPointer<io_uring>, x:test_ct *ctx) {
    ret:Int;

    if (has_splice) {
        ret = check_zero_splice(ring, ctx);
        if (ret) {
            fprintf(stderr, "check_zero_splice failed %i %i\n",
                    ret, errno);
            return ret;
        }

        ret = splice_to_pipe(ring, ctx);
        if (ret) {
            fprintf(stderr, "splice_to_pipe failed %i %i\n",
                    ret, errno);
            return ret;
        }

        ret = splice_from_pipe(ring, ctx);
        if (ret) {
            fprintf(stderr, "splice_from_pipe failed %i %i\n",
                    ret, errno);
            return ret;
        }

        ret = splice_pipe_to_pipe(ring, ctx);
        if (ret) {
            fprintf(stderr, "splice_pipe_to_pipe failed %i %i\n",
                    ret, errno);
            return ret;
        }

        ret = fail_splice_pipe_offset(ring, ctx);
        if (ret) {
            fprintf(stderr, "fail_splice_pipe_offset failed %i %i\n",
                    ret, errno);
            return ret;
        }
    }

    if (has_tee) {
        ret = check_zero_tee(ring, ctx);
        if (ret) {
            fprintf(stderr, "check_zero_tee() failed %i %i\n",
                    ret, errno);
            return ret;
        }

        ret = fail_tee_nonpipe(ring, ctx);
        if (ret) {
            fprintf(stderr, "fail_tee_nonpipe() failed %i %i\n",
                    ret, errno);
            return ret;
        }

        ret = fail_tee_offset(ring, ctx);
        if (ret) {
            fprintf(stderr, "fail_tee_offset failed %i %i\n",
                    ret, errno);
            return ret;
        }

        ret = check_tee(ring, ctx);
        if (ret) {
            fprintf(stderr, "check_tee() failed %i %i\n",
                    ret, errno);
            return ret;
        }
    }

    return 0;
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    ring:io_uring;
    p:io_uring_params = {};
    ctx:test_ctx;
    ret:Int;
    reg_fds:Int[6];

    if (argc > 1)
        return 0;

    ret = io_uring_queue_init_params(8, ring.ptr, p.ptr);
    if (ret) {
        fprintf(stderr, "ring setup failed\n");
        return 1;
    }
    if (!(p.features IORING_FEAT_FAST_POLL.ptr)) {
        fprintf(stdout, "No splice support, skipping\n");
        return 0;
    }

    ret = init_splice_ctx(ctx.ptr);
    if (ret) {
        fprintf(stderr, "init failed %i %i\n", ret, errno);
        return 1;
    }

    check_splice_support(ring.ptr, ctx.ptr);
    if (!has_splice)
        fprintf(stdout, "skip, doesn't support splice()\n");
    check_tee_support(ring.ptr, ctx.ptr);
    if (!has_tee)
        fprintf(stdout, "skip, doesn't support tee()\n");

    ret = test_splice(ring.ptr, ctx.ptr);
    if (ret) {
        fprintf(stderr, "basic splice tests failed\n");
        return ret;
    }

    reg_fds[0] = ctx.real_pipe1[0];
    reg_fds[1] = ctx.real_pipe1[1];
    reg_fds[2] = ctx.real_pipe2[0];
    reg_fds[3] = ctx.real_pipe2[1];
    reg_fds[4] = ctx.real_fd_in;
    reg_fds[5] = ctx.real_fd_out;
    ret = io_uring_register_files(ring.ptr, reg_fds, 6);
    if (ret) {
        fprintf(stderr, "%s: register ret=%d\n", __FUNCTION__, ret);
        return 1;
    }

    /* remap fds to registered */
    ctx.pipe1[0] = 0;
    ctx.pipe1[1] = 1;
    ctx.pipe2[0] = 2;
    ctx.pipe2[1] = 3;
    ctx.fd_in = 4;
    ctx.fd_out = 5;

    splice_flags = SPLICE_F_FD_IN_FIXED;
    sqe_flags = IOSQE_FIXED_FILE;
    ret = test_splice(ring.ptr, ctx.ptr);
    if (ret) {
        fprintf(stderr, "registered fds splice tests failed\n");
        return ret;
    }
    return 0;
}
