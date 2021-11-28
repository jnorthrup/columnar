package test.readwrite


import kotlinx.cinterop.*
import linux_uring.*
import linux_uring.include.t_create_buffers
import linux_uring.include.t_create_file
import linux_uring.include.t_create_ring
import linux_uring.include.t_register_buffers
import simple.simple.CZero.nz
import test.readwrite.AppState.Companion.end.*

/* SPDX-License-Identifier: MIT */
/*
 * Description: basic read/write tests with polled IO
 */
//#include <errno.h>
//#include <stdio.h>
//#include <unistd.h>
//#include <stdlib.h>
//#include <string.h>
//#include <fcntl.h>
//#include <sys/types.h>
//#include <sys/poll.h>
//#include <sys/eventfd.h>
//#include <sys/resource.h>
//#include "helpers.h"
//#include "liburing.h"
//#include "../src/syscall.h"

const val FILE_SIZE = (128 * 1024)
const val BS = 4096
const val BUFFERS = (FILE_SIZE / BS)


class AppState() : NativePlacement by nativeHeap {
    var vecs:  CValuesRef<iovec>  = cValue()
    var no_buf_select: IntVar = alloc()
    var no_iopoll: IntVar = alloc()


    fun provide_buffers(ring: CPointer<io_uring>): Int {

        val cqe: CPointerVar<io_uring_cqe> = alloc()
        var sqe: CPointer<io_uring_sqe>
        var ret = io_uring_submit(ring)


        for (i in 0 until BUFFERS) {
            sqe = io_uring_get_sqe(ring)!!
            memScoped{
                val iovec = vecs.getPointer(this)[i]
                io_uring_prep_provide_buffers(
                    sqe,
                    iovec.iov_base,
                    iovec.iov_len.toInt(),
                    1,
                    1,
                    i
                )
            }
        }

        if (ret != BUFFERS) {
            fprintf(stderr, "submit: %d\n", ret)
            return 1
        }

        for (i in 0 until BUFFERS) {
            ret = io_uring_wait_cqe(ring, cqe.ptr)
            val ioUringCqe = cqe.value!!.pointed
            if (ioUringCqe.res < 0) {
                fprintf(stderr, " cqe.pointed.res =%d\n", ioUringCqe.res)
                return 1
            }
            io_uring_cqe_seen(ring, ioUringCqe.ptr)
        }

        return 0
    }

    fun __test_io(file: String, ring: CPointer<io_uring>, write: Int, sqthread: Int, fixed: Int, buf_select: Int): Int =
        memScoped {
            val vp = vecs.getPointer(this)
        val fd: IntVar = alloc()
        val cqe: CPointerVar<io_uring_cqe> = alloc()

        val offset: off_tVar = alloc()
        var write = write
        var fixed = fixed
        if (buf_select.nz) {
            write = 0
            fixed = 0
        }
        if (buf_select.nz && provide_buffers(ring).nz)
            return 1

        var open_flags = if (write.nz)
            O_WRONLY
        else
            O_RDONLY
        open_flags = open_flags.or(__O_DIRECT)

        var goto: end? = null


        do {
            if (fixed.nz) {
                val ret = t_register_buffers(ring, vecs , BUFFERS.toUInt())
                if (ret == T_SETUP_SKIP) return 0
                if (ret != T_SETUP_OK) {
                    fprintf(stderr, "buffer reg failed: %d\n", ret)
                    goto = err
                    break
                }
            }

            fd.value = open(file, open_flags)
            if (fd.value < 0) {
                perror("file open")
                goto = err
                break
            }
            if (sqthread.nz) {
                val ret = io_uring_register_files(ring, fd.ptr, 1)
                if (ret.nz) {
                    fprintf(stderr, "file reg failed: %d\n", ret)
                    goto = err
                    break
                }
            }

            offset.value = 0
            var do_fixed: Int
            var use_fd: Int
            for (i in 0 until BUFFERS) {
                val sqe = io_uring_get_sqe(ring)!!

                offset.value = (BS * (rand() % BUFFERS)).toLong()
                if (write.nz) {
                    do_fixed = fixed
                    use_fd = fd.value

                    if (sqthread.nz)
                        use_fd = 0
                    if (fixed.nz && (i and 1).nz)
                        do_fixed = 0
                    if (do_fixed.nz) {
                        val iovec = vp[i]
                        io_uring_prep_write_fixed(
                            sqe, use_fd, iovec.iov_base,
                            iovec.iov_len.toUInt(),
                            offset.value.toULong(), i
                        )
                    } else {
                        io_uring_prep_writev(
                            sqe, use_fd, vp[i].reinterpret(), 1,
                            offset.value.toULong()
                        )
                    }
                } else {
                    do_fixed = fixed
                    use_fd = fd.value

                    if (sqthread.nz)
                        use_fd = 0
                    if (fixed.nz && (i and 1).nz)
                        do_fixed = 0
                    if (do_fixed.nz) {
                        io_uring_prep_read_fixed(
                            sqe, use_fd, vecs.getPointer(memScope)[i].iov_base,
                            vp[i].iov_len.toUInt(),
                            offset.value.toULong(), i
                        )
                    } else {
                        io_uring_prep_readv(
                            sqe, use_fd, vp[i].ptr, 1,
                            offset.value.toULong()
                        )
                    }

                }
                if (sqthread.nz)
                    sqe.pointed.flags = sqe.pointed.flags.plus(IOSQE_FIXED_FILE).toUByte()
                if (buf_select.nz) {
                    sqe.pointed.flags = sqe.pointed.flags.plus(IOSQE_BUFFER_SELECT).toUByte()
                    sqe.pointed.buf_group = buf_select.toUShort()
                    sqe.pointed.user_data = i.toULong()
                }
            }

            var ret = io_uring_submit(ring)
            if (ret != BUFFERS) {
                fprintf(stderr, "submit got %d, wanted %d\n", ret, BUFFERS)
                goto = err;break
            }

            for (i in 0 until BUFFERS) {
                ret = io_uring_wait_cqe(ring, cqe.ptr)
                if (ret.nz) {
                    fprintf(stderr, "wait_cqe=%d\n", ret)
                    goto = err;break
                } else {
                    val variadicArguments = cqe.value!!.pointed.res
                    if (variadicArguments == -EOPNOTSUPP) {
                        fprintf(stdout, "File/device/fs doesn't support polled IO\n")
                        no_iopoll.value = 1
                        goto = out;break
                    } else if (variadicArguments != BS) {
                        fprintf(stderr, "cqe res %d, wanted %d\n", variadicArguments, BS)
                        goto = err;break
                    }
                }
                io_uring_cqe_seen(ring, cqe.value)
            }

            if (fixed.nz) {
                ret = io_uring_unregister_buffers(ring)
                if (ret.nz) {
                    fprintf(stderr, "buffer unreg failed: %d\n", ret)
                    goto = err;break
                }
            }
            if (sqthread.nz) {
                ret = io_uring_unregister_files(ring)
                if (ret.nz) {
                    fprintf(stderr, "file unreg failed: %d\n", ret)
                    goto = err;break
                }
            }
        } while (false)

        return run {
            if (goto != err) {
                close(fd.value)
                0
            } else
                if (fd.value != -1)
                    close(fd.value)
            1
        }.also { goto = null }
    }


    /*
     * if we are polling io_uring_submit needs to always enter the
     * kernel to fetch events
     */
    fun test_io_uring_submit_enters(file: String): Int = memScoped{
        var vp = vecs.getPointer(this)
        val ring: io_uring = alloc()
        if (no_iopoll.value.nz)
            return 0

        val ring_flags = IORING_SETUP_IOPOLL
        var ret = io_uring_queue_init(64, ring.ptr, ring_flags)
        if (ret.nz) {
            fprintf(stderr, "ring create failed: %d\n", ret)
            return 1
        }

        val open_flags = O_WRONLY or __O_DIRECT
        var goto: end? = null
        val fd = open(file, open_flags)
        do {
            if (fd < 0) {
                perror("file open")
                goto = err;break
            }

            for (i in 0 until BUFFERS) {
                if(goto!=null)break;

                val offset: off_t = (BS * (rand() % BUFFERS)).toLong()
                val sqe: CPointer<io_uring_sqe> = io_uring_get_sqe(ring.ptr)!!
                io_uring_prep_writev(sqe, fd, vp [i].ptr, 1, offset.toULong())
                sqe.pointed.user_data = 1UL
            }

            /* submit manually to avoid adding IORING_ENTER_GETEVENTS */
            val ioUringFlushSq = __io_uring_flush_sq(ring.ptr)
            ret = io_uring_enter(
                ring.ring_fd, ioUringFlushSq.toUInt(), 0.toUInt(),
                0.toUInt(), null
            )
            if (ret < 0) {
                goto = err;break
            }

            for (i in 0 until 500) {
                if(goto!=null)break;
                ret = io_uring_submit(ring.ptr)
                if (ret != 0) {
                    fprintf(stderr, "still had %d sqes to submit, this is unexpected", ret)
                    goto = err;break
                }
                //io_uring_for_each_cqe translated
                //io_uring_for_each_cqe translated
                //io_uring_for_each_cqe translated
                //io_uring_for_each_cqe translated
                //io_uring_for_each_cqe translated
                //io_uring_for_each_cqe translated

                val cq: io_uring_cq = ring.cq
                var head: UInt = cq.khead?.pointed?.value ?: 0U

                /* io_uring_for_each_cqe(ring.ptr, head, cqe)*/
                while (goto == null) {
                    val maskir: UInt = (ring).cq.kring_mask!!.pointed.value
                    val key: UInt = head and maskir

                    read_barrier()
                    val theTail = (ring).cq.ktail!!.pointed.value
                    val cqe: CPointer<io_uring_cqe>? = if (head != theTail) {
                        (ring).cq.cqes!![key.toInt()].ptr
                    } else {
                        null
                    }

                    /* runs after test_io so should not have happened */
                    goto = if (cqe?.pointed!!.res == -EOPNOTSUPP) {
                        fprintf(stdout, "File/device/fs doesn't support polled IO\n")
                        err
                    } else
                        out

                    break
                    head++
                }
                goto ?: usleep(10000)
            }

        } while (false)

        return when(goto) {
            err-> {


                if (fd != -1)
                    close(fd)
            1
            }
            else-> {
                io_uring_queue_exit(ring.ptr)
                ret
            }
        }
    }

    fun test_io(file: String, write: Int, sqthread: Int, fixed: Int, buf_select: Int): Int {
        val ring: io_uring = alloc()
        var ret: Int
        val ring_flags = IORING_SETUP_IOPOLL

        if (no_iopoll.value.nz)
            return 0

        ret = t_create_ring(64, ring.ptr, ring_flags).toInt()
        if (ret == T_SETUP_SKIP.toInt())
            return 0
        if (ret != T_SETUP_OK.toInt()) {
            fprintf(stderr, "ring create failed: %d\n", ret)
            return 1
        }
        ret = __test_io(file, ring.ptr, write, sqthread, fixed, buf_select)
        io_uring_queue_exit(ring.ptr)
        return ret
    }

    fun probe_buf_select(): Int {
        val p: CPointerVar<io_uring_probe> = alloc()
        val ring: io_uring = alloc()

        val ret = io_uring_queue_init(1, ring.ptr, 0)
        if (ret.nz) {
            fprintf(stderr, "ring create failed: %d\n", ret)
            return 1
        }

        p.value = io_uring_get_probe_ring(ring.ptr)
        if (!(p.value.toLong()).nz || !io_uring_opcode_supported(p.value, IORING_OP_PROVIDE_BUFFERS.toInt()).nz) {
            no_buf_select.value = 1
            fprintf(stdout, "Buffer select not supported, skipping\n")
            return 0
        }
        io_uring_free_probe(p.value)
        return 0
    }

    fun main(args: Array<String>): Int {
        val fname: String
        if (probe_buf_select().nz)
            exit(1)

        if (args.size > 0) {
            fname = args[1]
        } else {
            srand(time(null).toUInt())
            fname = (
                    ".basic-rw-${rand()}-${getpid()}")
            t_create_file(fname, FILE_SIZE.toULong())
        }

        val buf_num = BUFFERS.toULong()
        val buf_size = BS.toULong()
        vecs = t_create_buffers(buf_num, buf_size).reinterpret()

        var nr = 16
        if (no_buf_select.value.nz)
            nr = 8

        var goto: end? = null

        do {
            for (i in 0 until nr) {
                val write: Int = (i and 1)
                val sqthread: Int = (i and 2)
                val fixed: Int = (i and 4)
                val buf_select: Int = (i and 8)

                val ret = test_io(fname, write, sqthread, fixed, buf_select)
                if (ret.nz) {
                    fprintf(
                        stderr, "test_io failed %d/%d/%d/%d\n",
                        write, sqthread, fixed, buf_select
                    )
                    goto = err;break
                }
                if (no_iopoll.value.nz)
                    break
            }

            val ret = test_io_uring_submit_enters(fname)
            if (ret.nz) {
                fprintf(stderr, "test_io_uring_submit_enters failed\n")
                goto = err;break
            }
        } while (false)
        if (fname != args[1])
            unlink(fname)
        return goto?.let { 1 } ?: 0
    }

    /*
     * Sync internal state with kernel ring state on the SQ side. Returns the
     * number of pending items in the SQ ring, for the shared ring.
     */
    fun __io_uring_flush_sq(ring: CPointer<io_uring>): Int {
        val sq: CPointer<io_uring_sq> = ring.pointed.sq.ptr
        val mask = sq.pointed.kring_mask!!.pointed.value
        var ktail = sq.pointed.ktail!!.pointed.value
        var to_submit = sq.pointed.sqe_tail - sq.pointed.sqe_head
        var goto: end?
        goto = null
        do {
            if (!to_submit.nz) {
                goto = out
                break
            }
            /* Fill in sqes that we have queued up, adding them to the kernel ring         */
            do {
                sq.pointed.array!![ktail.toInt() and mask.toInt()] = sq.pointed.sqe_head and mask
                ktail++
                sq.pointed.sqe_head++
            } while ((--to_submit).nz)

            /*
         * Ensure that the kernel sees the SQE updates before it sees the tail
         * update.
         */
            write_barrier()
            sq.pointed.ktail!!.pointed.value = ktail
            write_barrier()

        } while (false)

        /*
         * This _may_ look problematic, as we're not supposed to be reading
         * SQ.pointed.head  without acquire semantics. When we're in SQPOLL mode, the
         * kernel submitter could be updating this right now. For non-SQPOLL,
         * task itself does it, and there's no potential race. But even for
         * SQPOLL, the load is going to be potentially out-of-date the very
         * instant it's done, regardless or whether or not it's done
         * atomically. Worst case, we're going to be over-estimating what
         * we can submit. The point is, we need to be able to deal with this
         * situation regardless of any perceived atomicity.
         */
        return ktail.toInt() - sq.pointed.khead!!.pointed.value.toInt()
    }

    companion object {
        enum class end {
           err  ,
           out
        }
    }
}

fun iopoll(args: Array<String>) {
    AppState().main(args)
}
