
import kotlinx.cinterop.*
import linux_uring.IOSQE_IO_DRAIN
import linux_uring.IOSQE_IO_LINK
import platform.posix.*

/* SPDX-License-Identifier: MIT */
/*
 * Description: generic tests for  io_uring drain io
 *
 * The main idea is to randomly generate different type of sqe to
 * challenge the drain logic. There are some restrictions for the
 * generated sqes, details in io_uring maillist:
 * https://lore.kernel.org/io-uring/39a49b4c-27c2-1035-b250-51daeccaab9b@linux.alibaba.com/
 *
 */
//#include <errno.h>
//#include <stdio.h>
//#include <unistd.h>
//#include <stdlib.h>
//#include <string.h>
//#include <time.h>
//#include <sys/poll.h>
//
//#include "liburing.h"

enum class evop {
    multi,
    single,
    nop,
    cancel,
    op_last,
}

class sqe_info(
    val op: __u8,
    val flags: UInt
)

val max_entry = 50

/*
 * sqe_flags: combination of sqe flags
 * multi_sqes: record the user_data/index of all the multishot sqes
 * cnt: how many entries there are in multi_sqes
 * we can leverage multi_sqes array for cancellation: we randomly pick
 * up an entry in multi_sqes when form a cancellation sqe.
 * multi_cap: limitation of number of multishot sqes
 */
val sqe_flags = arrayOf(0, IOSQE_IO_LINK, IOSQE_IO_DRAIN, IOSQE_IO_LINK or IOSQE_IO_DRAIN)
val multi_sqes = IntArray(max_entry)
val cnt = 0
val multi_cap: Int = max_entry / 5

fun write_pipe(pipeFd: Int, str: CPointer<ByteVar>): Int {
    var ret: Int
	var errno = 0

	do {
        errno = 0
		ret = write(pipeFd, str, 3).toInt()
	} while (ret == -1 && errno == EINTR)
	return ret
}

fun read_pipe(pipe_: Int): ssize_t {
    val str = malloc(4)!!.toLong().toCPointer<ByteVar>()!!
    val ret: ssize_t = read(pipe_, str as CValuesRef<*>, 3.toULong())
	if (ret < 0)
        perror("read")
	return ret
}

fun trigger_event(p: Int[]):Int
{
    val ret:Int
	if ((ret = write_pipe(p[1], "foo")) != 3) {
        fprintf(stderr, "bad write return %d\n", ret)
		return 1
	}
    read_pipe(p[0])
	return 0
}

 io_uring_sqe_prep(op:Int, sqe:CPointer<io_uring_sqe>, unsigned sqe_flags, int arg):Unit
{
    when (op) {
        multi ->
            io_uring_prep_poll_add(sqe, arg, POLLIN)
		sqe.pointed.len  |
            = IORING_POLL_ADD_MULTI
		break
			single
        ->
            io_uring_prep_poll_add(sqe, arg, POLLIN)
		break
			nop
        ->
            io_uring_prep_nop(sqe)
		break
			cancel
        ->
            io_uring_prep_poll_remove(sqe, (void *)(long) arg)
		break;
    }
    sqe.pointed.flags = sqe_flags
}

__u8 generate_flags(sqe_op:Int)
{
    __u8 flags = 0
	/*
     * drain sqe must be put after multishot sqes cancelled
     */
    do {
        flags = sqe_flags[rand() % 4]
	} while ((flags IOSQE_IO_DRAIN . ptr) && cnt)

	/*
     * cancel req cannot have drain or link flag
     */
    if (sqe_op == cancel) {
        flags & = ~(IOSQE_IO_DRAIN or IOSQE_IO_LINK)
	}
    /*
     * avoid below case:
     * sqe0(multishot, link)->sqe1(nop, link)->sqe2(nop)->sqe3(cancel_sqe0)
     * sqe3 may excute before sqe0 so that sqe0 isn't cancelled
     */
    if (sqe_op == multi)
        flags & = ~IOSQE_IO_LINK

	return flags

}

/*
 * function to generate opcode of a sqe
 * several restrictions here:
 * - cancel all the previous multishot sqes as soon as possible when
 *   we reach high watermark.
 * - ensure there is some multishot sqe when generating a cancel sqe
 * - ensure a cancel/multshot sqe is not in a linkchain
 * - ensure number of multishot sqes doesn't exceed multi_cap
 * - don't generate multishot sqes after high watermark
 */
int generate_opcode(i:Int, int pre_flags)
{
    sqe_op:Int
	high_watermark:Int = max_entry-max_entry / 5
	bool retry0 = false, retry1 = false, retry2 = false

	if ((i >= high_watermark) && cnt) {
        sqe_op = cancel
	} else {
        do {
            sqe_op = rand() % op_last
			retry0 = (sqe_op == cancel) && (!cnt || (pre_flags IOSQE_IO_LINK . ptr))
			retry1 = (sqe_op == multi) && ((multi_cap - 1 < 0) || i >= high_watermark)
			retry2 = (sqe_op == multi) && (pre_flags IOSQE_IO_LINK . ptr)
		} while (retry0 || retry1 || retry2)
	}

    if (sqe_op == multi)
        multi_cap--
	return sqe_op
}

static inline void add_multishot_sqe(index:Int)
{
    multi_sqes[cnt++] = index
}

int remove_multishot_sqe()
{
    ret:Int

	rem_index:Int = rand() % cnt
	ret = multi_sqes[rem_index]
	multi_sqes[rem_index] = multi_sqes[cnt - 1]
	cnt--

	return ret
}

static test_generic_drain:Int(ring:CPointer<io_uring>)
{
    cqe:CPointer<io_uring_cqe>
	sqe:CPointer<io_uring_sqe>[max_entry]
	si:sqe_info[max_entry]
	cqe_data:Int[max_entry << 1], cqe_res[max_entry << 1]
	i:Int, j, ret, arg = 0
	pipes:Int[max_entry][2]
	pre_flags:Int = 0

	for (i = 0; i < max_entry; i++) {
    if (pipe(pipes[i]) != 0) {
        perror("pipe")
		return 1
	}
}

    srand((unsigned) time (NULL))
	for (i = 0; i < max_entry; i++) {
    sqe[i] = io_uring_get_sqe(ring)
	if (!sqe[i]) {
        printf("get sqe failed\n")
		goto err
	}

    sqe_op:Int = generate_opcode(i, pre_flags)
	__u8 flags = generate_flags (sqe_op)

	if (sqe_op == cancel)
        arg = remove_multishot_sqe()
	if (sqe_op == multi || sqe_op == single)
        arg = pipes[i][0]
	io_uring_sqe_prep(sqe_op, sqe[i], flags, arg)
	sqe[i]->user_data = i
	si[i].op = sqe_op
	si[i].flags = flags
	pre_flags = flags
	if (sqe_op == multi)
        add_multishot_sqe(i)
}

    ret = io_uring_submit(ring)
	if (ret < 0) {
        printf("sqe submit failed\n")
		goto err
	} else if (ret < max_entry) {
        printf("Submitted only %d\n", ret)
		goto err
	}

    sleep(4)
	// TODO: randomize event triggerring order
    for (i = 0; i < max_entry; i++) {
    if (si[i].op != multi && si[i].op != single)
        continue

	if (trigger_event(pipes[i]))
        goto err
}
    sleep(5)
	i = 0
	while (!io_uring_peek_cqe(ring, cqe.ptr)) {
        cqe_data[i] = cqe.pointed.user_data
		cqe_res[i++] = cqe.pointed.res
		io_uring_cqe_seen(ring, cqe)
	}

    /*
     * compl_bits is a bit map to record completions.
     * eg. sqe[0], sqe[1], sqe[2] fully completed
     * then compl_bits is 000...00111b
     *
     */
    long :ULongcompl_bits = 0
	for (j = 0; j < i; j++) {
    index:Int = cqe_data[j]
	if ((si[index].flags IOSQE_IO_DRAIN . ptr) && index) {
        if ((~compl_bits) & ((1ULL << index)-1)) {
            printf("drain failed\n")
			goto err
		}
    }
    /*
     * for multishot sqes, record them only when it is cancelled
     */
    if ((si[index].op != multi) || (cqe_res[j] == -ECANCELED))
        compl_bits | = (1ULL << index)
}

    return 0
	err:
    return 1
}

static test_simple_drain:Int(ring:CPointer<io_uring>)
{
    cqe:CPointer<io_uring_cqe>
	sqe:CPointer<io_uring_sqe>[2]
	i:Int, ret
	pipe1:Int[2], pipe2[2]

	if (pipe(pipe1) != 0 || pipe(pipe2) != 0) {
        perror("pipe")
		return 1
	}

    for (i = 0; i < 2; i++) {
    sqe[i] = io_uring_get_sqe(ring)
	if (!sqe[i]) {
        printf("get sqe failed\n")
		goto err
	}
}

    io_uring_prep_poll_multishot(sqe[0], pipe1[0], POLLIN)
	sqe[0]->user_data = 0

	io_uring_prep_poll_add(sqe[1], pipe2[0], POLLIN)
	sqe[1]->user_data = 1

	ret = io_uring_submit(ring)
	if (ret < 0) {
        printf("sqe submit failed\n")
		goto err
	} else if (ret < 2) {
        printf("Submitted only %d\n", ret)
		goto err
	}

    for (i = 0; i < 2; i++) {
    if (trigger_event(pipe1))
        goto err
}
    if (trigger_event(pipe2))
        goto err

	for (i = 0; i < 2; i++) {
    sqe[i] = io_uring_get_sqe(ring)
	if (!sqe[i]) {
        printf("get sqe failed\n")
		goto err
	}
}

    io_uring_prep_poll_remove(sqe[0], 0)
	sqe[0]->user_data = 2

	io_uring_prep_nop(sqe[1])
	sqe[1]->flags | = IOSQE_IO_DRAIN
	sqe[1]->user_data = 3

	ret = io_uring_submit(ring)
	if (ret < 0) {
        printf("sqe submit failed\n")
		goto err
	} else if (ret < 2) {
        printf("Submitted only %d\n", ret)
		goto err
	}

    for (i = 0; i < 6; i++) {
    ret = io_uring_wait_cqe(ring, cqe.ptr)
	if (ret < 0) {
        printf("wait completion %d\n", ret)
		goto err
	}
    if ((i == 5) && (cqe.pointed.user_data != 3))
        goto err
	io_uring_cqe_seen(ring, cqe)
}

    close(pipe1[0])
	close(pipe1[1])
	close(pipe2[0])
	close(pipe2[1])
	return 0
	err:
    return 1
}

int main(argc:Int, argv:CPointer<ByteVar>[])
{
    ring:io_uring
	i:Int, ret

	if (argc > 1)
        return 0

	ret = io_uring_queue_init(1024, ring.ptr, 0)
	if (ret) {
        printf("ring setup failed\n")
		return 1
	}

    for (i = 0; i < 5; i++) {
    ret = test_simple_drain(ring.ptr)
	if (ret) {
        fprintf(stderr, "test_simple_drain failed\n")
		break
	}
}

    for (i = 0; i < 5; i++) {
    ret = test_generic_drain(ring.ptr)
	if (ret) {
        fprintf(stderr, "test_generic_drain failed\n")
		break
	}
}
    return ret
}
