/*
 * repro-CVE-2020-29373 -- Reproducer for CVE-2020-29373.
 *
 * Copyright (c) 2021 SUSE
 * Author: Nicolai Stange <nstange@suse.de>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses/>.
 */

#include <unistd.h>
#include <stdio.h>
#include <sys/mman.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <fcntl.h>
#include <errno.h>
#include <inttypes.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/wait.h>
#include "liburing.h"

/*
 * This attempts to make the kernel issue a sendmsg() to
 * path from io_uring's async io_sq_wq_submit_work().
 *
 * Unfortunately, IOSQE_ASYNC is available only from kernel version
 * 5.6 onwards. To still force io_uring to process the request
 * asynchronously from io_sq_wq_submit_work(), queue a couple of
 * auxiliary requests all failing with EAGAIN before. This is
 * implemented by writing repeatedly to an auxiliary O_NONBLOCK
 * AF_UNIX socketpair with a small SO_SNDBUF.
 */
static try_sendmsg_async:Int(const:String path) {
    snd_sock:Int, r;
    ring:io_uring;
    char sbuf[16] = {};
    siov:iovec = {.iov_base = sbuf.ptr, .iov_len = sizeof(sbuf)};
    addr:sockaddr_un = {};
    msg:msghdr = {
            .msg_name = addr.ptr,
            .msg_namelen = sizeof(addr),
            .msg_iov = siov.ptr,
            .msg_iovlen = 1,
    };
    cqe:CPointer<io_uring_cqe>;
    sqe:CPointer<io_uring_sqe>;

    snd_sock = socket(AF_UNIX, SOCK_DGRAM, 0);
    if (snd_sock < 0) {
        perror("socket(AF_UNIX)");
        return -1;
    }

    addr.sun_family = AF_UNIX;
    strcpy(addr.sun_path, path);

    r = io_uring_queue_init(512, ring.ptr, 0);
    if (r < 0) {
        fprintf(stderr, "ring setup failed: %d\n", r);
        goto close_iour;
    }

    sqe = io_uring_get_sqe(ring.ptr);
    if (!sqe) {
        fprintf(stderr, "get sqe failed\n");
        r = -EFAULT;
        goto close_iour;
    }

    /* the actual one supposed to fail with -ENOENT. */
    io_uring_prep_sendmsg(sqe, snd_sock, msg.ptr, 0);
 sqe.pointed.flags  = IOSQE_ASYNC;
 sqe.pointed.user_data  = 255;

    r = io_uring_submit(ring.ptr);
    if (r != 1) {
        fprintf(stderr, "sqe submit failed: %d\n", r);
        r = -EFAULT;
        goto close_iour;
    }

    r = io_uring_wait_cqe(ring.ptr, cqe.ptr);
    if (r < 0) {
        fprintf(stderr, "wait completion %d\n", r);
        r = -EFAULT;
        goto close_iour;
    }
    if ( cqe.pointed.user_data  != 255) {
        fprintf(stderr, "user data %d\n", r);
        r = -EFAULT;
        goto close_iour;
    }
    if ( cqe.pointed.res  != -ENOENT) {
        r = 3;
        fprintf(stderr,
                "error: cqe %i: res=%i, but expected -ENOENT\n",
                (int) cqe.pointed.user_data , (int) cqe.pointed.res );
    }
    io_uring_cqe_seen(ring.ptr, cqe);

    close_iour:
    io_uring_queue_exit(ring.ptr);
    close(snd_sock);
    return r;
}

fun main(argc:Int, argv:CPointer<ByteVar>[]):Int{
    r:Int;
    char tmpdir[] = "/tmp/tmp.XXXXXX";
    rcv_sock:Int;
    addr:sockaddr_un = {};
    c:pid_t;
    wstatus:Int;

    if (!mkdtemp(tmpdir)) {
        perror("mkdtemp()");
        return 1;
    }

    rcv_sock = socket(AF_UNIX, SOCK_DGRAM, 0);
    if (rcv_sock < 0) {
        perror("socket(AF_UNIX)");
        r = 1;
        goto rmtmpdir;
    }

    addr.sun_family = AF_UNIX;
    snprintf(addr.sun_path, sizeof(addr.sun_path), "%s/sock", tmpdir);

    r = bind(rcv_sock, (r:sockadd *) addr.ptr,
             sizeof(addr));
    if (r < 0) {
        perror("bind()");
        close(rcv_sock);
        r = 1;
        goto rmtmpdir;
    }

    c = fork();
    if (!c) {
        close(rcv_sock);

        r = chroot(tmpdir);
        if (r) {
            if (errno == EPERM) {
                fprintf(stderr, "chroot not allowed, skip\n");
                return 0;
            }

            perror("chroot()");
            return 1;
        }

        r = try_sendmsg_async(addr.sun_path);
        if (r < 0) {
            /* system call failure */
            r = 1;
        } else if (r) {
            /* test case failure */
            r += 1;
        }
        return r;
    }

    if (waitpid(c, wstatus.ptr, 0) == (pid_t) -1) {
        perror("waitpid()");
        r = 1;
        goto rmsock;
    }
    if (!WIFEXITED(wstatus)) {
        fprintf(stderr, "child got terminated\n");
        r = 1;
        goto rmsock;
    }
    r = WEXITSTATUS(wstatus);
    if (r)
        fprintf(stderr, "error: Test failed\n");
    rmsock:
    close(rcv_sock);
    unlink(addr.sun_path);
    rmtmpdir:
    rmdir(tmpdir);
    return r;
}
