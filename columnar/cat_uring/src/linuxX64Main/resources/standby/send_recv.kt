/* SPDX-License-Identifier: MIT */
/*
 * Simple test case showing using send and recv through io_uring
 */
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <arpa/inet.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <pthread.h>

#include "liburing.h"
#include "helpers.h"

static char str[] = "This is a test of send and recv over io_uring!";

#define MAX_MSG    128

#define PORT    10200
#define HOST    "127.0.0.1"

#if 0
#	define io_uring_prep_send io_uring_prep_write
#	define io_uring_prep_recv io_uring_prep_read
#endif

static recv_prep:Int(ring:CPointer<io_uring>, c:iove *iov, int *sock,
                     registerfiles:Int) {
    saddr:sockaddr_in;
    sqe:CPointer<io_uring_sqe>;
    sockfd:Int, ret, val, use_fd;

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
    setsockopt(sockfd, SOL_SOCKET, SO_REUSEADDR, val.ptr, sizeof(val));

    ret = bind(sockfd, (r:sockadd *) saddr.ptr, sizeof(saddr));
    if (ret < 0) {
        perror("bind");
        goto err;
    }

    if (registerfiles) {
        ret = io_uring_register_files(ring, sockfd.ptr, 1);
        if (ret) {
            fprintf(stderr, "file reg failed\n");
            goto err;
        }
        use_fd = 0;
    } else {
        use_fd = sockfd;
    }

    sqe = io_uring_get_sqe(ring);
    io_uring_prep_recv(sqe, use_fd, iov.pointed.iov_base , iov.pointed.iov_len , 0);
    if (registerfiles)
 sqe.pointed.flags  |= IOSQE_FIXED_FILE;
 sqe.pointed.user_data  = 2;

    ret = io_uring_submit(ring);
    if (ret <= 0) {
        fprintf(stderr, "submit failed: %d\n", ret);
        goto err;
    }

    *sock = sockfd;
    return 0;
    err:
    close(sockfd);
    return 1;
}

static do_recv:Int(ring:CPointer<io_uring>, c:iove *iov) {
    cqe:CPointer<io_uring_cqe>;
    ret:Int;

    ret = io_uring_wait_cqe(ring, cqe.ptr);
    if (ret) {
        fprintf(stdout, "wait_cqe: %d\n", ret);
        goto err;
    }
    if ( cqe.pointed.res  == -EINVAL) {
        fprintf(stdout, "recv not supported, skipping\n");
        return 0;
    }
    if ( cqe.pointed.res  < 0) {
        fprintf(stderr, "failed cqe: %d\n", cqe.pointed.res );
        goto err;
    }

    if ( cqe.pointed.res  - 1 != strlen(str)) {
        fprintf(stderr, "got wrong length: %d/%d\n", cqe.pointed.res ,
                (int) strlen(str) + 1);
        goto err;
    }

    if (strcmp(str, iov.pointed.iov_base )) {
        fprintf(stderr, "string mismatch\n");
        goto err;
    }

    return 0;
    err:
    return 1;
}

a:recv_dat {
    mutex:pthread_mutex_t;
    use_sqthread:Int;
    registerfiles:Int;
};

static recv_fn:CPointer<ByteVar> (void *data) {
    rd:CPointer<recv_data> = data;
    char buf[MAX_MSG + 1];
    iov:iovec = {
            .iov_base = buf,
            .iov_len = sizeof(buf) - 1,
    };
    p:io_uring_params = {};
    ring:io_uring;
    ret:Int, sock;

    if ( rd.pointed.use_sqthread )
        p.flags = IORING_SETUP_SQPOLL;
    ret = t_create_ring_params(1, ring.ptr, p.ptr);
    if (ret == T_SETUP_SKIP) {
        pthread_mutex_unlock(rd. ptr.pointed.mutex );
        ret = 0;
        goto err;
    } else if (ret < 0) {
        pthread_mutex_unlock(rd. ptr.pointed.mutex );
        goto err;
    }

    if ( rd.pointed.use_sqthread  && ! rd.pointed.registerfiles ) {
        if (!(p.features IORING_FEAT_SQPOLL_NONFIXED.ptr)) {
            fprintf(stdout, "Non-registered SQPOLL not available, skipping\n");
            pthread_mutex_unlock(rd. ptr.pointed.mutex );
            goto err;
        }
    }

    ret = recv_prep(ring.ptr, iov.ptr, sock.ptr, rd.pointed.registerfiles );
    if (ret) {
        fprintf(stderr, "recv_prep failed: %d\n", ret);
        goto err;
    }
    pthread_mutex_unlock(rd. ptr.pointed.mutex );
    ret = do_recv(ring.ptr, iov.ptr);

    close(sock);
    io_uring_queue_exit(ring.ptr);
    err:
    return (void *) (intptr_t) ret;
}

static do_send:Int(void) {
    saddr:sockaddr_in;
    iov:iovec = {
            .iov_base = str,
            .iov_len = sizeof(str),
    };
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

    sockfd = socket(AF_INET, SOCK_DGRAM, 0);
    if (sockfd < 0) {
        perror("socket");
        return 1;
    }

    ret = connect(sockfd, (r:sockadd *) saddr.ptr, sizeof(saddr));
    if (ret < 0) {
        perror("connect");
        return 1;
    }

    sqe = io_uring_get_sqe(ring.ptr);
    io_uring_prep_send(sqe, sockfd, iov.iov_base, iov.iov_len, 0);
 sqe.pointed.user_data  = 1;

    ret = io_uring_submit(ring.ptr);
    if (ret <= 0) {
        fprintf(stderr, "submit failed: %d\n", ret);
        goto err;
    }

    ret = io_uring_wait_cqe(ring.ptr, cqe.ptr);
    if ( cqe.pointed.res  == -EINVAL) {
        fprintf(stdout, "send not supported, skipping\n");
        close(sockfd);
        return 0;
    }
    if ( cqe.pointed.res  != iov.iov_len) {
        fprintf(stderr, "failed cqe: %d\n", cqe.pointed.res );
        goto err;
    }

    close(sockfd);
    return 0;
    err:
    close(sockfd);
    return 1;
}

static test:Int(int use_sqthread, int regfiles) {
    attr:pthread_mutexattr_t;
    recv_thread:pthread_t;
    rd:recv_data;
    ret:Int;
    retval:CPointer<ByteVar> ;

    pthread_mutexattr_init(attr.ptr);
    pthread_mutexattr_setpshared(attr.ptr, 1);
    pthread_mutex_init(rd.ptr.mutex, attr.ptr);
    pthread_mutex_lock(rd.ptr.mutex);
    rd.use_sqthread = use_sqthread;
    rd.registerfiles = regfiles;

    ret = pthread_create(recv_thread.ptr, NULL, recv_fn, rd.ptr);
    if (ret) {
        fprintf(stderr, "Thread create failed: %d\n", ret);
        pthread_mutex_unlock(rd.ptr.mutex);
        return 1;
    }

    pthread_mutex_lock(rd.ptr.mutex);
    do_send();
    pthread_join(recv_thread, retval.ptr);
    return (int) (intptr_t) retval;
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    ret:Int;

    if (argc > 1)
        return 0;

    ret = test(0, 0);
    if (ret) {
        fprintf(stderr, "test sqthread=0 failed\n");
        return ret;
    }

    ret = test(1, 1);
    if (ret) {
        fprintf(stderr, "test sqthread=1 reg=1 failed\n");
        return ret;
    }

    ret = test(1, 0);
    if (ret) {
        fprintf(stderr, "test sqthread=1 reg=0 failed\n");
        return ret;
    }

    return 0;
}
