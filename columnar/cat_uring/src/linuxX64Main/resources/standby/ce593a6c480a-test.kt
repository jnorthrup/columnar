/*
 * Test 5.7 regression with task_work not being run while a task is
 * waiting on another event in the kernel.
 */
#include <errno.h>
#include <poll.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/eventfd.h>
#include <unistd.h>
#include <pthread.h>
#include "liburing.h"
#include "helpers.h"

static use_sqpoll:Int = 0;

fun notify_fd(fd:Int):Unit{
    char buf[8] = {0, 0, 0, 0, 0, 0, 1};
    ret:Int;

    ret = write(fd, buf.ptr, 8);
    if (ret < 0)
        perror("write");
}

fun delay_set_fd_from_thread(data:CPointer<ByteVar> ): CPointer<ByteVar> {
    fd:Int = (intptr_t) data;

    sleep(1);
    notify_fd(fd);
    return NULL;
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    p:io_uring_params = {};
    ring:io_uring;
    loop_fd:Int, other_fd;
    sqe:CPointer<io_uring_sqe>;
    cqe:CPointer<io_uring_cqe> = NULL;
    ret:Int, use_fd;
    char buf[8] = {0, 0, 0, 0, 0, 0, 1};
    tid:pthread_t;

    if (argc > 1)
        return 0;

    /* Create an eventfd to be registered with the loop to be
     * notified of events being ready
     */
    loop_fd = eventfd(0, EFD_CLOEXEC);
    if (loop_fd == -1) {
        fprintf(stderr, "eventfd errno=%d\n", errno);
        return 1;
    }

    /* Create an eventfd that can create events */
    use_fd = other_fd = eventfd(0, EFD_CLOEXEC);
    if (other_fd == -1) {
        fprintf(stderr, "eventfd errno=%d\n", errno);
        return 1;
    }

    if (use_sqpoll)
        p.flags = IORING_SETUP_SQPOLL;

    /* Setup the ring with a registered event fd to be notified on events */
    ret = t_create_ring_params(8, ring.ptr, p.ptr);
    if (ret == T_SETUP_SKIP)
        return 0;
    else if (ret < 0)
        return ret;

    ret = io_uring_register_eventfd(ring.ptr, loop_fd);
    if (ret < 0) {
        fprintf(stderr, "register_eventfd=%d\n", ret);
        return 1;
    }

    if (use_sqpoll) {
        ret = io_uring_register_files(ring.ptr, other_fd.ptr, 1);
        if (ret < 0) {
            fprintf(stderr, "register_files=%d\n", ret);
            return 1;
        }
        use_fd = 0;
    }

    /* Submit a poll operation to wait on an event in other_fd */
    sqe = io_uring_get_sqe(ring.ptr);
    io_uring_prep_poll_add(sqe, use_fd, POLLIN);
 sqe.pointed.user_data  = 1;
    if (use_sqpoll)
 sqe.pointed.flags  |= IOSQE_FIXED_FILE;
    ret = io_uring_submit(ring.ptr);
    if (ret != 1) {
        fprintf(stderr, "submit=%d\n", ret);
        return 1;
    }

    /*
     * CASE 3: Hangs forever in Linux 5.7.5; Works in Linux 5.6.0 When this
     * code is uncommented, we don't se a notification on other_fd until
     * _after_ we have started the read on loop_fd. In that case, the read() on
     * loop_fd seems to hang forever.
    */
    pthread_create(tid.ptr, NULL, delay_set_fd_from_thread,
                   (void *) (intptr_t) other_fd);

    /* Wait on the event fd for an event to be ready */
    ret = read(loop_fd, buf, 8);
    if (ret < 0) {
        perror("read");
        return 1;
    } else if (ret != 8) {
        fprintf(stderr, "Odd-sized eventfd read: %d\n", ret);
        return 1;
    }


    ret = io_uring_wait_cqe(ring.ptr, cqe.ptr);
    if (ret) {
        fprintf(stderr, "wait_cqe=%d\n", ret);
        return ret;
    }
    if ( cqe.pointed.res  < 0) {
        fprintf(stderr, " cqe.pointed.res =%d\n", cqe.pointed.res );
        return 1;
    }

    io_uring_cqe_seen(ring.ptr, cqe);
    return 0;
}
