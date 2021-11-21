package simple


import kotlinx.cinterop.*
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



fun open_mmap(): Unit {
    memScoped {

        val posixFile = PosixFile("/etc/sysctl.conf", O_RDONLY)
        val cPointer = posixFile.mmap(41.toULong(), offset = 0)
        posixFile.close()
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
    } catch (e: kotlin.IllegalArgumentException) {
        e.printStackTrace()
    }
}

fun cFileRead() {
    val buf = ByteArray(41)
    val posixFile = PosixFile("/etc/sysctl.conf", O_RDONLY)
    val read = posixFile.read(buf)
    println(buf.decodeToString(0, read.toInt()))
    posixFile.close()
}
