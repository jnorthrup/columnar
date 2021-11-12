@file:Suppress("FunctionName")

package uring

import kotlinx.cinterop.*
import platform.linux.BLKGETSIZE64
import platform.posix.*
import platform.posix.__S_IFBLK
import platform.posix.__S_IFCHR
import platform.posix.__S_IFDIR
import platform.posix.__S_IFIFO
import platform.posix.__S_IFLNK
import platform.posix.__S_IFMT
import platform.posix.__S_IFREG
import platform.posix.off_t
import platform.posix.stat
import kotlin.reflect.KProperty1


import platform.posix.ioctl as posix_ioctl

fun __S_ISTYPE(mode: Int, mask: Int) = (((mode) and __S_IFMT) == (mask))
fun S_ISDIR(mode: Int) = __S_ISTYPE((mode), __S_IFDIR)
fun S_ISCHR(mode: Int) = __S_ISTYPE((mode), __S_IFCHR)
fun S_ISBLK(mode: Int) = __S_ISTYPE((mode), __S_IFBLK)
fun S_ISREG(mode: Int) = __S_ISTYPE((mode), __S_IFREG)
fun S_ISFIFO(mode: Int) = __S_ISTYPE((mode), __S_IFIFO)
fun S_ISLNK(mode: Int) = __S_ISTYPE((mode), __S_IFLNK)


inline fun <reified A : CStructVar, reified B : CVariable> NativePlacement.allocWithFlex(
    bProperty: KProperty1<A, CPointer<B>>,
    count: Int,
): A = alloc(sizeOf<A>() + sizeOf<B>() * count, alignOf<A>()).reinterpret()

fun fileInfo(iovec_count: Int): file_info = nativeHeap.allocWithFlex(file_info::iovecs, iovec_count)


/*
* Returns the size of the file whose open file descriptor is passed in.
* Properly handles regular file and block devices as well. Pretty.
* local-scope safe
 */
fun get_file_size(fd: Int): off_t = memScoped {
    print("get_filesize")
    val st: stat = alloc()

    if (fstat(fd, st.ptr) < 0) {
        perror("fstat")
        return -1
    }
    if (S_ISBLK(st.st_mode.toInt())) {
        val bytes: ULongVar = alloc()
        bytes.usePinned {
            if (posix_ioctl(fd, BLKGETSIZE64, it.get().ptr) == 0)
                return it.get().value.toLong()
        }
        perror("ioctl")
        return -1

    } else if (S_ISREG(st.st_mode.toInt()))
        return st.st_size
    return -1
}

/*
 * Output a string of characters of len length to stdout.
 * We use buffered output here to be efficient,
 * since we need to output character-by-character.
 * */
fun output_to_console(buf: CPointer<ByteVar>, len: Int) {
    print("output_to_console")
    println(buf.toKString().take(len))
}

/*
 * Wait for a completion to be available, fetch the data from
 * the readv operation and print it to the console.
 * */

fun get_completion_and_print(ring: CPointer<io_uring>): Int = memScoped {
    print("get_completion_and_print")
    val cqe: io_uring_cqe = nativeHeap.alloc<io_uring_cqe>()
    print("get_completion_and_print1")
    val ret: Int = io_uring_wait_cqe(ring!!, cqe!!.ptr!!.reinterpret<CPointerVar<io_uring_cqe>>()!!.get(0)!!.toLong().toCPointer<CPointerVar<io_uring_cqe>>()!!.reinterpret())
    print("get_completion_and_print1.1 $ret")
  if (ret >= 0) {
        print("get_completion_and_print2")

        if (cqe.res >= 0) {
            val fi: CPointer<file_info> = io_uring_cqe_get_data(cqe.ptr)!!.reinterpret()
            var blocks: Int = (fi.pointed.file_sz / BLOCK_SZ.toLong()).toInt()
            if (0L != fi.pointed.file_sz % BLOCK_SZ.toLong()) blocks++
            print("get_completion_and_print3")
            for (i in 0 until blocks)
                output_to_console(
                    fi.pointed.iovecs[i].iov_base!!.reinterpret(),
                    fi.pointed.iovecs[i].iov_len.toInt())
            io_uring_cqe_seen(ring, cqe.ptr)
            print("get_completion_and_print4")
            return 0
        } else fprintf(stderr, "Async readv failed.\n")
    } else perror("io_uring_wait_cqe")
    return 1
}
/*
 * Submit the readv request via liburing
 * */

fun submit_read_request(file_path: String, ring: CPointer<io_uring>): Int {
    println("submit_read_request")
    val file_fd: Int = open(file_path, O_RDONLY)
    if (file_fd >= 0) {
        val file_sz: off_t = get_file_size(file_fd)
        var bytes_remaining: off_t = file_sz
        var offset: off_t = 0
        var current_block: Int = 0
        var blocks: Int = (file_sz / BLOCK_SZ.toLong()).toInt()
        if (0 != (file_sz % BLOCK_SZ.toLong()).toInt()) blocks++
        val fi = fileInfo(blocks)

        /*
         * For each block of the file we need to read, we allocate an iovec struct
         * which is indexed into the iovecs array. This array is passed in as part
         * of the submission. If you don't understand this, then you need to look
         * up how the readv() and writev() system calls work.
         */
        while (0L != bytes_remaining) {
            var bytes_to_read: off_t = bytes_remaining
            println("++remaining $bytes_remaining")
            if (bytes_to_read > BLOCK_SZ)
                bytes_to_read = BLOCK_SZ.toLong()

            offset += bytes_to_read

            fi.iovecs[current_block].iov_len = bytes_to_read.toULong()

            val buf: COpaquePointerVar = nativeHeap.alloc()
            if (0 != posix_memalign(buf.ptr, BLOCK_SZ, BLOCK_SZ)) {
                perror("posix_memalign")
                return 1
            }
            println("submit_read_request done")
            fi.iovecs[current_block].iov_base = buf.ptr

            current_block++
            bytes_remaining -= bytes_to_read
        }
        fi.file_sz = file_sz

        /* Get an SQE */
        println("/* Get an SQE */")
        val sqe: CPointer<io_uring_sqe> = io_uring_get_sqe(ring)!!
        /* Setup a readv operation */
        println("/* Setup a readv operation */")
        io_uring_prep_readv(sqe, file_fd, fi.iovecs, blocks.toUInt(), 0)
        /* Set user data */
        println("/* Set user data */")
        io_uring_sqe_set_data(sqe, fi.ptr)
        /* Finally, submit the request */
        println("/* Finally, submit the request */")
        io_uring_submit(ring)
        return 0
    }
    perror("open")
    return 1
}

fun main(args1: Array<String>) {
    val ring: io_uring = nativeHeap.alloc()
    val args = args1.takeIf { it.isNotEmpty() } ?: arrayOf("/etc/sysctl.conf")
    require(args.isNotEmpty()) { "Usage: %0 [file name] <[file name] ...>\n" }

    val initStatus = io_uring_queue_init(QUEUE_DEPTH, ring.ptr, 0)
    /* Initialize io_uring */
    require(0 == initStatus) { "uring init failed." }
    println("initStatus: $initStatus")
    for (arg in args) {
        val ret: Int = submit_read_request(arg, ring.ptr)
        require(0 == ret) { "Error reading file: $arg" }
        get_completion_and_print(ring.ptr)
    }

    /* Call the clean-up function. */
    io_uring_queue_exit(ring.ptr)

}