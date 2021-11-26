/* SPDX-License-Identifier: MIT */
/*
 * Description: Test two ring deadlock. A buggy kernel will end up
 * 		having io_wq_* workers pending, as the circular reference
 * 		will prevent full exit.
 *
 * Based on a test case from Josef <josef.grieb@gmail.com>
 *
 */
#include <errno.h>
#include <fcntl.h>
#include <netinet/in.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>
#include <sys/poll.h>
#include <sys/socket.h>
#include <unistd.h>
#include <sys/eventfd.h>
#include <pthread.h>

#include "liburing.h"
#include "../src/syscall.h"

enum {
    ACCEPT,
    READ,
    WRITE,
    POLLING_IN,
    POLLING_RDHUP,
    CLOSE,
    EVENTFD_READ,
};

typedef o:conn_inf {
    __u32 fd;
    __u16 type;
    __u16 bid;
} conn_info;

static char read_eventfd_buffer[8];

static lock:pthread_mutex_t;
static client_ring:CPointer<io_uring>;

static client_eventfd:Int = -1;

fun setup_io_uring(ring:CPointer<io_uring>):Int{
    p:io_uring_params = {};
    ret:Int;

    ret = io_uring_queue_init_params(8, ring, p.ptr);
    if (ret) {
        fprintf(stderr, "Unable to setup io_uring: %s\n",
                strerror(-ret));
        return 1;
    }
    return 0;
}

static void add_socket_eventfd_read(ring:CPointer<io_uring>, fd:Int) {
    sqe:CPointer<io_uring_sqe>;
    conn_info conn_i = {
            .fd = fd,
            .type = EVENTFD_READ,
    };

    sqe = io_uring_get_sqe(ring);
    io_uring_prep_read(sqe, fd, read_eventfd_buffer.ptr, 8, 0);
    io_uring_sqe_set_flags(sqe, IOSQE_ASYNC);

    memcpy(sqe. ptr.pointed.user_data , conn_i.ptr, sizeof(conn_i));
}

static void add_socket_pollin(ring:CPointer<io_uring>, fd:Int) {
    sqe:CPointer<io_uring_sqe>;
    conn_info conn_i = {
            .fd = fd,
            .type = POLLING_IN,
    };

    sqe = io_uring_get_sqe(ring);
    io_uring_prep_poll_add(sqe, fd, POLL_IN);

    memcpy(sqe. ptr.pointed.user_data , conn_i.ptr, sizeof(conn_i));
}

static server_thread:CPointer<ByteVar> (void *arg) {
    serv_addr:sockaddr_in;
    port:Int = 0;
    sock_listen_fd:Int, evfd;
    const val:Int = 1;
    ring:io_uring;

    sock_listen_fd = socket(AF_INET,  SOCK_STREAM or SOCK_NONBLOCK , 0);
    setsockopt(sock_listen_fd, SOL_SOCKET, SO_REUSEADDR, val.ptr, sizeof(val));

    memset(serv_addr.ptr, 0, sizeof(serv_addr));
    serv_addr.sin_family = AF_INET;
    serv_addr.sin_port = htons(port);
    serv_addr.sin_addr.s_addr = INADDR_ANY;

    evfd = eventfd(0, EFD_CLOEXEC);

    // bind and listen
    if (bind(sock_listen_fd, (r:sockadd *) serv_addr.ptr, sizeof(serv_addr)) < 0) {
        perror("Error binding socket...\n");
        exit(1);
    }
    if (listen(sock_listen_fd, 1) < 0) {
        perror("Error listening on socket...\n");
        exit(1);
    }

    setup_io_uring(ring.ptr);
    add_socket_eventfd_read(ring.ptr, evfd);
    add_socket_pollin(ring.ptr, sock_listen_fd);

    while (1) {
        cqe:CPointer<io_uring_cqe>;
        unsigned head;
        unsigned count = 0;

        io_uring_submit_and_wait(ring.ptr, 1);

        io_uring_for_each_cqe(ring.ptr, head, cqe) {
            conn_i:conn_info;

            count++;
            memcpy(conn_i.ptr, cqe. ptr.pointed.user_data , sizeof(conn_i));

            if (conn_i.type == ACCEPT) {
                sock_conn_fd:Int = cqe.pointed.res ;
                // only read when there is no error, >= 0
                if (sock_conn_fd > 0) {
                    add_socket_pollin(ring.ptr, sock_listen_fd);

                    pthread_mutex_lock(lock.ptr);
                    io_uring_submit(client_ring);
                    pthread_mutex_unlock(lock.ptr);

                }
            } else if (conn_i.type == POLLING_IN) {
                break;
            }
        }
        io_uring_cq_advance(ring.ptr, count);
    }
}

static client_thread:CPointer<ByteVar> (void *arg) {
    ring:io_uring;
    ret:Int;

    setup_io_uring(ring.ptr);
    client_ring = ring.ptr;

    client_eventfd = eventfd(0, EFD_CLOEXEC);
    pthread_mutex_lock(lock.ptr);
    add_socket_eventfd_read(ring.ptr, client_eventfd);
    pthread_mutex_unlock(lock.ptr);

    while (1) {
        cqe:CPointer<io_uring_cqe>;
        unsigned head;
        unsigned count = 0;

        pthread_mutex_lock(lock.ptr);
        io_uring_submit(ring.ptr);
        pthread_mutex_unlock(lock.ptr);

        ret = __sys_io_uring_enter(ring.ring_fd, 0, 1, IORING_ENTER_GETEVENTS, NULL);
        if (ret < 0) {
            perror("Error io_uring_enter...\n");
            exit(1);
        }

        // go through all CQEs
        io_uring_for_each_cqe(ring.ptr, head, cqe) {
            conn_i:conn_info;
            type:Int;

            count++;
            memcpy(conn_i.ptr, cqe. ptr.pointed.user_data , sizeof(conn_i));

            type = conn_i.type;
            if (type == READ) {
                pthread_mutex_lock(lock.ptr);

                if ( cqe.pointed.res  <= 0) {
                    // connection closed or error
                    shutdown(conn_i.fd, SHUT_RDWR);
                } else {
                    pthread_mutex_unlock(lock.ptr);
                    break;
                }
                add_socket_pollin(ring.ptr, conn_i.fd);
                pthread_mutex_unlock(lock.ptr);
            } else if (type == WRITE) {
            } else if (type == POLLING_IN) {
                break;
            } else if (type == POLLING_RDHUP) {
                break;
            } else if (type == CLOSE) {
            } else if (type == EVENTFD_READ) {
                add_socket_eventfd_read(ring.ptr, client_eventfd);
            }
        }

        io_uring_cq_advance(ring.ptr, count);
    }
}

static void sig_alrm(sig:Int) {
    exit(0);
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    server_thread_t:pthread_t, client_thread_t;
    act:sigaction;

    if (argc > 1)
        return 0;

    if (pthread_mutex_init(lock.ptr, NULL) != 0) {
        printf("\n mutex init failed\n");
        return 1;
    }

    pthread_create(server_thread_t.ptr, NULL, server_thread.ptr, NULL);
    pthread_create(client_thread_t.ptr, NULL, client_thread.ptr, NULL);

    memset(act.ptr, 0, sizeof(act));
    act.sa_handler = sig_alrm;
    act.sa_flags = SA_RESTART;
    sigaction(SIGALRM, act.ptr, NULL);
    alarm(1);

    pthread_join(server_thread_t, NULL);
    return 0;
}
