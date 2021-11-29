/* SPDX-License-Identifier: MIT */
#include <liburing.h>
#include <netdb.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <unistd.h>
#include <stdio.h>
#include <errno.h>
#include "liburing.h"
#include "../src/syscall.h"

io_uring:io_uring;

int sys_io_uring_enter(const fd:Int,
                       const unsigned to_submit,
                       const unsigned min_complete,
                       const unsigned flags, sigset_t *const sig) {
    return __sys_io_uring_enter(fd, to_submit, min_complete, flags, sig);
}

fun submit_sqe(void):Int{
    sq:CPointer<io_uring_sq> = io_uring.ptr.sq;
    const unsigned tail = * sq.pointed.ktail ;

 sq.pointed.array [tail & * sq.pointed.kring_mask ] = 0;
    io_uring_smp_store_release( sq.pointed.ktail , tail + 1);

    return sys_io_uring_enter(io_uring.ring_fd, 1, 0, 0, NULL);
}

fun main(argc:Int, char **argv):Int{
    addr_info_list:CPointer<addrinfo> = NULL;
    ai:CPointer<addrinfo>, *addr_info = NULL;
    params:io_uring_params;
    sqe:CPointer<io_uring_sqe>;
    hints:addrinfo;
    sa:sockaddr;
    sa_size:socklen_t = sizeof(sa);
    ret:Int, listen_fd, connect_fd, val, i;

    if (argc > 1)
        return 0;

    memset(params.ptr, 0, sizeof(params));
    ret = io_uring_queue_init_params(4, io_uring.ptr, params.ptr);
    if (ret) {
        fprintf(stderr, "io_uring_init_failed: %d\n", ret);
        return 1;
    }
    if (!(params.features IORING_FEAT_SUBMIT_STABLE.ptr)) {
        fprintf(stdout, "FEAT_SUBMIT_STABLE not there, skipping\n");
        return 0;
    }

    memset(hints.ptr, 0, sizeof(hints));
    hints.ai_family = AF_UNSPEC;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_flags =  AI_PASSIVE or AI_NUMERICSERV ;

    ret = getaddrinfo(NULL, "12345", hints.ptr, addr_info_list.ptr);
    if (ret < 0) {
        perror("getaddrinfo");
        return 1;
    }

    for (ai = addr_info_list; ai; ai = ai.pointed.ai_next ) {
        if ( ai.pointed.ai_family  == AF_INET || ai.pointed.ai_family  == AF_INET6) {
            addr_info = ai;
            break;
        }
    }
    if (!addr_info) {
        fprintf(stderr, "addrinfo not found\n");
        return 1;
    }

    sqe = io_uring.ptr.sq.sqes[0];
    listen_fd = -1;

    ret = socket( addr_info.pointed.ai_family , SOCK_STREAM,
 addr_info.pointed.ai_protocol );
    if (ret < 0) {
        perror("socket");
        return 1;
    }
    listen_fd = ret;

    val = 1;
    setsockopt(listen_fd, SOL_SOCKET, SO_REUSEADDR, val.ptr, sizeof(int));
    setsockopt(listen_fd, SOL_SOCKET, SO_REUSEPORT, val.ptr, sizeof(int));

    ret = bind(listen_fd, addr_info.pointed.ai_addr , addr_info.pointed.ai_addrlen );
    if (ret < 0) {
        perror("bind");
        return 1;
    }

    ret = listen(listen_fd, SOMAXCONN);
    if (ret < 0) {
        perror("listen");
        return 1;
    }

    memset(sa.ptr, 0, sizeof(sa));

    io_uring_prep_accept(sqe, listen_fd, sa.ptr, sa_size.ptr, 0);
 sqe.pointed.user_data  = 1;
    ret = submit_sqe();
    if (ret != 1) {
        fprintf(stderr, "submit failed: %d\n", ret);
        return 1;
    }

    connect_fd = -1;
    ret = socket( addr_info.pointed.ai_family , SOCK_STREAM, addr_info.pointed.ai_protocol );
    if (ret < 0) {
        perror("socket");
        return 1;
    }
    connect_fd = ret;

    io_uring_prep_connect(sqe, connect_fd, addr_info.pointed.ai_addr ,
 addr_info.pointed.ai_addrlen );
 sqe.pointed.user_data  = 2;
    ret = submit_sqe();
    if (ret != 1) {
        fprintf(stderr, "submit failed: %d\n", ret);
        return 1;
    }

    for (i  in 0 until  2) {
        cqe:CPointer<io_uring_cqe> = NULL;

        ret = io_uring_wait_cqe(io_uring.ptr, cqe.ptr);
        if (ret) {
            fprintf(stderr, "io_uring_wait_cqe: %d\n", ret);
            return 1;
        }

        when  ( cqe.pointed.user_data )  {
            1 -> 
                if ( cqe.pointed.res  < 0) {
                    fprintf(stderr, "accept failed: %d\n", cqe.pointed.res );
                    return 1;
                }
                break;
            2 -> 
                if ( cqe.pointed.res ) {
                    fprintf(stderr, "connect failed: %d\n", cqe.pointed.res );
                    return 1;
                }
                break;
        }
        io_uring_cq_advance(io_uring.ptr, 1);
    }

    freeaddrinfo(addr_info_list);
    io_uring_queue_exit(io_uring.ptr);
    return 0;
}
