package uring

import kotlin.*
import kotlinx.cinterop.*
import platform.linux.memalign
import platform.posix.*
import simple.HasDescriptor.Companion.S_ISBLK
import simple.HasDescriptor.Companion.S_ISREG
import simple.HasPosixErr.Companion.posixRequires
import simple.allocWithFlex
import simple.simple.CZero.nz
import simple.simple.CZero.z
import platform.posix.bzero as posix_bzero
import platform.linux.BLKGETSIZE64 as PlatformLinuxBLKGETSIZE64
import platform.posix.fstat as posix_fstat
import platform.posix.ioctl as posix_ioctl
import platform.posix.mmap as posix_mmap
import platform.posix.off_t as posix_off_t
import platform.posix.perror as posix_perror
import platform.posix.stat as posix_stat
import platform.posix.syscall as posix_syscall

/*
 * This code is written in the days when io_uring-related system calls are not
 * part of standard C libraries. So, we roll our own system call wrapper
 * functions.
 * */

fun io_uring_setup(entries: UInt, p: CPointer<io_uring_params>): Int =
    posix_syscall(__NR_io_uring_setup.toLong(), entries, p).toInt()

fun io_uring_enter(ring_fd: Int, to_submit: UInt, min_complete: UInt, flags: UInt): Int =
    posix_syscall(__NR_io_uring_enter.toLong(), ring_fd, to_submit, min_complete, flags, 0L, 0).toInt()

/*
 * Returns the size of the file whose open file descriptor is passed in.
 * Properly handles regular file and block devices as well. Pretty.
 * */

fun get_file_size(fd: Int): posix_off_t = memScoped {
    val st: posix_stat = alloc()
    if (posix_fstat(fd, st.ptr as CValuesRef<posix_stat>) >= 0) {
        if (S_ISBLK(st.st_mode)) {
            val bytes: CPointerVar<LongVar> = alloc()
            return if (posix_ioctl(fd, PlatformLinuxBLKGETSIZE64, bytes) == 0) bytes.pointed!!.value.toLong()
            else {
                posix_perror("ioctl")
                -1
            }
        } else if (S_ISREG(st.st_mode)) return st.st_size else return -1
    }
    posix_perror("fstat")
    return -1
}

/**
 * io_uring requires a lot of setup which looks pretty hairy, but isn't all
 * that difficult to understand. Because of all this boilerplate code,
 * io_uring's author has created liburing, which is relatively easy to use.
 * However, you should take your time and understand this code. It is always
 * good to know how it all works underneath. Apart from bragging rights,
 * it does offer you a certain strange geeky peace.
 */

fun app_setup_uring(s: CPointer<submitter>): Int = kotlinx.cinterop.nativeHeap.run {
    val sring: CPointer<app_io_sq_ring> = s.pointed.sq_ring.ptr
    val cring: CPointer<app_io_cq_ring> = s.pointed.cq_ring.ptr
    val p: io_uring_params = alloc<io_uring_params>()


    /*
     * We need to pass in the io_uring_params structure to the io_uring_setup()
     * call zeroed out. We could set any flags if we need to, but for this
     * example, we don't.
     * */
    posix_bzero(p.ptr, sizeOf<io_uring_params>().toULong())

    s.pointed.ring_fd = io_uring_setup(QUEUE_DEPTH.toUInt(), p.ptr)
    val ringFd = s.pointed.ring_fd
    val mustBe = ringFd >= 0
    posixRequires(mustBe, ringFd)
    /*
 * io_uring communication happens via 2 shared kernel-user space ring buffers,
 * which can be jointly mapped with a single mmap() call in recent kernels.
 * While the completion queue is directly manipulated, the submission queue
 * has an indirection array in between. We map that in as well.
 * */

    val sqOff: io_sqring_offsets = p.sq_off
    var sring_sz: Int = (sqOff.array + p.sq_entries * UInt.SIZE_BYTES.toUInt()).toInt()
    val cqOff: io_cqring_offsets = p.cq_off
    var cring_sz: Int = (cqOff.cqes + p.cq_entries * sizeOf<io_uring_cqe>().toUInt()).toInt()

    /* In kernel version 5.4 and above, it is possible to map the submission and
     * completion buffers with a single mmap() call. Rather than check for kernel
     * versions, the recommended way is to just check the features field of the
     * io_uring_params structure, which is a bit mask. If the
     * IORING_FEAT_SINGLE_MMAP is set, then we can do away with the second mmap()
     * call to map the completion ring.
     */
    if ((p.features and IORING_FEAT_SINGLE_MMAP).nz) {
        if (cring_sz > sring_sz) {
            sring_sz = cring_sz
        }
        cring_sz = sring_sz
    }


    /* Map in the submission and completion queue ring buffers.
 * Older kernels only map in the submission queue, though.
 * */
    val sq_ptr = posix_mmap(NULL,
        sring_sz.toULong(),
        platform.posix.PROT_READ or platform.posix.PROT_WRITE,
        platform.posix.MAP_SHARED or platform.posix.MAP_POPULATE,
        ringFd,
        IORING_OFF_SQ_RING.toLong())!!.reinterpret<ByteVar>()
    posixRequires(sq_ptr != platform.posix.MAP_FAILED) { "mmap" }
    val cq_ptr: CPointer<ByteVar>
    if ((p.features and IORING_FEAT_SINGLE_MMAP).nz) {
        cq_ptr = sq_ptr
    } else {
        /* Map in the completion queue ring buffer in older kernels separately */
        cq_ptr = mmap(NULL,
            cring_sz.toULong(),
            PROT_READ or PROT_WRITE,
            MAP_SHARED or MAP_POPULATE,
            ringFd,
            IORING_OFF_CQ_RING.toLong())!!.reinterpret()
       posixRequires (cq_ptr != MAP_FAILED) { "mmap" }
    }
    /* Save useful fields in a global app_io_sq_ring r:fo later easy reference */
    val sqLong = sq_ptr.toLong()
    sqLong.let { sqLong ->
        val toLong = sqOff.head.toLong()
        val toLong1 = sqOff.tail.toLong()
        val toLong2 = sqOff.ring_mask.toLong()
        val toLong3 = sqOff.ring_entries.toLong()
        val toLong4 = sqOff.flags.toLong()
        val toLong5 = sqOff.array.toLong()
        sring.pointed.head = (sqLong + toLong).toCPointer() 
        sring.pointed.tail = (sqLong + toLong1).toCPointer()
        sring.pointed.ring_mask = (sqLong + toLong2).toCPointer()
        sring.pointed.ring_entries = (sqLong + toLong3).toCPointer()
        sring.pointed.flags = (sqLong + toLong4).toCPointer()
        sring.pointed.array = (sqLong + toLong5).toCPointer()
    }

    /* Map in the submission queue entries array */
    s.pointed.sqes = mmap(NULL,
        (p.sq_entries * sizeOf<io_uring_sqe>().toUInt()).toULong(),
        platform.posix.PROT_READ or platform.posix.PROT_WRITE,
        platform.posix.MAP_SHARED or platform.posix.MAP_POPULATE,
        ringFd,IORING_OFF_SQES.toLong())!!.reinterpret()
    if (s.pointed.sqes == platform.posix.MAP_FAILED) {
        posix_perror("mmap")
        return 1
    }


    cq_ptr.toLong().let { cq_ptr ->
        /* Save useful fields in a global app_io_cq_ring r:fo later
* easy reference */
        cring.pointed.head = (cq_ptr + cqOff.head.toLong()).toCPointer()
        cring.pointed.tail = (cq_ptr + cqOff.tail.toLong()).toCPointer()
        cring.pointed.ring_mask = (cq_ptr + cqOff.ring_mask.toLong()).toCPointer()
        cring.pointed.ring_entries = (cq_ptr + cqOff.ring_entries.toLong()).toCPointer()
        cring.pointed.cqes = (cq_ptr + cqOff.cqes.toLong()).toCPointer()
    }

    return 0

}


/*
 * Output a string of characters of len length to stdout.
 * We use buffered output here to be efficient,
 * since we need to output character-by-character.
 * */
fun output_to_console(buf: CPointer<ByteVar>, len: Int): Unit {
    print(buf.toKStringFromUtf8().take(len))
}


/*
* Read from completion queue.
* In this function, we read completion events from the completion queue, get
* the data buffer that will have the file data and print it to the console.
* */

fun read_from_cq(s: CPointer<submitter>): Unit {
    var fi: CPointer<file_info>
    val cring: CPointer<app_io_cq_ring> = s.pointed.cq_ring.ptr
    var cqe: CPointer<io_uring_cqe>
    var head = cring.pointed.head!!.pointed.value
    println("entering loop")
    do {
        read_barrier()

        /*
         * Remember, this is a ring buffer. If head == tail, it means that the
         * buffer is empty.
         * */
        if (head == cring.pointed.tail!!.pointed.value) break

        /* Get the entry */
        val value = head.toLong() and s.pointed.cq_ring.ring_mask!!.pointed.value.toLong()
        cqe = cring.pointed.cqes!![value.toInt()].ptr
        fi = cqe.pointed.user_data.toLong().toCPointer<file_info>()!!
        if (cqe.pointed.res < 0) fprintf(stderr, "Error: %s\n", strerror(abs(cqe.pointed.res)))

        var blocks: Int = (fi.pointed.file_sz / BLOCK_SZ.toLong()).toInt()
        if (0L != fi.pointed.file_sz % BLOCK_SZ) blocks++

        for (i/*as int */ in 0 until blocks) output_to_console(fi.pointed.iovecs[i].iov_base!!.reinterpret(),
            fi.pointed.iovecs[i].iov_len.toInt())

        head++

    } while (true)

    cring.pointed.head!!.pointed.value = head
    write_barrier()
}

/*
 * Submit to submission queue.
 * In this function, we submit requests to the submission queue. You can submit
 * many types of requests. Ours is going to be the readv() request, which we
 * specify via IORING_OP_READV.
 *
 * */
fun submit_to_sq(file_path: String, s: CPointer<submitter>): Int = nativeHeap.run {
    val file_fd: Int = open(file_path, O_RDONLY)
    posixRequires(file_fd >= 0) { "fileopen $file_fd" }
    val sring: CPointer<app_io_sq_ring> = s.pointed.sq_ring.ptr
    var current_block = 0U

    val file_sz: posix_off_t = get_file_size(file_fd)
    if (file_sz >= 0L) {
        var bytes_remaining: posix_off_t = file_sz
        var blocks: Int = (file_sz / BLOCK_SZ).toInt()
        if (0 != (file_sz % BLOCK_SZ).toInt()) blocks++

        val fi: file_info = allocWithFlex(file_info::iovecs, blocks)//malloc(sizeof(*fi) + sizeof(c:iove) * blocks)
        fi.file_sz = file_sz

        /*
         * For each block of the file we need to read, we allocate an iovec struct
         * which is indexed into the iovecs array. This array is passed in as part
         * of the submission. If you don't understand this, then you need to look
         * up how the readv() and writev() system calls work.
         * */
        while (0L != bytes_remaining) {
            var bytes_to_read: posix_off_t = bytes_remaining
            val tehBlockSize: Long = BLOCK_SZ.toLong()
            if (bytes_to_read > tehBlockSize)
                bytes_to_read = tehBlockSize
            val curBlock = current_block.toInt()
            fi.iovecs[curBlock].iov_len = bytes_to_read.toULong()
            fi.iovecs[curBlock].iov_base = memalign(BLOCK_SZ, BLOCK_SZ)
            current_block++
            bytes_remaining -= bytes_to_read
        }

        /* Add our submission queue entry to the tail of the SQE ring buffer */
        var tail = sring.pointed.tail!!.pointed.value
        var next_tail = tail
        next_tail++
        read_barrier()
        val index = tail and s.pointed.sq_ring.ring_mask!!.pointed.value
        val sqe: CPointer<io_uring_sqe> = s.pointed.sqes!![index.toInt()].ptr


        sqe.pointed.run {
            fd = file_fd
            flags = 0.toUByte()
            opcode = IORING_OP_READV.toUByte()
            addr = fi.iovecs.toLong().toULong()
            len = blocks.toUInt()
            off = 0.toULong()
            user_data = fi.ptr.toLong().toULong()
        }
        sring.pointed.array!![index.toInt()] = index
        tail = next_tail

        /* Update the tail so the kernel can see it. */
        if (sring.pointed.tail!!.pointed.value != tail) {
            sring.pointed.tail!!.pointed.value = tail
            write_barrier()
        }

        /*
         * Tell the kernel we have submitted events with the io_uring_enter() system
         * call. We also pass in the IOURING_ENTER_GETEVENTS flag which causes the
         * io_uring_enter() call to wait until min_complete events (the 3rd param)
         * complete.
         *
         * ************** BLOCKS HERE
         * ************** BLOCKS HERE
         * ************** BLOCKS HERE
         * ************** BLOCKS HERE
         * ************** BLOCKS HERE
         * ************** BLOCKS HERE
         *
         */
        val ret: Int = io_uring_enter(s.pointed.ring_fd, 1.toUInt(), 1.toUInt(), IORING_ENTER_GETEVENTS)
        posixRequires(ret.nz, "io_uring_enter $ret")

        return 0
    }
    return 1
}

fun main(argv1: Array<String>): Unit {
    val s: submitter = nativeHeap.alloc()
    val argv = argv1.takeIf { it.isNotEmpty() } ?: arrayOf("/etc/sysctl.conf")
    println("setting up uring with args ${argv.toList()}")

    val appSetupUring = app_setup_uring(s.ptr)
    posixRequires(appSetupUring.z, appSetupUring)
    println("success setting up uring with args ${argv.toList()}")

    for (arg in argv) {
        posixRequires(!submit_to_sq(arg, s.ptr).nz, "Error reading file")
        println("calling read_from_cq(${s})")
        read_from_cq(s.ptr)
        println("back from read_from_cq(${s})")
    }
    println("exiting")
    fprintf(stderr, "Unable to setup uring!\n")
    return
}