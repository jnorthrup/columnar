package lib_uring

import kotlinx.cinterop.*
import linux_uring.tolower
import platform.posix.*
import platform.posix.socklen_tVar
import simple.HasDescriptor.Companion.S_ISREG
import simple.HasPosixErr
import simple.allocWithFlex
import simple.simple.CZero.nz
import simple.simple.CZero.z
import uring_httpd.*
import kotlin.math.max
import kotlin.math.min

/*
 * Utility function to convert a string to lower case.
 * */

fun strtolower(str: CPointer<ByteVar>): Unit { //    val s:CPointerVar<ByteVarOf<Byte>> =str.reinterpret()
    var c = 0
    while (str[c].nz) {
        str[c] = tolower(str[c].toInt()).toByte()
        c++
    }
}

/*
 One function that prints the system call and the error details
 and then exits with error code 1. Non-zero meaning things didn't go well.
 */
fun fatal_error(syscall: String): Unit {
    perror(syscall)
    exit(1)
}

/*
 * Helper function for cleaner looking code.
 * */

fun zh_malloc(size: size_t): CPointer<ByteVar> {
    val buf: CPointer<ByteVar> = malloc(size)!!.reinterpret()
    return buf
}

/*
 * This function is responsible for setting up the main listening socket used by the
 * web server.
 * */

fun setup_listening_socket(port: Int): Int = nativeHeap.run {
    val srv_addr: sockaddr_in = alloc()

    val sock = socket(PF_INET, SOCK_STREAM, 0)
    m d "socket fd $sock"
    if (sock == -1) fatal_error("socket()")

    val enable: IntVar = alloc()

    if (setsockopt(sock,
            SOL_SOCKET,
            SO_REUSEADDR,
            enable.ptr,
            Int.SIZE_BYTES.toUInt() /* = kotlin.UInt */) < 0
    ) fatal_error("setsockopt(SO_REUSEADDR)")


    memset(srv_addr.ptr, 0, sizeOf<sockaddr_in>().toULong() /* = kotlin.ULong */)
    srv_addr.sin_family = AF_INET.toUShort()
    srv_addr.sin_port = htons(port.toUShort())
    srv_addr.sin_addr.s_addr = htonl(INADDR_ANY)

    /* We bind to a port and turn this socket into a listening
     * socket.
     * */
    if (bind(sock,
            srv_addr.ptr.reinterpret(),
            sizeOf<sockaddr_in>().toUInt() /* = kotlin.UInt */).also { m d "bind returns $it" } < 0
    ) fatal_error("bind()")

    if (listen(sock, 10).also { m d "listen returns $it" } < 0) fatal_error("listen()")

    return (sock)
}

fun add_accept_request(
    server_socket: Int,
    client_addr: CPointer<sockaddr_in>,
    client_addr_len: CPointer<socklen_tVar>,
): Int = nativeHeap.run {
    m d "add_accept_request"
    val sqe: CPointer<io_uring_sqe> = io_uring_get_sqe(ring.ptr)!!
    io_uring_prep_accept(sqe, server_socket, client_addr.reinterpret(), client_addr_len as CValuesRef<socklen_tVar>, 0)
    val req: CPointer<request> = allocWithFlex(request::iov, 7).ptr
    req.pointed.event_type = EVENT_TYPE_ACCEPT
    io_uring_sqe_set_data(sqe, req)
    io_uring_submit(ring.ptr)

    return 0
}

fun add_read_request(client_socket: Int): Int = nativeHeap.run {
    m d "add_read_request"
    val sqe: CPointer<io_uring_sqe> = io_uring_get_sqe(ring.ptr)!!
    val req: CPointer<request> = allocWithFlex(request::iov, 7).ptr
    req.pointed.iov[0].iov_base = malloc(READ_SZ)
    req.pointed.iov[0].iov_len = READ_SZ.toULong()
    req.pointed.event_type = EVENT_TYPE_READ
    req.pointed.client_socket = client_socket
    memset(req.pointed.iov[0].iov_base, 0, READ_SZ)
    /* Linux kernel 5.5 has support for readv, but not for recv() or read() */
    io_uring_prep_readv(sqe, client_socket, req.pointed.iov[0].ptr, 1, 0)
    io_uring_sqe_set_data(sqe, req)
    io_uring_submit(ring.ptr)
    return 0
}

fun add_write_request(req: CPointer<request>): Int {
    val sqe: CPointer<io_uring_sqe> = io_uring_get_sqe(ring.ptr)!!
    req.pointed.event_type = EVENT_TYPE_WRITE
    io_uring_prep_writev(sqe, req.pointed.client_socket, req.pointed.iov, req.pointed.iovec_count.toUInt(), 0)
    io_uring_sqe_set_data(sqe, req)
    io_uring_submit(ring.ptr)
    return 0
}

fun sendStaticStringContent(str: String, client_socket: Int): Unit = nativeHeap.run {
    val req: CPointer<request> = allocWithFlex(request::iov, 7).ptr
    var slen: ULong = strlen(str)
    req.pointed.iovec_count = 1
    req.pointed.client_socket = client_socket
    req.pointed.iov[0].iov_base = zh_malloc(slen)
    req.pointed.iov[0].iov_len = slen
    memcpy(req.pointed.iov[0].iov_base, str.cstr, slen)
    add_write_request(req)
}

/*
 * When ZeroHTTPd encounters any other HTTP method other than GET or POST, this function
 * is used to inform the client.
 * */

fun handle_unimplemented_method(client_socket: Int): Unit {

    sendStaticStringContent(unimplemented_content!!.toKStringFromUtf8(), client_socket)

}

/*
 * This function is used to send a "HTTP Not Found" code and message to the client in
 * case the file requested is not found.
 * */

fun handle_http_404(client_socket: Int): Unit {
    nativeHeap d "handle_http_404"
    sendStaticStringContent(http_404_content!!.toKStringFromUtf8(), client_socket)
}

/*
 * Once a static file is identified to be served, this function is used to read the file
 * and write it over the client socket using Linux's sendfile() system call. This saves us
 * the hassle of transferring file buffers from kernel to user space and back.
 * */

fun copy_file_contents(file_path: CPointer<ByteVar>, file_size: off_t, iov: CPointer<iovec>): Unit = nativeHeap.run {
    m d "copy_file_contents ${file_path.toKStringFromUtf8()}"
    val buf: CPointer<ByteVar> = malloc(file_size.toULong())!!.reinterpret()
    val fd = open(file_path.toKStringFromUtf8(), O_RDONLY)
    HasPosixErr.posixRequires(fd >= 0) { "open" }

    /* We should really check for short reads here */
    val i: ssize_t = read(fd, buf, file_size.toULong())
    HasPosixErr.posixRequires(!(i < file_size)) { "Encountered a short read.\n" }
    close(fd)

    iov.pointed.iov_base = buf
    iov.pointed.iov_len = file_size.toULong() /* = kotlin.ULong */
}

/*
 * Simple function to get the file extension of the file that we are about to serve.
 */

fun get_filename_ext(filename: String): String = nativeHeap.run {
    m d "get_filename_ext $filename"
    filename.drop(min(filename.length, max(0, filename.lastIndexOf('.', max(0, filename.length - 5))) + 1))
        .also { m d it }
}

val the_end: Int by lazy {
    var c = 0
    while (suf[c].nz) c++
    c.also { nativeHeap d "suffixes counted: $c" }
}
/*
 * Sends the HTTP 200 OK header, the server string, for a few types of files, it can also
 * send the content type based on the file extension. It also sends the content length
 * header. Finally it send a '\r\n' in a line by itself signalling the end of headers
 * and the beginning of any content.
 * */

fun send_headers(path: String, len: off_t, iov: CPointer<iovec>): Unit = nativeHeap.run {
    m d ("send_headers   + ${path}")
    var send_buffer: CPointer<ByteVar> = malloc(1024)!!.reinterpret()
    val __dest: CPointer<ByteVar>
    val small_case_path: CPointer<ByteVar> = malloc(1024)!!.reinterpret()
    __dest = small_case_path
    strcpy(__dest, path)
    strtolower(__dest)

    val str = "HTTP/1.1 200 OK\r\n"
    var slen: ULong = str.length.toULong()
    iov[0].iov_base = zh_malloc(slen).reinterpret()
    iov[0].iov_len = slen
    memcpy(iov[0].iov_base, str.cstr, slen)
    m d "iov[0] http1.1"

    slen = strlen(SERVER_STRING)
    iov[1].iov_base = zh_malloc(slen)
    iov[1].iov_len = slen
    memcpy(iov[1].iov_base, SERVER_STRING.cstr, slen)
    m d "iov[1] serverstring"
    /*
     * Check the file extension for certain common types of files
     * on web pages and send the appropriate content-type header.
     * Since extensions can be mixed case like JPG, jpg or Jpg,
     * we turn the extension into lower case before checking.
     * */

    val ext: u_int32_tVar = alloc { this.value = 0U /* = kotlin.UInt */ }
    val fourBytes = ext.ptr.reinterpret<ByteVar>()
    strncpy(fourBytes as CValuesRef<ByteVar  >,
        get_filename_ext(small_case_path.toKStringFromUtf8()),
        sizeOf<uint32_tVar>().toULong())
        m d "32bits ${fourBytes.toKStringFromUtf8()}"


    var c = 0
    do {
        if (c == the_end || ext.value.toUInt() == suf[c].toUInt()) {
            m d "choosing suf $c - ${ext.value} ?= ${suf[c]} "
            break
        }
        c++
    } while (true)

    slen = strlen(ctype[c]!!.toKStringFromUtf8())
    strncpy(send_buffer, ctype[c]!!.toKStringFromUtf8(), slen)

    iov[2].iov_base = zh_malloc(slen)
    iov[2].iov_len = slen
    memcpy(iov[2].iov_base, send_buffer, slen)

    /* Send the content-length header, which is the file size in this case. */
    sprintf(send_buffer, "content-length: %ld\r\n", len)
    slen = strlen(send_buffer.toKStringFromUtf8())
    iov[3].iov_base = zh_malloc(slen)
    iov[3].iov_len = slen
    memcpy(iov[3].iov_base, send_buffer, slen)

    /* Send the connection header. */
    sprintf(send_buffer, "connection: %s\r\n", "keep-alive")
    slen = strlen(send_buffer.toKStringFromUtf8())
    iov[4].iov_base = zh_malloc(slen)
    iov[4].iov_len = slen
    memcpy(iov[4].iov_base, send_buffer, slen)

    /*
     * When the browser sees a '\r\n' sequence in a line on its own,
     * it understands there are no more headers. Content may follow.
     * */
    strcpy(send_buffer, "\r\n")
    slen = strlen(send_buffer.toKStringFromUtf8())
    iov[5].iov_base = zh_malloc(slen)
    iov[5].iov_len = slen
    memcpy(iov[5].iov_base, send_buffer, slen)
}

fun handle_get_method(path: CPointer<ByteVar>, client_socket: Int): Unit = nativeHeap.run {
    m d "handle_get_method ${path.toKStringFromUtf8()}"
    val final_path: CPointer<ByteVar> = malloc(1024)!!.reinterpret()

    /*
     If a path ends in a trailing slash, the client probably wants the index
     file inside of that directory.
     */
    val toInt = (strlen(path.toKStringFromUtf8()) - 1u).toInt()
    if (path[toInt] == '/'.code.toByte()) {
        strcpy(final_path, "public")
        strcat(final_path, path.toKStringFromUtf8())
        strcat(final_path, "index.html")
    } else {
        strcpy(final_path, "public")
        strcat(final_path, path.toKStringFromUtf8())
    }

    /* The stat() system call will give you information about the file
     * like type (regular file, directory, etc), size, etc. */
    val path_stat: stat = alloc()
    if (stat(final_path.toKStringFromUtf8(), path_stat.ptr) == -1) {
        printf("404 Not Found: %s (%s)\n", final_path, path)
        handle_http_404(client_socket)
    } else {
        /* Check if this is a normal/regular file and not a directory or something else */
        if (S_ISREG(path_stat.st_mode)) {
            val req: CPointer<request> = allocWithFlex(request::iov, 7).ptr.reinterpret<request>()
            req.pointed.iovec_count = 7
            req.pointed.client_socket = client_socket
            send_headers(final_path.toKStringFromUtf8(), path_stat.st_size, req.pointed.iov.reinterpret())
            copy_file_contents(final_path, path_stat.st_size, req.pointed.iov[6].ptr.reinterpret())
            printf("200 %s %ld bytes\n", final_path, path_stat.st_size)
            add_write_request(req)
        } else {
            handle_http_404(client_socket)
            printf("404 Not Found: %s\n", final_path)
        }
    }
}

/*
 * This function looks at method used and calls the appropriate handler function.
 * Since we only implement GET and POST methods, it calls handle_unimplemented_method()
 * in case both these don't match. This sends an error to the client.
 * */

fun handle_http_method(method_buffer: CPointer<ByteVar>, client_socket: Int): Unit = nativeHeap.run {
    m d "handle_http_method"
    val method: CPointerVar<ByteVar> = alloc()
    val path: CPointerVar<ByteVar> = alloc()
    val saveptr: CPointerVar<ByteVar> = alloc()

    method.value = strtok_r(method_buffer, " ", saveptr.ptr)
    method.value?.run {
        m d "method ${method.value}"
        strtolower(method.value!!.reinterpret())
        m d "lower method ${method.value}"
        path.value = strtok_r(null, " ", saveptr.ptr)
        if (strcmp(method.value!!.toKStringFromUtf8(), "get") == 0) {
            handle_get_method(path.value!!.reinterpret(), client_socket)

        }
    } ?: handle_unimplemented_method(client_socket)

}

fun get_line(src: CPointer<ByteVar>, dest: CPointer<ByteVar>, dest_sz: Int): Int = memScoped {
    m d "get_line src=${src.toKStringFromUtf8()} dest=$dest len:$dest_sz"
    for (i in 0 until dest_sz) {
        dest[i] = src[i]
        if (src[i] == '\r'.code.toByte() && ((src[i + 1])) == '\n'.code.toByte()) {
            dest[i] = 0.toByte()
            return 0.also { m d "getline=($dest)${dest.toKStringFromUtf8()}" }
        }
    }
    return 1
}

fun handle_client_request(req: CPointer<request>): Int = memScoped {
    m d "handle_client_request"

    /* Get the first line, which will be the request */
    val http_request = alloc(1024, alignOf<CPointerVar<ByteVar>>()).reinterpret<ByteVar>().ptr
    HasPosixErr.warning(get_line(req.pointed.iov[0].iov_base!!.reinterpret(),
        http_request,
        1024).z) { "Malformed request\n" }
    handle_http_method(http_request, req.pointed.client_socket)
    return 0
}

fun server_loop(server_socket: Int): Unit = nativeHeap.run {
    val cqe: CPointerVar<io_uring_cqe> = alloc()
    val client_addr: sockaddr_in = nativeHeap.alloc()
    val client_addr_len: socklen_tVar = nativeHeap.alloc { value = sizeOf<sockaddr_in>().toUInt() }
    add_accept_request(server_socket, client_addr.ptr, client_addr_len.ptr)

    var c = 0
    while (true) {
        m d "loop iter ${c++}"
        val ret: Int = io_uring_wait_cqe(ring.ptr, cqe.ptr as CValuesRef<CPointerVar<io_uring_cqe>>)
        m d "$ret cqe requests fulfulled"
        HasPosixErr.warning(ret >= 0) { "io_uring_wait_cqe" }

        val ioUringCqe: io_uring_cqe = cqe.pointed!!
        val req: CPointer<request> = ioUringCqe.user_data.toLong().toCPointer()!!
        HasPosixErr.warning(ioUringCqe.res >= 0) { "Async request failed: ${strerror(-ioUringCqe.res)} for event: ${req.pointed.event_type}" }

        /*
        m d "handling req.pointed.event_type=${req.pointed.event_type} among ${
            listOf(
                "EVENT_TYPE_ACCEPT" to EVENT_TYPE_ACCEPT,
                "EVENT_TYPE_READ" to EVENT_TYPE_READ,
                "EVENT_TYPE_WRITE" to EVENT_TYPE_WRITE,
            ).let { list ->
                list.map { (a, b) -> "$a & $b = ${req.pointed.event_type and b}" }
            }
        }"
        */
        when (req.pointed.event_type) {
            EVENT_TYPE_ACCEPT -> {
                m d "EVENT_TYPE_ACCEPT ->"
                add_accept_request(server_socket, client_addr.ptr, client_addr_len.ptr)
                add_read_request(ioUringCqe.res)
                free(req.also { m d "freeing $it" })
            }
            EVENT_TYPE_READ -> {
                m d "EVENT_TYPE_READ->  res:${ioUringCqe.res}"
                if (ioUringCqe.res.z) {
                    close(req.pointed.client_socket)
                    fprintf(stderr, "Empty request!\n")
                }
                handle_client_request(req)
                free(req.pointed.iov[0].iov_base.also { m d "freeing $it" })
                free(req.also { m d "freeing $it" })
            }
            EVENT_TYPE_WRITE -> {
                m d "EVENT_TYPE_WRITE ->"
                add_read_request(req.pointed.client_socket)
                for (i/*as int */ in 0 until req.pointed.iovec_count) {
                    free(req.pointed.iov[i].iov_base.also { m d "freeing $it" })
                }
                free(req.also { m d "freeing $it" })
            }
        }
        /* Mark this request as processed */
        io_uring_cqe_seen(ring.ptr, ioUringCqe.ptr)
    }

}

fun sigint_handler(signo: Int): Unit {
    printf("^C pressed. Shutting down.\n")
    io_uring_queue_exit(ring.ptr)
    exit(0)
}

fun httpd() = memScoped {

    val server_socket: Int = setup_listening_socket(DEFAULT_SERVER_PORT)
    signal(SIGINT, staticCFunction(::sigint_handler))

    memScoped {
        val buf = alloca(2000)!!
        val s = getcwd(__buf = buf.reinterpret(), 2000)!!.toKStringFromUtf8()
        m d "launching in $s"
    }
    io_uring_queue_init(QUEUE_DEPTH, ring.ptr, 0)
    m d "entering main loop"
    server_loop(server_socket)

}

val NativePlacement.m get() = this
infix fun NativePlacement.d(d: Any?) = memScoped { fprintf(stderr, ">>> $d".trim()) }