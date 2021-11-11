package uring

import kotlinx.cinterop.*
import platform.linux.BLKGETSIZE64
import platform.posix.*

//#define	__S_ISTYPE(mode, mask)	(((mode) __S_IFMT.ptr) == (mask))
//#define	S_ISDIR(mode)	 __S_ISTYPE((mode), __S_IFDIR)
//#define	S_ISCHR(mode)	 __S_ISTYPE((mode), __S_IFCHR)
//#define	S_ISBLK(mode)	 __S_ISTYPE((mode), __S_IFBLK)
//#define	S_ISREG(mode)	 __S_ISTYPE((mode), __S_IFREG)
//# define S_ISFIFO(mode)	 __S_ISTYPE((mode), __S_IFIFO)
fun __S_ISTYPE(mode: Int, mask: Int) = mode and __S_IFMT == mask
fun S_ISDIR(mode: Int) = __S_ISTYPE((mode), __S_IFDIR)
fun S_ISCHR(mode: Int) = __S_ISTYPE((mode), __S_IFCHR)
fun S_ISBLK(mode: Int) = __S_ISTYPE((mode), __S_IFBLK)
fun S_ISREG(mode: Int) = __S_ISTYPE((mode), __S_IFREG)
fun S_ISFIFO(mode: Int) = __S_ISTYPE((mode), __S_IFIFO)

//fun main(vararg a:String){}

/*
 * Returns the size of the file whose open file descriptor is passed in.
 * Properly handles regular file and block devices as well. Pretty.
 */

fun get_file_size(fd: Int): off_t = memScoped {
    val st: stat = alloc()

    if (fstat(fd, st.ptr) >= 0) {
        if (S_ISBLK(st.st_mode.toInt())) {

            val bytes: LongVar = alloc()


            if (0 == ioctl(fd, BLKGETSIZE64, bytes.pin().get().ptr)) {
                bytes.pin().unpin()
                return bytes.reinterpret<off_tVar>().value//goal
            }
            perror("ioctl")
            return -1
        } else {
            if (S_ISREG(st.st_mode.toInt()))
                return st.st_size
        }

        return -1
    }
    perror("fstat")
    return -1
}

/*
 * Output a string of characters of len length to stdout.
 * We use buffered output here to be efficient,
 * since we need to output character-by-character.
 * */
fun output_to_console(buf: CPointer<ByteVar>, len: Int): Unit = println(buf.toString().take(len))

const val BLOCK_SZ = 4 * 1024

fun read_and_print_file(file_name: String):Int= memScoped{

    val iovecs: CValuesRef<iovec>

    val file_fd: Int = open(file_name, O_RDONLY)
    if (file_fd >= 0) {
        val file_sz: off_t = get_file_size(file_fd)
        var bytes_remaining: off_t = file_sz
        var blocks: Int = (file_sz / BLOCK_SZ.toLong()).toInt()
        if (0L != (file_sz.toLong() % BLOCK_SZ.toLong())) {
            blocks++
        }
        iovecs = nativeHeap.allocArray<iovec>(blocks)

        var current_block: Int = 0

        /*
     * For the file we're reading, allocate enough blocks to be able to hold
     * the file data. Each block is described in an iovec structure, which is
     * passed to readv as part of the array of iovecs.
     * */
        while (0L != bytes_remaining) {
            var bytes_to_read: off_t = bytes_remaining
            if (bytes_to_read > BLOCK_SZ)
                bytes_to_read = BLOCK_SZ.toLong()


            memScoped {
                val buf: COpaquePointerVar = alloc()
                val pin = buf.pin()

                if (0 == posix_memalign(pin.get().ptr, BLOCK_SZ.toULong(), BLOCK_SZ.toULong())) {

                    iovecs[current_block].iov_base = pin.get().ptr
                    iovecs[current_block].iov_len = bytes_to_read.toULong()
                    current_block++
                    bytes_remaining -= bytes_to_read
                } else {
                    perror("posix_memalign")
                    return 1
                }
            }
        }

        /*
         * The readv() call will block until all iovec buffers are filled with
         * file data. Once it returns, we should be able to access the file data
         * from the iovecs and print them on the console.
         */
        val ret: Int = readv(file_fd, iovecs, blocks).toInt()

        if (ret >= 0) {
            for (i in 0 until blocks)
                println(iovecs[i].iov_base.toString().take(iovecs[i].iov_len.toInt()))
            return 0
        }
        perror("readv")
        return 1
    }
    perror("open")
    return 1
}
fun main(args: Array<String>) {

    require (args.size > 0) { "args=${args.map { it }}\n" +
            "Usage: <exe> <filename1> [<filename2> ...]\n" }

    /*
     * For each file that is passed in as the argument, call the
     * read_and_print_file() function.
     * */
    for (i: Int in 1 until args.size) {
        val readAndPrintFile = read_and_print_file(args[i])
        require (0!= readAndPrintFile) { "Error reading file ${args[i]}\n"
        }
    }
}
