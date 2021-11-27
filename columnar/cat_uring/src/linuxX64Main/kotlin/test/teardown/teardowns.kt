package test.teardown
import kotlinx.cinterop.*
import linux_uring.*
import platform.posix.waitpid
import simple.HasPosixErr
import simple.simple.CZero.nz

/* SPDX-License-Identifier: MIT */
//#include <stdint.h>
//#include <stdio.h>
//#include <stdlib.h>
//#include <string.h>
//#include <sys/types.h>
//#include <sys/wait.h>
//#include <unistd.h>
//#include <errno.h>
//
//#include "liburing.h"

fun loop() = memScoped {
    var ret = 0
    for (i in 0 until 100) {
        val ring: io_uring = alloc()


        //bzero memset(ring.ptr, 0, sizeOf<io_uring>( )  )
        var fd = io_uring_queue_init(0xa4, ring.ptr, 0)
        if (fd >= 0) {
            close(fd)
            continue
        }
        if (fd != -ENOMEM)
            ret++
    }
    exit(ret)
}

fun ____WEXITSTATUS(status: Int) = (((status) and 0xff00) shr 8)


fun main(): Unit = memScoped {
    for (i in 0 until 12) {
        if (!fork().nz) {
            loop()
            break
        }
    }

    var ret = 0
    val status: IntVar = alloc()
    for (i in 0 until 12) {
        HasPosixErr.posixFailOn(waitpid(-1, status.ptr, 0) < 0) {
            perror("waitpid")

        }
        if (____WEXITSTATUS(status.value).nz)
            ret++
    }

    println("returning status $ret")
    return
}
