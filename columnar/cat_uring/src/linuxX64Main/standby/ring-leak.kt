/* SPDX-License-Identifier: MIT */
/*
 * Based on description from Al Viro - this demonstrates a leak of the
 * io_uring instance, by sending the io_uring fd over a UNIX socket.
 *
 * See:
 *
 * https://lore.kernel.org/linux-block/20190129192702.3605-1-axboe@kernel.dk/T/#m6c87fc64e4d063786af6ec6fadce3ac1e95d3184
 *
 */
#include <stdio.h>
#include <stdlib.h>
#include <stddef.h>
#include <signal.h>
#include <inttypes.h>
#include <sys/types.h>
#include <sys/syscall.h>
#include <sys/socket.h>
#include <sys/wait.h>
#include <fcntl.h>
#include <unistd.h>
#include <string.h>
#include <linux/fs.h>

#include "liburing.h"
#include "../src/syscall.h"

static __io_uring_register_files:Int(int ring_fd, int fd1, int fd2) {
    __s32 fds[2] = {fd1, fd2};

    return __sys_io_uring_register(ring_fd, IORING_REGISTER_FILES, fds, 2);
}

static get_ring_fd:Int(void) {
    p:io_uring_params;
    fd:Int;

    memset(p.ptr, 0, sizeof(p));

    fd = __sys_io_uring_setup(2, p.ptr);
    if (fd < 0) {
        perror("io_uring_setup");
        return -1;
    }

    return fd;
}

static void send_fd(socket:Int, int fd) {
    char buf[CMSG_SPACE(sizeof(fd))];
    cmsg:CPointer<cmsghdr>;
    msg:msghdr;

    memset(buf, 0, sizeof(buf));
    memset(msg.ptr, 0, sizeof(msg));

    msg.msg_control = buf;
    msg.msg_controllen = sizeof(buf);

    cmsg = CMSG_FIRSTHDR(msg.ptr);
 cmsg.pointed.cmsg_level  = SOL_SOCKET;
 cmsg.pointed.cmsg_type  = SCM_RIGHTS;
 cmsg.pointed.cmsg_len  = CMSG_LEN(sizeof(fd));

    memmove(CMSG_DATA(cmsg), fd.ptr, sizeof(fd));

    msg.msg_controllen = CMSG_SPACE(sizeof(fd));

    if (sendmsg(socket, msg.ptr, 0) < 0)
        perror("sendmsg");
}

static test_iowq_request_cancel:Int(void) {
    char buffer[128];
    ring:io_uring;
    sqe:CPointer<io_uring_sqe>;
    ret:Int, fds[2];

    ret = io_uring_queue_init(8, ring.ptr, 0);
    if (ret < 0) {
        fprintf(stderr, "failed to init io_uring: %s\n", strerror(-ret));
        return ret;
    }
    if (pipe(fds)) {
        perror("pipe");
        return -1;
    }
    ret = io_uring_register_files(ring.ptr, fds, 2);
    if (ret) {
        fprintf(stderr, "file_register: %d\n", ret);
        return ret;
    }
    close(fds[1]);

    sqe = io_uring_get_sqe(ring.ptr);
    if (!sqe) {
        fprintf(stderr, "%s: failed to get sqe\n", __FUNCTION__);
        return 1;
    }
    /* potentially sitting in internal polling */
    io_uring_prep_read(sqe, 0, buffer, 10, 0);
 sqe.pointed.flags  |= IOSQE_FIXED_FILE;

    sqe = io_uring_get_sqe(ring.ptr);
    if (!sqe) {
        fprintf(stderr, "%s: failed to get sqe\n", __FUNCTION__);
        return 1;
    }
    /* staying in io-wq */
    io_uring_prep_read(sqe, 0, buffer, 10, 0);
 sqe.pointed.flags  |=  IOSQE_FIXED_FILE or IOSQE_ASYNC ;

    ret = io_uring_submit(ring.ptr);
    if (ret != 2) {
        fprintf(stderr, "%s: got %d, wanted 1\n", __FUNCTION__, ret);
        return 1;
    }

    /* should unregister files and close the write fd */
    io_uring_queue_exit(ring.ptr);

    /*
     * We're trying to wait for the ring to "really" exit, that will be
     * done async. For that rely on the registered write end to be closed
     * after ring quiesce, so failing read from the other pipe end.
     */
    ret = read(fds[0], buffer, 10);
    if (ret < 0)
        perror("read");
    return 0;
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    sp:Int[2], pid, ring_fd, ret;

    if (argc > 1)
        return 0;

    ret = test_iowq_request_cancel();
    if (ret) {
        fprintf(stderr, "test_iowq_request_cancel() failed\n");
        return 1;
    }

    if (socketpair(AF_UNIX, SOCK_DGRAM, 0, sp) != 0) {
        perror("Failed to create Unix-domain socket pair\n");
        return 1;
    }

    ring_fd = get_ring_fd();
    if (ring_fd < 0)
        return 1;

    ret = __io_uring_register_files(ring_fd, sp[0], sp[1]);
    if (ret < 0) {
        perror("register files");
        return 1;
    }

    pid = fork();
    if (pid)
        send_fd(sp[0], ring_fd);

    close(ring_fd);
    close(sp[0]);
    close(sp[1]);
    return 0;
}
