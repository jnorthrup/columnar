package linux_uring

import kotlinx.cinterop.*
import linux_uring.include.UringOpcode.Op_Close
import linux_uring.include.UringOpcode.Op_Readv
import linux_uring.include.UringSetupFeatures
import linux_uring.include.UringSetupFeatures.featSingle_mmap
import linux_uring.include.UringSqeFlags.sqeIo_hardlink
import linux_uring.include.UringSqeFlags.sqeIo_link
import platform.linux.memalign
import platform.posix.NULL
import platform.posix.sigset_t
import simple.HasDescriptor.Companion.S_ISBLK
import simple.HasDescriptor.Companion.S_ISREG
import simple.HasPosixErr.Companion.posixRequires
import simple.PosixFile.Companion.getDirFd
import simple.PosixFile.Companion.namedDirAndFile
import simple.allocWithFlex
import simple.simple.CZero.nz
import simple.simple.CZero.z
import kotlin.math.min
import platform.linux.BLKGETSIZE64 as PlatformLinuxBLKGETSIZE64
import platform.posix.fstat as posix_fstat
import platform.posix.ioctl as posix_ioctl
import platform.posix.mmap as posix_mmap
import platform.posix.off_t as posix_off_t
import platform.posix.stat as posix_stat
import platform.posix.syscall as posix_syscall

//class app_io_sq_ring(
//    val head: CPointer<UIntVar>?,
//    val tail: CPointer<UIntVar>?,
//    val ring_mask: CPointer<UIntVar>?,
//    val ring_entries: CPointer<UIntVar>?,
//    val flags: CPointer<UIntVar>?,
//    val array: CPointer<UIntVar>?,
//)

//class app_io_cq_ring(
//    val head: CPointer<UIntVar>?,
//    val tail: CPointer<UIntVar>?,
//    val ring_mask: CPointer<UIntVar>?,
//    val ring_entries: CPointer<UIntVar>?,
//    val cqes: CPointer<io_uring_cqe>?
//)


class submitter {
    val p: io_uring_params = nativeHeap.alloc()
    val ring_fd =
        io_uring_setup(CATQUEUE_DEPTH.toUInt(), p.ptr).also { ring_fd -> posixRequires(ring_fd >= 0, { ring_fd }) }
    val featuresInUse: Set<UringSetupFeatures> =
        (UringSetupFeatures.values().fold(setOf<UringSetupFeatures>()) { a, x ->
            when {
                (x.feat_const and p.features).nz -> a + x
                else -> a
            }
        }).also {
            fprintf(stderr, "p.sq_entries: ${p.sq_entries}")
            fprintf(stderr, "io_uring features: $it")
        }

    val singleMmap = featSingle_mmap in featuresInUse

    val sring_sz = (p.sq_off.array + p.sq_entries * UInt.SIZE_BYTES.toUInt())
    val cring_sz = (p.cq_off.cqes + p.cq_entries * sizeOf<io_uring_cqe>().toUInt())
    val sqes = run {
        read_barrier()
        posix_mmap(
            NULL,
            (p.sq_entries * sizeOf<io_uring_sqe>().toUInt()).toULong(),
            platform.posix.PROT_READ or platform.posix.PROT_WRITE,
            platform.posix.MAP_SHARED or platform.posix.MAP_POPULATE,
            ring_fd,
            IORING_OFF_SQES.toLong()
        )!!.reinterpret<io_uring_sqe>().also {
            posixRequires(it != platform.posix.MAP_FAILED) { "mmap" }
        }
    }
    val sqPtr = run {
        val __len = sring_sz.toULong()
        val __prot = platform.posix.PROT_READ or platform.posix.PROT_WRITE
        val __flags = platform.posix.MAP_SHARED or platform.posix.MAP_POPULATE
        val __offset = IORING_OFF_SQ_RING.toLong()
        mapIORingQueue(__len, __prot, __flags, __offset)
    }
    val sqRing = appIOSqRing(sqPtr)
    val cqPtr = if (singleMmap) sqPtr
    else {
        val __len = cring_sz.toULong()
        val __prot = platform.posix.PROT_READ or platform.posix.PROT_WRITE
        val __flags = platform.posix.MAP_SHARED or platform.posix.MAP_POPULATE
        val __offset = IORING_OFF_CQ_RING.toLong()
        mapIORingQueue(__len, __prot, __flags, __offset)
    }

    val cqRing = appIOCqRing(cqPtr)

    inner class appIOSqRing(sqptr1: CPointer<ByteVar>, val sqptr: Long = sqptr1.toLong()) {
        val head get() = (sqptr + p.sq_off.head.toLong()).toCPointer<UIntVar>()!!
        val tail get() = (sqptr + p.sq_off.tail.toLong()).toCPointer<UIntVar>()!!
        val ring_mask get() = (sqptr + p.sq_off.ring_mask.toLong()).toCPointer<UIntVar>()!!
        val ring_entries get() = (sqptr + p.sq_off.ring_entries.toLong()).toCPointer<UIntVar>()!!
        val flags get() = (sqptr + p.sq_off.flags.toLong()).toCPointer<UIntVar>()!!
        val array get() = (sqptr + p.sq_off.array.toLong()).toCPointer<UIntVar>()!!
    }

    inner class appIOCqRing(cqptr1: CPointer<ByteVar>, val cqptr: Long = cqptr1.toLong()) {
        val head get() = (cqptr + p.cq_off.head.toLong()).toCPointer<UIntVar>()!!
        val tail get() = (cqptr + p.cq_off.tail.toLong()).toCPointer<UIntVar>()!!
        val ring_mask get() = (cqptr + p.cq_off.ring_mask.toLong()).toCPointer<UIntVar>()!!
        val ring_entries get() = (cqptr + p.cq_off.ring_entries.toLong()).toCPointer<UIntVar>()!!
        val cqes get() = (cqptr + p.cq_off.cqes.toLong()).toCPointer<io_uring_cqe>()
    }

    fun mapIORingQueue(__len: ULong, __prot: Int, __flags: Int, __offset: Long): CPointer<ByteVar> {
        read_barrier()
        val qringPtr = posix_mmap(NULL, __len, __prot, __flags, ring_fd, __offset)!!.reinterpret<ByteVar>()
        posixRequires(qringPtr != platform.posix.MAP_FAILED) { "mmap" }
        return qringPtr
    }

    fun opReadWholeFile(file_fd: Int) = nativeHeap.run {
        val triple = sqePreamble()
        val sqe: CPointer<io_uring_sqe> = sqes[triple.third.toInt()].ptr
        //---opcode
        createfileInfoReaderSqe(file_fd, sqe)
        //---end opcode
        sqeSubmit(triple)
    }

    fun opCloseCatFile(file_fd: Int) = nativeHeap.run {
        val triple = sqePreamble()
        val sqe: CPointer<io_uring_sqe> = sqes[triple.third.toInt()].ptr
        //---opcode
        createfileInfoReaderSqe(file_fd, sqe)
        //---end opcode
        sqeSubmit(triple)
    }

    fun sqePreamble() = nativeHeap.run {
        val tail: UIntVar = alloc { value = sqRing.tail.pointed.value }
        var next_tail = tail.value
        next_tail++
        read_barrier()
        val index = tail.value and sqRing.ring_mask.pointed.value
        Triple(tail, next_tail, index)
    }

    fun sqeSubmit(triple: Triple<UIntVar, UInt, UInt>) {
        write_barrier()
        sqRing.array[triple.third.toInt()] = triple.third
        triple.first.value = triple.second
        /* Update the tail so the kernel can see it. */
        if (sqRing.tail.pointed.value != triple.first.value) {
            sqRing.tail.pointed.value = triple.first.value
            write_barrier()
        }
    }

    fun createfileInfoReaderSqe(
        file_fd: Int,
        sqe: CPointer<io_uring_sqe>
    ) = nativeHeap.run {
        val file_sz: posix_off_t = get_file_size(file_fd)
        var blocks: Int = (file_sz / BLOCK_SZ).toInt()
        val fi: file_info = allocWithFlex(file_info::iovecs, blocks)
        fi.file_sz = file_sz
        if (file_sz >= 0L && 0 != (file_sz % BLOCK_SZ).toInt()) blocks++
        /* For each block of the file we need to read, we allocate an iovec struct
             * which is indexed into the iovecs array. This array is passed in as part
             * of the submission. If you don't understand this, then you need to look
             * up how the readv() and writev() system calls work.
             */
        for ((chunk, remains) in (file_sz downTo 0L step tehBlockSize).withIndex())
            fi.iovecs[chunk].run {
                iov_len = min(remains, tehBlockSize).toULong()
                iov_base = memalign(BLOCK_SZ, BLOCK_SZ)
            }
        opReadToFileinfo(sqe, file_fd, fi, blocks)
    }

    fun opReadToFileinfo(
        sqe: CPointer<io_uring_sqe>,
        file_fd: Int,
        fi: file_info,
        blocks: Int
    ) {
        sqe.pointed.run {
            fd = file_fd
            flags = (sqeIo_link.flagConstant or sqeIo_hardlink.flagConstant).toUByte()
            opcode = Op_Readv.opConstant.toUByte()
            addr = fi.iovecs.toLong().toULong()
            len = blocks.toUInt()
            off = 0.toULong()
            user_data = fi.ptr.toLong().toULong()
        }
    }

    fun opCloseFd(
        sqe: CPointer<io_uring_sqe>,
        file_fd: Int,
    ) {
        val triple=sqePreamble()
        sqe.pointed.run {
            fd = file_fd
            flags = sqeIo_link.flagConstant.toUByte()
            opcode = Op_Close.opConstant.toUByte()
        }
        sqeSubmit(triple)
    }
}


/** This code is written in the days when io_uring-related system calls are not
 *  part of standard C libraries. So, we roll our own system call wrapper
 *  functions.
 */
fun io_uring_setup(entries: UInt, p: CPointer<io_uring_params>) =
    posix_syscall(__NR_io_uring_setup.toLong(), entries, p).toInt()

fun io_uring_enter(ring_fd: Int, to_submit: UInt, min_complete: UInt, flags: UInt, sig: CPointer<sigset_t>? = null) =
    posix_syscall(__NR_io_uring_enter.toLong(), ring_fd, to_submit, min_complete, flags, 0L, sig).toInt()

/** Returns the size of the file whose open file descriptor is passed in.
 *  Properly handles regular file and block devices as well. Pretty. */
fun get_file_size(fd: Int): posix_off_t = memScoped {
    val st: posix_stat = alloc()
    if (posix_fstat(fd, st.ptr as CValuesRef<posix_stat>) >= 0) {
        if (S_ISBLK(st.st_mode)) {
            return getBlockDeviceBlockSize(fd)
        } else posixRequires(S_ISREG(st.st_mode)) { "file handle invalid" }
    }
    return st.st_size
}

private fun MemScope.getBlockDeviceBlockSize(fd: Int): platform.posix.off_t {
    val bytes: LongVar = alloc()
    posixRequires(posix_ioctl(fd, PlatformLinuxBLKGETSIZE64, bytes.ptr).z) { ("ioctl") }
    return bytes.value /* = kotlin.Long */
}


/**
 * Output a string of characters of len length to stdout.
 * We use buffered output here to be efficient,
 * since we need to output character-by-character.
 * */
fun output_to_console(buf: CPointer<ByteVar>, len: Int): Unit {
    print(buf.toKStringFromUtf8().take(len))
}


/**
 * Read from completion queue.
 * In this function, we read completion events from the completion queue, get
 * the data buffer that will have the file data and print it to the console.
 * */

fun completionQueues(s: submitter) {
    var fi: CPointer<file_info>
    var cqe: CPointer<io_uring_cqe>
    val cring = s.cqRing
    var head = cring.head.pointed.value
    println("entering loop")
    do {
        read_barrier()
        /*
         * Remember, this is a ring buffer. If head == tail, it means that the
         * buffer is empty.
         * */
        if (head == cring.tail.pointed.value) break
        /* Get the entry */
        val value = head.toLong() and s.cqRing.ring_mask.pointed.value.toLong()
        cqe = cring.cqes!![value.toInt()].ptr
        fi = cqe.pointed.user_data.toLong().toCPointer()!!
        posixRequires(cqe.pointed.res >= 0) { "Error: ${cqe.pointed.res}" }

        var blocks: Int = (fi.pointed.file_sz / BLOCK_SZ.toLong()).toInt()
        if (0L != fi.pointed.file_sz % BLOCK_SZ) blocks++
        for (i in 0 until blocks) output_to_console(
            fi.pointed.iovecs[i].iov_base!!.reinterpret(),
            fi.pointed.iovecs[i].iov_len.toInt()
        )
        head++
    } while (true)

    write_barrier()
    cring.head.pointed.value = head
    write_barrier()
}

val tehBlockSize: Long = BLOCK_SZ.toLong()
val pvtHandle = 2113u

/*
 * Submit to submission queue.
 * In this function, we submit requests to the submission queue. You can submit
 * many types of requests. Ours is going to be the readv() request, which we
 * specify via IORING_OP_READV. */
fun createSubmission(file_path: String, s: submitter): Int {
    val namedDirAndFile1 = namedDirAndFile(file_path)
    nativeHeap.run {
        val dirfd = getDirFd(namedDirAndFile1)// never closed
        val file_fd = openat(dirfd, file_path, platform.posix.O_RDONLY)// never closed
        posixRequires(file_fd >= 0) { "fileopen $file_fd" }

        s.opReadWholeFile(file_fd)
    }


    /***************************************************************************
     * Tell the kernel we have submitted events with the io_uring_enter() system
     * call. We also pass in the IOURING_ENTER_GETEVENTS flag which causes the
     * io_uring_enter() call to wait until min_complete events (the 3rd param)
     * complete.
     *
     * ************** BLOCKS HERE
     *
     */
    io_uring_enter(s.ring_fd, 1.toUInt(), 1.toUInt(), IORING_ENTER_GETEVENTS).let { ret ->
        posixRequires(ret.nz) { "io_uring_enter $ret" }
    }


    return 0
}


fun cat_file(argv1: Array<String>): Unit = memScoped {
    val argv = argv1.takeIf { it.isNotEmpty() } ?: arrayOf("/etc/sysctl.conf")

    println("---setting up uring with args ${argv.toList()}")
    val s = submitter()

    println("---success setting up uring with args ${argv.toList()}")
    for (arg in argv) {
        //will block
        posixRequires(!createSubmission(arg, s).nz) { "Error reading file" }

        println("---calling read_from_cq(${s})")
        //done blocking
        completionQueues(s)
        println("---back from read_from_cq(${s})")
    }
    println("---exiting")
    return
}
