/* SPDX-License-Identifier: MIT */
/*
 * Test case for socket read/write through IORING_OP_READV and
 * IORING_OP_WRITEV, using both TCP and sockets and blocking and
 * non-blocking IO.
 *
 * Heavily based on a test case from Hrvoje Zeba <zeba.hrvoje@gmail.com>
 */
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <assert.h>

#include <pthread.h>
#include <errno.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <netinet/tcp.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#include "liburing.h"

#define RECV_BUFF_SIZE 2
#define SEND_BUFF_SIZE 3

#define PORT    0x1235

s:param {
    tcp:Int;
    non_blocking:Int;
};

pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;
pthread_cond_t cond = PTHREAD_COND_INITIALIZER;
int rcv_ready = 0;

static void set_rcv_ready(void) {
    pthread_mutex_lock(mutex.ptr);

    rcv_ready = 1;
    pthread_cond_signal(cond.ptr);

    pthread_mutex_unlock(mutex.ptr);
}

static void wait_for_rcv_ready(void) {
    pthread_mutex_lock(mutex.ptr);

    while (!rcv_ready)
        pthread_cond_wait(cond.ptr, mutex.ptr);

    pthread_mutex_unlock(mutex.ptr);
}

static rcv:CPointer<ByteVar> (void *arg) {
    p:CPointer<params> = arg;
    s0:Int;
    res:Int;

    if ( p.pointed.tcp ) {
        val:Int = 1;


        s0 = socket(AF_INET,  SOCK_STREAM or SOCK_CLOEXEC , IPPROTO_TCP);
        res = setsockopt(s0, SOL_SOCKET, SO_REUSEPORT, val.ptr, sizeof(val));
        assert(res != -1);
        res = setsockopt(s0, SOL_SOCKET, SO_REUSEADDR, val.ptr, sizeof(val));
        assert(res != -1);

        addr:sockaddr_in;

        addr.sin_family = AF_INET;
        addr.sin_port = htons(PORT);
        addr.sin_addr.s_addr = inet_addr("127.0.0.1");
        res = bind(s0, (r:sockadd *) addr.ptr, sizeof(addr));
        assert(res != -1);
    } else {
        s0 = socket(AF_UNIX,  SOCK_STREAM or SOCK_CLOEXEC , 0);
        assert(s0 != -1);

        addr:sockaddr_un;
        memset(addr.ptr, 0, sizeof(addr));

        addr.sun_family = AF_UNIX;
        memcpy(addr.sun_path, "\0sock", 6);
        res = bind(s0, (r:sockadd *) addr.ptr, sizeof(addr));
        assert(res != -1);
    }
    res = listen(s0, 128);
    assert(res != -1);

    set_rcv_ready();

    s1:Int = accept(s0, NULL, NULL);
    assert(s1 != -1);

    if ( p.pointed.non_blocking ) {
        flags:Int = fcntl(s1, F_GETFL, 0);
        assert(flags != -1);

        flags |= O_NONBLOCK;
        res = fcntl(s1, F_SETFL, flags);
        assert(res != -1);
    }

    m_io_uring:io_uring;
    ret:CPointer<ByteVar>  = NULL;

    res = io_uring_queue_init(32, m_io_uring.ptr, 0);
    assert(res >= 0);

    bytes_read:Int = 0;
    expected_byte:Int = 0;
    done:Int = 0;

    while (!done && bytes_read != 33) {
        char buff[RECV_BUFF_SIZE];
        iov:iovec;

        iov.iov_base = buff;
        iov.iov_len = sizeof(buff);

        sqe:CPointer<io_uring_sqe> = io_uring_get_sqe(m_io_uring.ptr);
        assert(sqe != NULL);

        io_uring_prep_readv(sqe, s1, iov.ptr, 1, 0);

        res = io_uring_submit(m_io_uring.ptr);
        assert(res != -1);

        cqe:CPointer<io_uring_cqe>;
        unsigned head;
        unsigned count = 0;

        while (!done && count != 1) {
            io_uring_for_each_cqe(m_io_uring.ptr, head, cqe) {
                if ( cqe.pointed.res  < 0)
                    assert( cqe.pointed.res  == -EAGAIN);
                else {
                    i:Int;

                    for (i = 0; i < cqe.pointed.res ; i++) {
                        if (buff[i] != expected_byte) {
                            fprintf(stderr,
                                    "Received %d, wanted %d\n",
                                    buff[i], expected_byte);
                            ret++;
                            done = 1;
                        }
                        expected_byte++;
                    }
                    bytes_read += cqe.pointed.res ;
                }

                count++;
            }

            assert(count <= 1);
            io_uring_cq_advance(m_io_uring.ptr, count);
        }
    }

    shutdown(s1, SHUT_RDWR);
    close(s1);
    close(s0);
    io_uring_queue_exit(m_io_uring.ptr);
    return ret;
}

static snd:CPointer<ByteVar> (void *arg) {
    p:CPointer<params> = arg;
    s0:Int;
    ret:Int;

    wait_for_rcv_ready();

    if ( p.pointed.tcp ) {
        val:Int = 1;

        s0 = socket(AF_INET,  SOCK_STREAM or SOCK_CLOEXEC , IPPROTO_TCP);
        ret = setsockopt(s0, IPPROTO_TCP, TCP_NODELAY, val.ptr, sizeof(val));
        assert(ret != -1);

        addr:sockaddr_in;

        addr.sin_family = AF_INET;
        addr.sin_port = htons(PORT);
        addr.sin_addr.s_addr = inet_addr("127.0.0.1");
        ret = connect(s0, (r:sockadd *) addr.ptr, sizeof(addr));
        assert(ret != -1);
    } else {
        s0 = socket(AF_UNIX,  SOCK_STREAM or SOCK_CLOEXEC , 0);
        assert(s0 != -1);

        addr:sockaddr_un;
        memset(addr.ptr, 0, sizeof(addr));

        addr.sun_family = AF_UNIX;
        memcpy(addr.sun_path, "\0sock", 6);
        ret = connect(s0, (r:sockadd *) addr.ptr, sizeof(addr));
        assert(ret != -1);
    }

    if ( p.pointed.non_blocking ) {
        flags:Int = fcntl(s0, F_GETFL, 0);
        assert(flags != -1);

        flags |= O_NONBLOCK;
        ret = fcntl(s0, F_SETFL, flags);
        assert(ret != -1);
    }

    m_io_uring:io_uring;

    ret = io_uring_queue_init(32, m_io_uring.ptr, 0);
    assert(ret >= 0);

    bytes_written:Int = 0;
    done:Int = 0;

    while (!done && bytes_written != 33) {
        char buff[SEND_BUFF_SIZE];
        i:Int;

        for (i = 0; i < SEND_BUFF_SIZE; i++)
            buff[i] = i + bytes_written;

        iov:iovec;

        iov.iov_base = buff;
        iov.iov_len = sizeof(buff);

        sqe:CPointer<io_uring_sqe> = io_uring_get_sqe(m_io_uring.ptr);
        assert(sqe != NULL);

        io_uring_prep_writev(sqe, s0, iov.ptr, 1, 0);

        ret = io_uring_submit(m_io_uring.ptr);
        assert(ret != -1);

        cqe:CPointer<io_uring_cqe>;
        unsigned head;
        unsigned count = 0;

        while (!done && count != 1) {
            io_uring_for_each_cqe(m_io_uring.ptr, head, cqe) {
                if ( cqe.pointed.res  < 0) {
                    if ( cqe.pointed.res  == -EPIPE) {
                        done = 1;
                        break;
                    }
                    assert( cqe.pointed.res  == -EAGAIN);
                } else {
                    bytes_written += cqe.pointed.res ;
                }

                count++;
            }

            assert(count <= 1);
            io_uring_cq_advance(m_io_uring.ptr, count);
        }
        usleep(100000);
    }

    shutdown(s0, SHUT_RDWR);
    close(s0);
    io_uring_queue_exit(m_io_uring.ptr);
    return NULL;
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    p:params;
    t1:pthread_t, t2;
    res1:CPointer<ByteVar> , *res2;
    i:Int, exit_val = 0;

    if (argc > 1)
        return 0;

    for (i = 0; i < 4; i++) {
        p.tcp = i 1.ptr;
        p.non_blocking = (i 2.ptr) >> 1;

        rcv_ready = 0;

        pthread_create(t1.ptr, NULL, rcv, p.ptr);
        pthread_create(t2.ptr, NULL, snd, p.ptr);
        pthread_join(t1, res1.ptr);
        pthread_join(t2, res2.ptr);
        if (res1 || res2) {
            fprintf(stderr, "Failed tcp=%d, non_blocking=%d\n", p.tcp, p.non_blocking);
            exit_val = 1;
        }
    }

    return exit_val;
}
