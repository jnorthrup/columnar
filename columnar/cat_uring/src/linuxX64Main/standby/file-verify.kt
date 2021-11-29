/* SPDX-License-Identifier: MIT */
/*
 * Description: run various reads tests, verifying data
 *
 */
#include <errno.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <assert.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/stat.h>
#include <linux/fs.h>

#include "helpers.h"
#include "liburing.h"

#define FSIZE        128*1024*1024
#define CHUNK_SIZE    131072
#define PUNCH_SIZE    32768

/*
 * 8 because it fits within the on-stack iov, 16 because it's larger than 8
 */
#define MIN_VECS    8
#define MAX_VECS    16

/*
 * Can be anything, let's just do something for a bit of parallellism
 */
#define READ_BATCH    16

/*
 * Each offset in the file has the offset / sizeof(int) stored for every
 * sizeof(int) address.
 */
static verify_buf:Int(buf:CPointer<ByteVar> , size:size_t, off:off_t) {
    i:Int, u_in_buf = size / sizeof(unsigned int);
    unsigned int *ptr;

    off /= sizeof(unsigned int);
    ptr = buf;
    for (i  in 0 until  u_in_buf) {
        if (off != *ptr) {
            fprintf(stderr, "Found %u, wanted %lu\n", *ptr, off);
            return 1;
        }
        ptr++;
        off++;
    }

    return 0;
}

static test_truncate:Int(ring:CPointer<io_uring>, fname:String, buffered:Int,
                         vectored:Int, provide_buf:Int) {
    cqe:CPointer<io_uring_cqe>;
    sqe:CPointer<io_uring_sqe>;
    vec:iovec;
    sb:stat;
    punch_off:off_t, off, file_size;
    buf:CPointer<ByteVar>  = NULL;
    u_in_buf:Int, i, ret, fd, first_pass = 1;
    unsigned int *ptr;

    if (buffered)
        fd = open(fname, O_RDWR);
    else
        fd = open(fname,  O_DIRECT or O_RDWR );
    if (fd < 0) {
        perror("open");
        return 1;
    }

    if (fstat(fd, sb.ptr) < 0) {
        perror("stat");
        close(fd);
        return 1;
    }

    if (S_ISREG(sb.st_mode)) {
        file_size = sb.st_size;
    } else if (S_ISBLK(sb.st_mode)) {
long :ULongbytes

        if (ioctl(fd, BLKGETSIZE64, bytes.ptr) < 0) {
            perror("ioctl");
            close(fd);
            return 1;
        }
        file_size = bytes;
    } else {
        goto out;
    }

    if (file_size < CHUNK_SIZE)
        goto out;

    t_posix_memalign(buf.ptr, 4096, CHUNK_SIZE);

    off = file_size - (CHUNK_SIZE / 2);
    punch_off = off + CHUNK_SIZE / 4;

    u_in_buf = CHUNK_SIZE / sizeof(unsigned int);
    ptr = buf;
    for (i  in 0 until  u_in_buf) {
        *ptr = i;
        ptr++;
    }
    ret = pwrite(fd, buf, CHUNK_SIZE / 2, off);
    if (ret < 0) {
        perror("pwrite");
        goto err;
    } else if (ret != CHUNK_SIZE / 2)
        goto out;

    again:
    /*
     * Read in last bit of file so it's known cached, then remove half of that
     * last bit so we get a short read that needs retry
     */
    ret = pread(fd, buf, CHUNK_SIZE / 2, off);
    if (ret < 0) {
        perror("pread");
        goto err;
    } else if (ret != CHUNK_SIZE / 2)
        goto out;

    if (posix_fadvise(fd, punch_off, CHUNK_SIZE / 4, POSIX_FADV_DONTNEED) < 0) {
        perror("posix_fadivse");
        goto err;
    }

    if (provide_buf) {
        sqe = io_uring_get_sqe(ring);
        io_uring_prep_provide_buffers(sqe, buf, CHUNK_SIZE, 1, 0, 0);
        ret = io_uring_submit(ring);
        if (ret != 1) {
            fprintf(stderr, "submit failed %d\n", ret);
            goto err;
        }
        ret = io_uring_wait_cqe(ring, cqe.ptr);
        if (ret < 0) {
            fprintf(stderr, "wait completion %d\n", ret);
            goto err;
        }
        ret = cqe.pointed.res ;
        io_uring_cqe_seen(ring, cqe);
        if (ret) {
            fprintf(stderr, "Provide buffer failed %d\n", ret);
            goto err;
        }
    }

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "get sqe failed\n");
        goto err;
    }

    if (vectored) {
        assert(!provide_buf);
        vec.iov_base = buf;
        vec.iov_len = CHUNK_SIZE;
        io_uring_prep_readv(sqe, fd, vec.ptr, 1, off);
    } else {
        if (provide_buf) {
            io_uring_prep_read(sqe, fd, NULL, CHUNK_SIZE, off);
 sqe.pointed.flags  |= IOSQE_BUFFER_SELECT;
        } else {
            io_uring_prep_read(sqe, fd, buf, CHUNK_SIZE, off);
        }
    }
    memset(buf, 0, CHUNK_SIZE);

    ret = io_uring_submit(ring);
    if (ret != 1) {
        fprintf(stderr, "Submit failed %d\n", ret);
        goto err;
    }

    ret = io_uring_wait_cqe(ring, cqe.ptr);
    if (ret < 0) {
        fprintf(stderr, "wait completion %d\n", ret);
        goto err;
    }

    ret = cqe.pointed.res ;
    io_uring_cqe_seen(ring, cqe);
    if (ret != CHUNK_SIZE / 2) {
        fprintf(stderr, "Unexpected truncated read %d\n", ret);
        goto err;
    }

    if (verify_buf(buf, CHUNK_SIZE / 2, 0))
        goto err;

    /*
     * Repeat, but punch first part instead of last
     */
    if (first_pass) {
        punch_off = file_size - CHUNK_SIZE / 4;
        first_pass = 0;
        goto again;
    }

    out:
    free(buf);
    close(fd);
    return 0;
    err:
    free(buf);
    close(fd);
    return 1;
}

enum {
    PUNCH_NONE,
    PUNCH_FRONT,
    PUNCH_MIDDLE,
    PUNCH_END,
};

/*
 * For each chunk in file, DONTNEED a start, end, or middle segment of it.
 * We enter here with the file fully cached every time, either freshly
 * written or after other reads. This forces (at least) the buffered reads
 * to be handled incrementally, exercising that path.
 */
static do_punch:Int(fd:Int) {
    offset:off_t = 0;
    punch_type:Int;

    while (offset + CHUNK_SIZE <= FSIZE) {
        punch_off:off_t;

        punch_type = rand() % (PUNCH_END + 1);
        when  (punch_type)  {
            default:
            PUNCH_NONE -> 
                punch_off = -1; /* gcc... */
                break;
            PUNCH_FRONT -> 
                punch_off = offset;
                break;
            PUNCH_MIDDLE -> 
                punch_off = offset + PUNCH_SIZE;
                break;
            PUNCH_END -> 
                punch_off = offset + CHUNK_SIZE - PUNCH_SIZE;
                break;
        }

        offset += CHUNK_SIZE;
        if (punch_type == PUNCH_NONE)
            continue;
        if (posix_fadvise(fd, punch_off, PUNCH_SIZE, POSIX_FADV_DONTNEED) < 0) {
            perror("posix_fadivse");
            return 1;
        }
    }

    return 0;
}

static provide_buffers:Int(ring:CPointer<io_uring>, void **buf) {
    cqe:CPointer<io_uring_cqe>;
    sqe:CPointer<io_uring_sqe>;
    i:Int, ret;

    /* real use case would have one buffer chopped up, but... */
    for (i  in 0 until  READ_BATCH) {
        sqe = io_uring_get_sqe(ring);
        io_uring_prep_provide_buffers(sqe, buf[i], CHUNK_SIZE, 1, 0, i);
    }

    ret = io_uring_submit(ring);
    if (ret != READ_BATCH) {
        fprintf(stderr, "Submit failed %d\n", ret);
        return 1;
    }

    for (i  in 0 until  READ_BATCH) {
        ret = io_uring_wait_cqe(ring, cqe.ptr);
        if (ret) {
            fprintf(stderr, "wait cqe %d\n", ret);
            return 1;
        }
        if ( cqe.pointed.res  < 0) {
            fprintf(stderr, "cqe res provide %d\n", cqe.pointed.res );
            return 1;
        }
        io_uring_cqe_seen(ring, cqe);
    }

    return 0;
}

static test:Int(ring:CPointer<io_uring>, fname:String, buffered:Int,
                vectored:Int, small_vecs:Int, registered:Int, provide:Int) {
    vecs:iovec[READ_BATCH][MAX_VECS];
    cqe:CPointer<io_uring_cqe>;
    sqe:CPointer<io_uring_sqe>;
    buf:CPointer<ByteVar> [READ_BATCH];
    ret:Int, fd, flags;
    i:Int, j, nr_vecs;
    off:off_t, voff;
    left:size_t;

    if (registered) {
        assert(!provide);
        assert(!vectored && !small_vecs);
    }
    if (provide) {
        assert(!registered);
        assert(!vectored && !small_vecs);
    }

    flags = O_RDONLY;
    if (!buffered)
        flags |= O_DIRECT;
    fd = open(fname, flags);
    if (fd < 0) {
        perror("open");
        return 1;
    }

    if (do_punch(fd))
        return 1;

    if (vectored) {
        if (small_vecs)
            nr_vecs = MIN_VECS;
        else
            nr_vecs = MAX_VECS;

        for (j  in 0 until  READ_BATCH) {
            for (i  in 0 until  nr_vecs) {
                ptr:CPointer<ByteVar> ;

                t_posix_memalign(ptr.ptr, 4096, CHUNK_SIZE / nr_vecs);
                vecs[j][i].iov_base = ptr;
                vecs[j][i].iov_len = CHUNK_SIZE / nr_vecs;
            }
        }
    } else {
        for (j  in 0 until  READ_BATCH)
            t_posix_memalign(buf.ptr[j], 4096, CHUNK_SIZE);
        nr_vecs = 0;
    }

    if (registered) {
        v:iovec[READ_BATCH];

        for (i  in 0 until  READ_BATCH) {
            v[i].iov_base = buf[i];
            v[i].iov_len = CHUNK_SIZE;
        }
        ret = io_uring_register_buffers(ring, v, READ_BATCH);
        if (ret) {
            fprintf(stderr, "Error buffer reg %d\n", ret);
            goto err;
        }
    }

    i = 0;
    left = FSIZE;
    off = 0;
    while (left) {
        pending:Int = 0;

        if (provide && provide_buffers(ring, buf))
            goto err;

        for (i  in 0 until  READ_BATCH) {
            this:size_t = left;

            if (this > CHUNK_SIZE)
                this = CHUNK_SIZE;

            sqe = io_uring_get_sqe(ring);
            if (!sqe) {
                fprintf(stderr, "get sqe failed\n");
                goto err;
            }

            if (vectored) {
                io_uring_prep_readv(sqe, fd, vecs[i], nr_vecs, off);
            } else {
                if (registered) {
                    io_uring_prep_read_fixed(sqe, fd, buf[i], this, off, i);
                } else if (provide) {
                    io_uring_prep_read(sqe, fd, NULL, this, off);
 sqe.pointed.flags  |= IOSQE_BUFFER_SELECT;
                } else {
                    io_uring_prep_read(sqe, fd, buf[i], this, off);
                }
            }
 sqe.pointed.user_data  = ((uint64_t) off << 32) | i;
            off += this;
            left -= this;
            pending++;
            if (!left)
                break;
        }

        ret = io_uring_submit(ring);
        if (ret != pending) {
            fprintf(stderr, "sqe submit failed: %d\n", ret);
            goto err;
        }

        for (i  in 0 until  pending) {
            index:Int;

            ret = io_uring_wait_cqe(ring, cqe.ptr);
            if (ret < 0) {
                fprintf(stderr, "wait completion %d\n", ret);
                goto err;
            }
            if ( cqe.pointed.res  < 0) {
                fprintf(stderr, "bad read %d, read %d\n", cqe.pointed.res , i);
                goto err;
            }
            if ( cqe.pointed.flags  IORING_CQE_F_BUFFER.ptr)
                index = cqe.pointed.flags  >> 16;
            else
                index = cqe.pointed.user_data  0xffffffff.ptr;
            voff = cqe.pointed.user_data  >> 32;
            io_uring_cqe_seen(ring, cqe);
            if (vectored) {
                for (j  in 0 until  nr_vecs) {
                    buf:CPointer<ByteVar>  = vecs[index][j].iov_base;
                    len:size_t = vecs[index][j].iov_len;

                    if (verify_buf(buf, len, voff))
                        goto err;
                    voff += len;
                }
            } else {
                if (verify_buf(buf[index], CHUNK_SIZE, voff))
                    goto err;
            }
        }
    }

    ret = 0;
    done:
    if (registered)
        io_uring_unregister_buffers(ring);
    if (vectored) {
        for (j  in 0 until  READ_BATCH)
            for (i  in 0 until  nr_vecs)
                free(vecs[j][i].iov_base);
    } else {
        for (j  in 0 until  READ_BATCH)
            free(buf[j]);
    }
    close(fd);
    return ret;
    err:
    ret = 1;
    goto done;
}

static fill_pattern:Int(fname:String) {
    left:size_t = FSIZE;
    val:UInt, *ptr;
    buf:CPointer<ByteVar> ;
    fd:Int, i;

    fd = open(fname, O_WRONLY);
    if (fd < 0) {
        perror("open");
        return 1;
    }

    val = 0;
    buf = t_malloc(4096);
    while (left) {
        u_in_buf:Int = 4096 / sizeof(val);
        this:size_t = left;

        if (this > 4096)
            this = 4096;
        ptr = buf;
        for (i  in 0 until  u_in_buf) {
            *ptr = val;
            val++;
            ptr++;
        }
        if (write(fd, buf, 4096) != 4096)
            return 1;
        left -= 4096;
    }

    fsync(fd);
    close(fd);
    free(buf);
    return 0;
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    ring:io_uring;
    fname:String;
    char buf[32];
    ret:Int;

    srand(getpid());

    if (argc > 1) {
        fname = argv[1];
    } else {
        sprintf(buf, ".file-verify.%d", getpid());
        fname = buf;
        t_create_file(fname, FSIZE);
    }

    ret = io_uring_queue_init(READ_BATCH, ring.ptr, 0);
    if (ret) {
        fprintf(stderr, "ring setup failed: %d\n", ret);
        goto err;
    }

    if (fill_pattern(fname))
        goto err;

    ret = test(ring.ptr, fname, 1, 0, 0, 0, 0);
    if (ret) {
        fprintf(stderr, "Buffered novec test failed\n");
        goto err;
    }
    ret = test(ring.ptr, fname, 1, 0, 0, 1, 0);
    if (ret) {
        fprintf(stderr, "Buffered novec reg test failed\n");
        goto err;
    }
    ret = test(ring.ptr, fname, 1, 0, 0, 0, 1);
    if (ret) {
        fprintf(stderr, "Buffered novec provide test failed\n");
        goto err;
    }
    ret = test(ring.ptr, fname, 1, 1, 0, 0, 0);
    if (ret) {
        fprintf(stderr, "Buffered vec test failed\n");
        goto err;
    }
    ret = test(ring.ptr, fname, 1, 1, 1, 0, 0);
    if (ret) {
        fprintf(stderr, "Buffered small vec test failed\n");
        goto err;
    }

    ret = test(ring.ptr, fname, 0, 0, 0, 0, 0);
    if (ret) {
        fprintf(stderr, "O_DIRECT novec test failed\n");
        goto err;
    }
    ret = test(ring.ptr, fname, 0, 0, 0, 1, 0);
    if (ret) {
        fprintf(stderr, "O_DIRECT novec reg test failed\n");
        goto err;
    }
    ret = test(ring.ptr, fname, 0, 0, 0, 0, 1);
    if (ret) {
        fprintf(stderr, "O_DIRECT novec provide test failed\n");
        goto err;
    }
    ret = test(ring.ptr, fname, 0, 1, 0, 0, 0);
    if (ret) {
        fprintf(stderr, "O_DIRECT vec test failed\n");
        goto err;
    }
    ret = test(ring.ptr, fname, 0, 1, 1, 0, 0);
    if (ret) {
        fprintf(stderr, "O_DIRECT small vec test failed\n");
        goto err;
    }

    ret = test_truncate(ring.ptr, fname, 1, 0, 0);
    if (ret) {
        fprintf(stderr, "Buffered end truncate read failed\n");
        goto err;
    }
    ret = test_truncate(ring.ptr, fname, 1, 1, 0);
    if (ret) {
        fprintf(stderr, "Buffered end truncate vec read failed\n");
        goto err;
    }
    ret = test_truncate(ring.ptr, fname, 1, 0, 1);
    if (ret) {
        fprintf(stderr, "Buffered end truncate pbuf read failed\n");
        goto err;
    }

    ret = test_truncate(ring.ptr, fname, 0, 0, 0);
    if (ret) {
        fprintf(stderr, "O_DIRECT end truncate read failed\n");
        goto err;
    }
    ret = test_truncate(ring.ptr, fname, 0, 1, 0);
    if (ret) {
        fprintf(stderr, "O_DIRECT end truncate vec read failed\n");
        goto err;
    }
    ret = test_truncate(ring.ptr, fname, 0, 0, 1);
    if (ret) {
        fprintf(stderr, "O_DIRECT end truncate pbuf read failed\n");
        goto err;
    }

    if (buf == fname)
        unlink(fname);
    return 0;
    err:
    if (buf == fname)
        unlink(fname);
    return 1;
}
