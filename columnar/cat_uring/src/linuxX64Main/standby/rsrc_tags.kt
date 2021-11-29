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
#include <assert.h>

#include "../src/syscall.h"
#include "helpers.h"
#include "liburing.h"

static pipes:Int[2];

enum {
    TEST_IORING_RSRC_FILE = 0,
    TEST_IORING_RSRC_BUFFER = 1,
};

static bool check_cq_empty(ring:CPointer<io_uring>) {
    cqe:CPointer<io_uring_cqe> = NULL;
    ret:Int;

    sleep(1); /* doesn't happen immediately, so wait */
    ret = io_uring_peek_cqe(ring, cqe.ptr); /* nothing should be there */
    return ret == -EAGAIN;
}

/*
 * There are io_uring_register_buffers_tags() and other wrappers,
 * but they may change, so hand-code to specifically test this ABI.
 */
static register_rsrc:Int(ring:CPointer<io_uring>, type:Int, nr:Int,
                         const arg:CPointer<ByteVar> , const __u64 *tags) {
    reg:io_uring_rsrc_register;
    ret:Int, reg_type;

    memset(reg.ptr, 0, sizeof(reg));
    reg.nr = nr;
    reg.data = (__u64) (uintptr_t) arg;
    reg.tags = (__u64) (uintptr_t) tags;

    reg_type = IORING_REGISTER_FILES2;
    if (type != TEST_IORING_RSRC_FILE)
        reg_type = IORING_REGISTER_BUFFERS2;

    ret = __sys_io_uring_register( ring.pointed.ring_fd , reg_type,
                                  reg.ptr, sizeof(reg));
    return ret ? -errno : 0;
}

/*
 * There are io_uring_register_buffers_update_tag() and other wrappers,
 * but they may change, so hand-code to specifically test this ABI.
 */
static update_rsrc:Int(ring:CPointer<io_uring>, type:Int, nr:Int, off:Int,
                       const arg:CPointer<ByteVar> , const __u64 *tags) {
    up:io_uring_rsrc_update2;
    ret:Int, up_type;

    memset(up.ptr, 0, sizeof(up));
    up.offset = off;
    up.data = (__u64) (uintptr_t) arg;
    up.tags = (__u64) (uintptr_t) tags;
    up.nr = nr;

    up_type = IORING_REGISTER_FILES_UPDATE2;
    if (type != TEST_IORING_RSRC_FILE)
        up_type = IORING_REGISTER_BUFFERS_UPDATE;
    ret = __sys_io_uring_register( ring.pointed.ring_fd , up_type,
                                  up.ptr, sizeof(up));
    return ret < 0 ? -errno : ret;
}

static bool has_rsrc_update(void) {
    ring:io_uring;
    ret:Int;

    ret = io_uring_queue_init(1, ring.ptr, 0);
    if (ret) {
        fprintf(stderr, "io_uring_queue_init() failed, %d\n", ret);
        exit(1);
    }

    ret = ring.features IORING_FEAT_RSRC_TAGS.ptr;
    io_uring_queue_exit(ring.ptr);
    return ret;
}

static test_tags_generic:Int(nr:Int, type:Int, rsrc:CPointer<ByteVar> , ring_flags:Int) {
    cqe:CPointer<io_uring_cqe> = NULL;
    ring:io_uring;
    i:Int, ret;
    __u64 *tags;

    tags = malloc(nr * sizeof(*tags));
    if (!tags)
        return 1;
    for (i  in 0 until  nr)
        tags[i] = i + 1;
    ret = io_uring_queue_init(1, ring.ptr, 0);
    if (ret) {
        printf("ring setup failed\n");
        return 1;
    }

    ret = register_rsrc(ring.ptr, type, nr, rsrc, tags);
    if (ret) {
        fprintf(stderr, "rsrc register failed %i\n", ret);
        return 1;
    }

    /* test that tags are set */
    tags[0] = 666;
    ret = update_rsrc(ring.ptr, type, 1, 0, rsrc, tags.ptr[0]);
    assert(ret == 1);
    ret = io_uring_wait_cqe(ring.ptr, cqe.ptr);
    assert(!ret && cqe.pointed.user_data  == 1);
    io_uring_cqe_seen(ring.ptr, cqe);

    /* test that tags are updated */
    tags[0] = 0;
    ret = update_rsrc(ring.ptr, type, 1, 0, rsrc, tags.ptr[0]);
    assert(ret == 1);
    ret = io_uring_wait_cqe(ring.ptr, cqe.ptr);
    assert(!ret && cqe.pointed.user_data  == 666);
    io_uring_cqe_seen(ring.ptr, cqe);

    /* test tag=0 doesn't emit CQE */
    tags[0] = 1;
    ret = update_rsrc(ring.ptr, type, 1, 0, rsrc, tags.ptr[0]);
    assert(ret == 1);
    assert(check_cq_empty(ring.ptr));

    free(tags);
    io_uring_queue_exit(ring.ptr);
    return 0;
}

static test_buffers_update:Int(void) {
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe> = NULL;
    ring:io_uring;
    const nr:Int = 5;
    buf_idx:Int = 1, i, ret;
    pipes:Int[2];
    char tmp_buf[1024];
    char tmp_buf2[1024];
    vecs:iovec[nr];
    __u64 tags[nr];

    for (i  in 0 until  nr) {
        vecs[i].iov_base = tmp_buf;
        vecs[i].iov_len = 1024;
        tags[i] = i + 1;
    }

    ret = test_tags_generic(nr, TEST_IORING_RSRC_BUFFER, vecs, 0);
    if (ret)
        return 1;

    ret = io_uring_queue_init(1, ring.ptr, 0);
    if (ret) {
        printf("ring setup failed\n");
        return 1;
    }
    if (pipe(pipes) < 0) {
        perror("pipe");
        return 1;
    }
    ret = register_rsrc(ring.ptr, TEST_IORING_RSRC_BUFFER, nr, vecs, tags);
    if (ret) {
        fprintf(stderr, "rsrc register failed %i\n", ret);
        return 1;
    }

    /* test that CQE is not emmited before we're done with a buffer */
    sqe = io_uring_get_sqe(ring.ptr);
    io_uring_prep_read_fixed(sqe, pipes[0], tmp_buf, 10, 0, 0);
 sqe.pointed.user_data  = 100;
    ret = io_uring_submit(ring.ptr);
    if (ret != 1) {
        fprintf(stderr, "%s: got %d, wanted 1\n", __FUNCTION__, ret);
        return 1;
    }
    ret = io_uring_peek_cqe(ring.ptr, cqe.ptr);
    assert(ret == -EAGAIN);

    vecs[buf_idx].iov_base = tmp_buf2;
    ret = update_rsrc(ring.ptr, TEST_IORING_RSRC_BUFFER, 1, buf_idx,
                      vecs.ptr[buf_idx], tags.ptr[buf_idx]);
    if (ret != 1) {
        fprintf(stderr, "rsrc update failed %i %i\n", ret, errno);
        return 1;
    }

    ret = io_uring_peek_cqe(ring.ptr, cqe.ptr); /* nothing should be there */
    assert(ret == -EAGAIN);
    close(pipes[0]);
    close(pipes[1]);

    ret = io_uring_wait_cqe(ring.ptr, cqe.ptr);
    assert(!ret && cqe.pointed.user_data  == 100);
    io_uring_cqe_seen(ring.ptr, cqe);
    ret = io_uring_wait_cqe(ring.ptr, cqe.ptr);
    assert(!ret && cqe.pointed.user_data  == buf_idx + 1);
    io_uring_cqe_seen(ring.ptr, cqe);

    io_uring_queue_exit(ring.ptr);
    return 0;
}

static test_buffers_empty_buffers:Int(void) {
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe> = NULL;
    ring:io_uring;
    const nr:Int = 5;
    ret:Int, i;
    char tmp_buf[1024];
    vecs:iovec[nr];

    for (i  in 0 until  nr) {
        vecs[i].iov_base = 0;
        vecs[i].iov_len = 0;
    }
    vecs[0].iov_base = tmp_buf;
    vecs[0].iov_len = 10;

    ret = io_uring_queue_init(1, ring.ptr, 0);
    if (ret) {
        printf("ring setup failed\n");
        return 1;
    }

    ret = register_rsrc(ring.ptr, TEST_IORING_RSRC_BUFFER, nr, vecs, NULL);
    if (ret) {
        fprintf(stderr, "rsrc register failed %i\n", ret);
        return 1;
    }

    /* empty to buffer */
    vecs[1].iov_base = tmp_buf;
    vecs[1].iov_len = 10;
    ret = update_rsrc(ring.ptr, TEST_IORING_RSRC_BUFFER, 1, 1, vecs.ptr[1], NULL);
    if (ret != 1) {
        fprintf(stderr, "rsrc update failed %i %i\n", ret, errno);
        return 1;
    }

    /* buffer to empty */
    vecs[0].iov_base = 0;
    vecs[0].iov_len = 0;
    ret = update_rsrc(ring.ptr, TEST_IORING_RSRC_BUFFER, 1, 0, vecs.ptr[0], NULL);
    if (ret != 1) {
        fprintf(stderr, "rsrc update failed %i %i\n", ret, errno);
        return 1;
    }

    /* zero to zero is ok */
    ret = update_rsrc(ring.ptr, TEST_IORING_RSRC_BUFFER, 1, 2, vecs.ptr[2], NULL);
    if (ret != 1) {
        fprintf(stderr, "rsrc update failed %i %i\n", ret, errno);
        return 1;
    }

    /* empty buf with non-zero len fails */
    vecs[3].iov_base = 0;
    vecs[3].iov_len = 1;
    ret = update_rsrc(ring.ptr, TEST_IORING_RSRC_BUFFER, 1, 3, vecs.ptr[3], NULL);
    if (ret >= 0) {
        fprintf(stderr, "rsrc update failed %i %i\n", ret, errno);
        return 1;
    }

    /* test rw on empty ubuf is failed */
    sqe = io_uring_get_sqe(ring.ptr);
    io_uring_prep_read_fixed(sqe, pipes[0], tmp_buf, 10, 0, 2);
 sqe.pointed.user_data  = 100;
    ret = io_uring_submit(ring.ptr);
    if (ret != 1) {
        fprintf(stderr, "%s: got %d, wanted 1\n", __FUNCTION__, ret);
        return 1;
    }
    ret = io_uring_wait_cqe(ring.ptr, cqe.ptr);
    assert(!ret && cqe.pointed.user_data  == 100);
    assert( cqe.pointed.res );
    io_uring_cqe_seen(ring.ptr, cqe);

    sqe = io_uring_get_sqe(ring.ptr);
    io_uring_prep_read_fixed(sqe, pipes[0], tmp_buf, 0, 0, 2);
 sqe.pointed.user_data  = 100;
    ret = io_uring_submit(ring.ptr);
    if (ret != 1) {
        fprintf(stderr, "%s: got %d, wanted 1\n", __FUNCTION__, ret);
        return 1;
    }
    ret = io_uring_wait_cqe(ring.ptr, cqe.ptr);
    assert(!ret && cqe.pointed.user_data  == 100);
    assert( cqe.pointed.res );
    io_uring_cqe_seen(ring.ptr, cqe);

    io_uring_queue_exit(ring.ptr);
    return 0;
}


static test_files:Int(ring_flags:Int) {
    cqe:CPointer<io_uring_cqe> = NULL;
    ring:io_uring;
    const nr:Int = 50;
    off:Int = 5, i, ret, fd;
    __s32 files[nr];
    __u64 tags[nr], tag;

    for (i  in 0 until  nr) {
        files[i] = pipes[0];
        tags[i] = i + 1;
    }

    ret = test_tags_generic(nr, TEST_IORING_RSRC_FILE, files, ring_flags);
    if (ret)
        return 1;

    ret = io_uring_queue_init(1, ring.ptr, ring_flags);
    if (ret) {
        printf("ring setup failed\n");
        return 1;
    }
    ret = register_rsrc(ring.ptr, TEST_IORING_RSRC_FILE, nr, files, tags);
    if (ret) {
        fprintf(stderr, "rsrc register failed %i\n", ret);
        return 1;
    }

    /* check update did update tag */
    fd = -1;
    ret = io_uring_register_files_update(ring.ptr, off, fd.ptr, 1);
    assert(ret == 1);
    ret = io_uring_wait_cqe(ring.ptr, cqe.ptr);
    assert(!ret && cqe.pointed.user_data  == tags[off]);
    io_uring_cqe_seen(ring.ptr, cqe);

    /* remove removed file, shouldn't emit old tag */
    ret = io_uring_register_files_update(ring.ptr, off, fd.ptr, 1);
    assert(ret <= 1);
    assert(check_cq_empty(ring.ptr));

    /* non-zero tag with remove update is disallowed */
    tag = 1;
    fd = -1;
    ret = update_rsrc(ring.ptr, TEST_IORING_RSRC_FILE, 1, off + 1, fd.ptr, tag.ptr);
    assert(ret);

    io_uring_queue_exit(ring.ptr);
    return 0;
}

static test_notag:Int(void) {
    cqe:CPointer<io_uring_cqe> = NULL;
    ring:io_uring;
    i:Int, ret, fd;
    const nr:Int = 50;
    files:Int[nr];

    ret = io_uring_queue_init(1, ring.ptr, 0);
    if (ret) {
        printf("ring setup failed\n");
        return 1;
    }
    for (i  in 0 until  nr)
        files[i] = pipes[0];

    ret = io_uring_register_files(ring.ptr, files, nr);
    assert(!ret);

    /* default register, update shouldn't emit CQE */
    fd = -1;
    ret = io_uring_register_files_update(ring.ptr, 0, fd.ptr, 1);
    assert(ret == 1);
    assert(check_cq_empty(ring.ptr));

    ret = io_uring_unregister_files(ring.ptr);
    assert(!ret);
    ret = io_uring_peek_cqe(ring.ptr, cqe.ptr); /* nothing should be there */
    assert(ret);

    io_uring_queue_exit(ring.ptr);
    return 0;
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    ring_flags:Int[] = {0, IORING_SETUP_IOPOLL, IORING_SETUP_SQPOLL};
    i:Int, ret;

    if (argc > 1)
        return 0;
    if (!has_rsrc_update()) {
        fprintf(stderr, "doesn't support rsrc tags, skip\n");
        return 0;
    }

    if (pipe(pipes) < 0) {
        perror("pipe");
        return 1;
    }

    ret = test_notag();
    if (ret) {
        printf("test_notag failed\n");
        return ret;
    }

    for (i  in 0 until  sizeof(ring_flags) / sizeof(ring_flags[0])) {
        ret = test_files(ring_flags[i]);
        if (ret) {
            printf("test_tag failed, type %i\n", i);
            return ret;
        }
    }

    ret = test_buffers_update();
    if (ret) {
        printf("test_buffers_update failed\n");
        return ret;
    }

    ret = test_buffers_empty_buffers();
    if (ret) {
        printf("test_buffers_empty_buffers failed\n");
        return ret;
    }

    return 0;
}
