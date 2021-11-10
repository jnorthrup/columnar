package simple


import kotlinx.cinterop.*
import platform.linux.*
import platform.posix.*


fun open_read() {

    val fd = open("/etc/sysctl.conf", O_RDONLY)
    memScoped {
        val buf: CArrayPointer<ByteVar> = this.allocArray<ByteVar>(41)

        var count: ssize_t
        var disp: Int = 0
        do {
            count = read(fd, buf, 40UL)
            if (disp == 0) printf("%s\n", buf)
            disp++
        } while (count > 0)
    }
    close(fd)
}


fun epoll_popen_loop_cat(): Int = memScoped {

    val pIoFile: CPointer<FILE> =
        popen("cat /etc/sysctl.conf </dev/null", "r")!!   //works when stdin is piped, not dev/null tho

    val event: epoll_event = alloc()
    event.data.fd = pIoFile.pointed._fileno
    event.events = (EPOLLIN or EPOLLPRI or EPOLLERR).toUInt()

    val epoll_fd:Int = epoll_create1(0)
    val epollCtl =
        epoll_ctl(epoll_fd, EPOLL_CTL_ADD as Int, event.data.fd as Int, event.ptr as CValuesRef<epoll_event>)
    if (0!=epollCtl) {
        fprintf(stderr, "Failed to add file descriptor to epoll_popen_loop_cat\n")
        close(epoll_fd)
        return 1
    }

    val  READ_SIZE : Int = 10
    var running:Int = 1
    while (0!=running) {
        printf("\nPolling for input...\n")
    val   MAX_EVENTS : Int = 5
        val events:CArrayPointer<epoll_event> = allocArray(5)
        val event_count:Int = epoll_wait(epoll_fd, events, MAX_EVENTS, 3000)

        printf("%d ready events\n", event_count)
        for (i in   0 until event_count) {
            val fd:Int = events[i].data.fd
            printf("Reading file descriptor '%d' -- ", fd)

            val  buf:CArrayPointer<ByteVar> = allocArray( READ_SIZE + 1)
            val bytes_read:ssize_t = read(fd, buf, READ_SIZE.toULong())
            printf("%zd bytes read.\n", bytes_read)
            if (bytes_read < 1) {
                running = 0
                break
            }
            buf[bytes_read.toInt()] = 0.toByte()
            printf("Read '%s'\n", buf)

        }
    }

    if (0!=close(epoll_fd)) {
        fprintf(stderr, "Failed to close epoll_popen_loop_cat file descriptor\n")
        return 1
    }
    return 0
}

fun open_mmap(): Unit {
    memScoped {

        val cFile = CFile("/etc/sysctl.conf", O_RDONLY)
        val cPointer = cFile.mmap(41.toULong(), offset = 0)
        cFile.close()
        val ccptr = cPointer.toLong().toCPointer<ByteVar>()
        println(ccptr!!.pin().get().toKStringFromUtf8().take(40))

        munmap(ccptr, 41)
    }
}

fun main() {
    try {
        open_read()
        cFileRead()
        open_mmap()
        epoll_popen_loop_cat()
    } catch (e: kotlin.IllegalArgumentException) {
        e.printStackTrace()
    }
}

fun cFileRead() {
    val buf = ByteArray(41)
    val cFile = CFile("/etc/sysctl.conf", O_RDONLY)
    val read = cFile.read(buf)
    println(buf.decodeToString(0, read.toInt()))
    cFile.close()
}
