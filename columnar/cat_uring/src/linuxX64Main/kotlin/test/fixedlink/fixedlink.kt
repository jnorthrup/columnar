package test.fixedlink


//import kotlin.jvm.JvmStatic
import kotlinx.cinterop.*
import linux_uring.*
import platform.posix.O_RDONLY
import platform.posix.calloc
import platform.posix.close
import platform.posix.free
import platform.posix.iovec
import platform.posix.open
import platform.posix.strlen
import simple.HasPosixErr
import simple.simple.CZero.z

/* SPDX-License-Identifier: MIT */


val IOVECS_LEN = 2
fun fixedlink(argv: Array<String>): Unit = memScoped {
    val argc: Int = argv.size
    val iovecs: CArrayPointer<iovec>/*[IOVECS_LEN]*/ =
        calloc(sizeOf<iovec>().toULong(), IOVECS_LEN.toULong())!!.reinterpret()
    val ring: io_uring = alloc()
    val i: Int
    var ret: Int

    if (argc > 1)
        return

	val fd = open("/dev/zero", O_RDONLY)
	HasPosixErr.posixRequires(!(fd < 0)) { "Failed to open /dev/zero" }

    HasPosixErr.posixRequires(!(io_uring_queue_init(32, ring.ptr.reinterpret(), 0) < 0)) {
        "Faild to init io_uring"
    }
    for (i in 0 until IOVECS_LEN) {
        iovecs[i].iov_base = malloc(64)
		iovecs[i].iov_len = 64.toULong()
	}

	ret = io_uring_register_buffers(ring.ptr, iovecs.reinterpret(), IOVECS_LEN.toUInt())
	HasPosixErr.posixRequires(ret.z) { "Failed to register buffers\n" }

    for (i in 0 until IOVECS_LEN) {
        val sqe: CPointer<io_uring_sqe> = io_uring_get_sqe(ring.ptr)!!
		val str: String = "#include <errno.h>"

		iovecs[i].iov_len = strlen(str)
		io_uring_prep_read_fixed(sqe, fd, iovecs[i].iov_base, str.length.toUInt(), 0, i)
		if (i == 0)
            io_uring_sqe_set_flags(sqe, IOSQE_IO_LINK)
		io_uring_sqe_set_data(sqe, str.cstr)
	}

    ret = io_uring_submit_and_wait(ring.ptr, IOVECS_LEN.toUInt())
	HasPosixErr.posixRequires(!(ret < 0)) { "Failed to submit IO" }
    HasPosixErr.posixRequires(!(ret < 2)) { "Submitted $ret, wanted $IOVECS_LEN" }

    for (i in 0 until IOVECS_LEN) {
        val cqe: CPointerVar<io_uring_cqe> = alloc()

        ret = io_uring_wait_cqe(ring.ptr, cqe.ptr)
        HasPosixErr.posixRequires(ret.z) {
            "wait_cqe=$ret"

        }
        HasPosixErr.posixRequires(cqe.pointed!!.res == iovecs[i].iov_len.toInt()) {
            "read: wanted ${iovecs[i].iov_len}, got ${cqe.pointed?.res}"
        }
        val cqe1 = cqe.value
        io_uring_cqe_seen(ring.ptr, cqe1)
	}

    close(fd)
	io_uring_queue_exit(ring.ptr)

	for (i in 0 until IOVECS_LEN)
        free(iovecs[i].iov_base)

	println("success")
}
