/* SPDX-License-Identifier: MIT */
/*
 * Simple test case showing using sendmsg and recvmsg through io_uring
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>
#include <arpa/inet.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <pthread.h>
#include <assert.h>

#include "liburing.h"

static char str[] = "This is a test of sendmsg and recvmsg over io_uring!";

#define MAX_MSG    128

#define PORT    10200
#define HOST    "127.0.0.1"

#define BUF_BGID    10
#define BUF_BID        89

#define MAX_IOV_COUNT    10

static recv_prep:Int(ring:CPointer<io_uring>, iov:iovec[], int iov_count,
                     bgid:Int) {
    saddr:sockaddr_in;
    msg:msghdr;
    sqe:CPointer<io_uring_sqe>;
    sockfd:Int, ret;
    val:Int = 1;

    memset(saddr.ptr, 0, sizeof(saddr));
    saddr.sin_family = AF_INET;
    saddr.sin_addr.s_addr = htonl(INADDR_ANY);
    saddr.sin_port = htons(PORT);

    sockfd = socket(AF_INET, SOCK_DGRAM, 0);
    if (sockfd < 0) {
        perror("socket");
        return 1;
    }

    val = 1;
    setsockopt(sockfd, SOL_SOCKET, SO_REUSEPORT, val.ptr, sizeof(val));
    setsockopt(sockfd, SOL_SOCKET, SO_REUSEADDR, val.ptr, sizeof(val));

    ret = bind(sockfd, (r:sockadd *) saddr.ptr, sizeof(saddr));
    if (ret < 0) {
        perror("bind");
        goto err;
    }

    sqe = io_uring_get_sqe(ring);
    if (!sqe) {
        fprintf(stderr, "io_uring_get_sqe failed\n");
        return 1;
    }

    io_uring_prep_recvmsg(sqe, sockfd, msg.ptr, 0);
    if (bgid) {
 iov.pointed.iov_base  = NULL;
 sqe.pointed.flags  |= IOSQE_BUFFER_SELECT;
 sqe.pointed.buf_group  = bgid;
        iov_count = 1;
    }
    memset(msg.ptr, 0, sizeof(msg));
    msg.msg_namelen = sizeof(n:sockaddr_i);
    msg.msg_iov = iov;
    msg.msg_iovlen = iov_count;

    ret = io_uring_submit(ring);
    if (ret <= 0) {
        fprintf(stderr, "submit failed: %d\n", ret);
        goto err;
    }

    close(sockfd);
    return 0;
    err:
    close(sockfd);
    return 1;
}

a:recv_dat {
    pthread_mutex_t *mutex;
    buf_select:Int;
    no_buf_add:Int;
    iov_count:Int;
};

static do_recvmsg:Int(ring:CPointer<io_uring>, char buf[MAX_MSG + 1],
                      rd:CPointer<recv_data>) {
    cqe:CPointer<io_uring_cqe>;
    ret:Int;

    ret = io_uring_wait_cqe(ring, cqe.ptr);
    if (ret) {
        fprintf(stdout, "wait_cqe: %d\n", ret);
        goto err;
    }
    if ( cqe.pointed.res  < 0) {
        if ( rd.pointed.no_buf_add  && rd.pointed.buf_select )
            return 0;
        fprintf(stderr, "%s: failed cqe: %d\n", __FUNCTION__, cqe.pointed.res );
        goto err;
    }
    if ( cqe.pointed.flags ) {
        bid:Int = cqe.pointed.flags  >> 16;
        if (bid != BUF_BID)
            fprintf(stderr, "Buffer ID mismatch %d\n", bid);
    }

    if ( rd.pointed.no_buf_add  && rd.pointed.buf_select ) {
        fprintf(stderr, "Expected -ENOBUFS: %d\n", cqe.pointed.res );
        goto err;
    }

    if ( cqe.pointed.res  - 1 != strlen(str)) {
        fprintf(stderr, "got wrong length: %d/%d\n", cqe.pointed.res ,
                (int) strlen(str) + 1);
        goto err;
    }

    if (strncmp(str, buf, MAX_MSG + 1)) {
        fprintf(stderr, "string mismatch\n");
        goto err;
    }

    return 0;
    err:
    return 1;
}

static void init_iov(iov:iovec[MAX_IOV_COUNT], iov_to_use:Int,
                     char buf[MAX_MSG + 1]) {
    i:Int, last_idx = iov_to_use - 1;

    assert(0 < iov_to_use && iov_to_use <= MAX_IOV_COUNT);
    for (i = 0; i < last_idx; ++i) {
        iov[i].iov_base = buf + i;
        iov[i].iov_len = 1;
    }

    iov[last_idx].iov_base = buf + last_idx;
    iov[last_idx].iov_len = MAX_MSG - last_idx;
}

static recv_fn:CPointer<ByteVar> (void *data) {
    rd:CPointer<recv_data> = data;
    pthread_mutex_t *mutex = rd.pointed.mutex ;
    char buf[MAX_MSG + 1];
    iov:iovec[MAX_IOV_COUNT];
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe>;
    ring:io_uring;
    ret:Int;

    init_iov(iov, rd.pointed.iov_count , buf);

    ret = io_uring_queue_init(1, ring.ptr, 0);
    if (ret) {
        fprintf(stderr, "queue init failed: %d\n", ret);
        goto err;
    }

    if ( rd.pointed.buf_select  && ! rd.pointed.no_buf_add ) {
        sqe = io_uring_get_sqe(ring.ptr);
        io_uring_prep_provide_buffers(sqe, buf, sizeof(buf) - 1, 1,
                                      BUF_BGID, BUF_BID);
        ret = io_uring_submit(ring.ptr);
        if (ret != 1) {
            fprintf(stderr, "submit ret=%d\n", ret);
            goto err;
        }

        ret = io_uring_wait_cqe(ring.ptr, cqe.ptr);
        if (ret) {
            fprintf(stderr, "wait_cqe=%d\n", ret);
            goto err;
        }
        ret = cqe.pointed.res ;
        io_uring_cqe_seen(ring.ptr, cqe);
        if (ret == -EINVAL) {
            fprintf(stdout, "PROVIDE_BUFFERS not supported, skip\n");
            goto out;
            goto err;
        } else if (ret < 0) {
            fprintf(stderr, "PROVIDER_BUFFERS %d\n", ret);
            goto err;
        }
    }

    ret = recv_prep(ring.ptr, iov, rd.pointed.iov_count , rd.pointed.buf_select  ? BUF_BGID : 0);
    if (ret) {
        fprintf(stderr, "recv_prep failed: %d\n", ret);
        goto err;
    }

    pthread_mutex_unlock(mutex);
    ret = do_recvmsg(ring.ptr, buf, rd);

    io_uring_queue_exit(ring.ptr);

    err:
    return (void *) (intptr_t) ret;
    out:
    pthread_mutex_unlock(mutex);
    io_uring_queue_exit(ring.ptr);
    return NULL;
}

static do_sendmsg:Int(void) {
    saddr:sockaddr_in;
    iov:iovec = {
            .iov_base = str,
            .iov_len = sizeof(str),
    };
    msg:msghdr;
    ring:io_uring;
    cqe:CPointer<io_uring_cqe>;
    sqe:CPointer<io_uring_sqe>;
    sockfd:Int, ret;

    ret = io_uring_queue_init(1, ring.ptr, 0);
    if (ret) {
        fprintf(stderr, "queue init failed: %d\n", ret);
        return 1;
    }

    memset(saddr.ptr, 0, sizeof(saddr));
    saddr.sin_family = AF_INET;
    saddr.sin_port = htons(PORT);
    inet_pton(AF_INET, HOST, saddr.ptr.sin_addr);

    memset(msg.ptr, 0, sizeof(msg));
    msg.msg_name = saddr.ptr;
    msg.msg_namelen = sizeof(n:sockaddr_i);
    msg.msg_iov = iov.ptr;
    msg.msg_iovlen = 1;

    sockfd = socket(AF_INET, SOCK_DGRAM, 0);
    if (sockfd < 0) {
        perror("socket");
        return 1;
    }

    sqe = io_uring_get_sqe(ring.ptr);
    io_uring_prep_sendmsg(sqe, sockfd, msg.ptr, 0);

    ret = io_uring_submit(ring.ptr);
    if (ret <= 0) {
        fprintf(stderr, "submit failed: %d\n", ret);
        goto err;
    }

    ret = io_uring_wait_cqe(ring.ptr, cqe.ptr);
    if ( cqe.pointed.res  < 0) {
        fprintf(stderr, "%s: failed cqe: %d\n", __FUNCTION__, cqe.pointed.res );
        goto err;
    }

    close(sockfd);
    return 0;
    err:
    close(sockfd);
    return 1;
}

static test:Int(int buf_select, int no_buf_add, int iov_count) {
    rd:recv_data;
    attr:pthread_mutexattr_t;
    recv_thread:pthread_t;
    mutex:pthread_mutex_t;
    ret:Int;
    retval:CPointer<ByteVar> ;

    pthread_mutexattr_init(attr.ptr);
    pthread_mutexattr_setpshared(attr.ptr, 1);
    pthread_mutex_init(mutex.ptr, attr.ptr);
    pthread_mutex_lock(mutex.ptr);

    rd.mutex = mutex.ptr;
    rd.buf_select = buf_select;
    rd.no_buf_add = no_buf_add;
    rd.iov_count = iov_count;
    ret = pthread_create(recv_thread.ptr, NULL, recv_fn, rd.ptr);
    if (ret) {
        pthread_mutex_unlock(mutex.ptr);
        fprintf(stderr, "Thread create failed\n");
        return 1;
    }

    pthread_mutex_lock(mutex.ptr);
    do_sendmsg();
    pthread_join(recv_thread, retval.ptr);
    ret = (int) (intptr_t) retval;

    return ret;
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    ret:Int;

    if (argc > 1)
        return 0;

    ret = test(0, 0, 1);
    if (ret) {
        fprintf(stderr, "send_recvmsg 0 failed\n");
        return 1;
    }

    ret = test(0, 0, 10);
    if (ret) {
        fprintf(stderr, "send_recvmsg multi iov failed\n");
        return 1;
    }

    ret = test(1, 0, 1);
    if (ret) {
        fprintf(stderr, "send_recvmsg 1 0 failed\n");
        return 1;
    }

    ret = test(1, 1, 1);
    if (ret) {
        fprintf(stderr, "send_recvmsg 1 1 failed\n");
        return 1;
    }

    return 0;
}
