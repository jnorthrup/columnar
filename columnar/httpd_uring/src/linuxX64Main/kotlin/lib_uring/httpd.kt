package lib_uring

import platform.linux.*
import kotlinx.cinterop.*
import kotlinx.cinterop.nativeHeap.alloc
import platform.linux.free
import platform.posix.*
import simple.*
import simple.HasDescriptor.Companion.S_ISREG
import simple.HasPosixErr.Companion.posixRequires
import simple.simple.CZero.nz
import simple.simple.CZero.z
import uring_httpd.*
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KProperty1
import platform.posix.exit as posixExit
import platform.posix.fprintf as posixFprintf
import platform.posix.malloc as posixMalloc
import platform.posix.memset as posixMemset
import platform.posix.perror as posix_perror
import platform.posix.stderr as posixStderr

typealias `*_`<T> = CPointer<T>// started simply as 'k' but deemed too likely to conflict with nested loops.  :(

class ChunkEnding<A : CStructVar, B : CVariable, P : KProperty1<A, CPointer<B>>>(
    val present: NativePlacement,
    val prop: P,
)

inline val NativePlacement._k: NativePlacement get() = this
inline operator fun <A : CStructVar, B : CVariable, P : KProperty1<A, CPointer<B>>> NativePlacement.times(x: P) =
    ChunkEnding(this, x)

inline operator fun <reified A : CStructVar, reified B : CVariable, P : KProperty1<A, CPointer<B>>> ChunkEnding<A, B, P>.rem(
    count: Int,
): A = present.alloc(sizeOf<A>() + sizeOf<B>() * count, alignOf<A>()).reinterpret()

inline infix fun <reified T : CPointed> NativePlacement.`&`(v: T): CPointer<T> = v.ptr //pointer to

object `>`;inline infix operator fun <reified T : CPointed> CPointer<T>.minus(x: `>`): T = pointed
inline infix operator fun <reified T : NativePointed, reified P : CPointer<T>> NativePlacement.times(v: P) =
    interpretPointed<T>(v.rawValue)// "*" dereference aka pointed

/**
One function that prints the system call and the error details
and then exits with error code 1. Non-zero meaning things didn't go well.
 */
fun fatal_error(syscall: String): Unit {
    posix_perror(syscall)
    posixExit(1)
}

/**
 * Helper function for cleaner looking code.
 */

fun zh_malloc(size: size_t): `*_`<ByteVar> {
    val buf = posixMalloc(size).toLong()
    if (buf.z) {
        posixFprintf(posixStderr, "Fatal error: unable to allocate memory.\n")
        posixExit(1)
    }
    return buf.toCPointer<ByteVar>()!!.reinterpret()
}

/**
 * This function is responsible for setting up the main listening socket used by the
 * web server.
 */

fun setup_listening_socket(port: Int): Int = nativeHeap.run {

    val srv_addr = alloc<sockaddr_in>();

    val sock = socket(PF_INET, SOCK_STREAM, 0).also { _k `>` "server listening fd: $it port: $port" }
    posixRequires(sock != -1) { "socket()" }

    val enable: IntVar = alloc { value = 1 }
    val sockopt = setsockopt(sock,
        SOL_SOCKET,
        SO_REUSEADDR,
        _k `&` enable,
        Int.SIZE_BYTES.toUInt()).also {_k`>` "sockopt: $it" }
    posixRequires(sockopt >= 0) { "setsockopt(SO_REUSEADDR)" }

    bzero(_k `&` srv_addr,  sizeOf<sockaddr_in>().toULong())
    srv_addr.sin_family = AF_INET.toUShort()
    srv_addr.sin_port = htons(port.toUShort())
    srv_addr.sin_addr.s_addr = htonl(INADDR_ANY)

    /* We bind to a port and turn this socket into a listening
     * socket.
     * */
    val bind = bind(sock, (_k `&` srv_addr).reinterpret(), sizeOf<sockaddr_in>().toUInt()).also {_k`>` "bind returns : $it" }
    posixRequires(0 <= bind) { "bind()" }

    val listen = listen(sock, 10).also {_k`>` "listen returns: $it" }
    posixRequires(listen >= 0) { "listen()" }

    return sock.also {_k`>` "setup_listening_socket returning fd $it" }
}

fun add_accept_request(
    server_socket: Int,
    client_addr: `*_`<sockaddr_in>,
    client_addr_len: `*_`<socklen_tVar>,
): Int = nativeHeap.run {
    val sqe: `*_`<io_uring_sqe> = io_uring_get_sqe((_k `&` ring))!!
    io_uring_prep_accept(sqe, server_socket, client_addr.reinterpret(), client_addr_len, 0)
    val req: `*_`<request> = _k `&` allocWithFlex(request::iov, 1)
    (_k * req).event_type = EVENT_TYPE_ACCEPT
    io_uring_sqe_set_data(sqe, req)
    io_uring_submit((_k `&` ring))
    return 0.also { _k `>` "added accept request" }
}

fun add_read_request(client_socket: Int): Int = nativeHeap.run {
    val sqe: `*_`<io_uring_sqe> = io_uring_get_sqe(_k `&` ring)!!
    val req: `*_`<request> = _k `&` this * request::iov % 1 // malloc(sizeof(*req) + sizeof(c:iove))
    (_k * req).iov[0].iov_base = malloc(READ_SZ) as COpaquePointer
    (req - `>`).iov[0].iov_len = READ_SZ.toULong()
    (_k * req).event_type = EVENT_TYPE_READ
    (_k * req).client_socket = client_socket
    posixMemset((_k * req).iov, 0, READ_SZ)
    /* Linux kernel 5.5 has support for readv, but not for recv() or read() */
    io_uring_prep_readv(sqe, client_socket, (_k * req).iov[0].ptr, 1, 0)
    io_uring_sqe_set_data(sqe, req)
    io_uring_submit((_k `&` ring))
    return 0.also { _k `>` "added read request" }
}

fun add_write_request(req: `*_`<request>): Int = nativeHeap.run {_k `>` "add_write_request"
    val sqe: `*_`<io_uring_sqe> = io_uring_get_sqe((_k `&` ring))!!
    (_k * req).event_type = EVENT_TYPE_WRITE
    io_uring_prep_writev(sqe, (_k * req).client_socket, (_k * req).iov, (_k * req).iovec_count.toUInt(), 0)
    io_uring_sqe_set_data(sqe, req)
    io_uring_submit(_k `&` ring)
    return 0.also { _k `>` "added write request" }
}

fun sendStaticStringContent(str: String, client_socket: Int): Unit = kotlinx.cinterop.nativeHeap.run {
    val req: `*_`<request> = allocWithFlex(request::iov, 1).ptr// malloc(sizeof(*req) + sizeof(c:iove))
    val slen: ULong = strlen(str.also { ::`>` })
    (_k * req).iovec_count = 1
    (_k * req).client_socket = client_socket
    (_k * req).iov[0].iov_base = zh_malloc(slen)
    (_k * req).iov[0].iov_len = slen
    memcpy((_k * req).iov[0].iov_base, str.cstr, slen)
    add_write_request(req).also { _k `>` "added  write_request" }
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
    sendStaticStringContent(http_404_content!!.toKStringFromUtf8(), client_socket)
}

/*
 * Once a static file is identified to be served, this function is used to read the file
 * and write it over the client socket ~~using Linux's sendfile() system call. This saves us
 * the hassle of transferring file buffers from kernel to user space and back.~~
 */

fun copy_file_contents(file_path: `*_`<ByteVar>, file_size: off_t, iov: `*_`<iovec>): Unit = nativeHeap.run {
    val buf: `*_`<ByteVar> = malloc(file_size.toULong())!!.reinterpret()
    val fd = open(file_path.toKStringFromUtf8(), O_RDONLY)
    posixRequires(fd >= 0) { "open" }

    /* We should really check for short reads here */
    val i: ssize_t = read(fd, buf, file_size.toULong())
    if (i < file_size) {
        "Encountered a short read.\n".also(::`>`)
    }
    close(fd).also { _k `>` "closed file" }
    (_k * iov).iov_base = buf
    (_k * iov).iov_len = file_size.toULong()
}

/*
 * Simple function to get the file extension of the file that we are about to serve.
 * */

fun get_filename_ext(filename: String): String =
    filename.drop(min(filename.length, max(0, filename.lastIndexOf('.', max(0, filename.length - 5))) + 1))

val sufCount: Int by lazy {
    var c = 0
    while (suf[c].nz) c++
    c
}

infix fun NativePlacement.`>`(debug: String) {
    platform.posix.fprintf(_IO_stderr, ">>> ${debug.trim()}")
}


/*
 * Sends the HTTP 200 OK header, the server string, for a few types of files, it can also
 * send the content type based on the file extension. It also sends the content length
 * header. Finally it send a '\r\n' in a line by itself signalling the end of headers
 * and the beginning of any content.
 */

fun send_headers(path: String, len: off_t, iov: `*_`<iovec>): Unit = memScoped {

    val str = "HTTP/1.1 200 OK\r\n".also(::`>`)
    var slen = (str.length).toULong()

    iov[0].iov_base = strdup(str)

    iov[0].iov_len = slen

    slen = (SERVER_STRING.length.toULong())
    iov[1].iov_base = strdup(SERVER_STRING.also(::`>`))
    iov[1].iov_len = slen


    /*
     * Check the file extension for certain common types of files
     * on web pages and send the appropriate content-type header.
     * Since extensions can be mixed case like JPG, jpg or Jpg,
     * we turn the extension into lower case before checking.
     * */

    val ext: u_int32_tVar = alloc()
    val filename = path.lowercase()
    strncpy((_k `&` ext).reinterpret(), get_filename_ext(filename), 4UL)

    var x = 0
    for (p in 0 until sufCount) if (ext.value == suf[p]) {
        x = p
        break
    }

    val __s = ctype[x].toString().also(::`>`)
    iov[2].iov_base = strdup(__s)
    iov[2].iov_len = __s.length.toULong()

    /* Send the content-length header, which is the file size in this case. */
    var s = "content-length: $len\r\n".also(::`>`)
    iov[3].iov_base = strdup(s)
    iov[3].iov_len = s.length.toULong()

    /* Send the connection header. */
    s = "connection: keep-alive\r\n".also(::`>`)
    iov[4].iov_base = strdup(s)
    iov[4].iov_len = s.length.toULong()

    /*
     * When the browser sees a '\r\n' sequence in a line on its own,
     * it understands there are no more headers. Content may follow.
     */
    iov[5].iov_base = strdup("\r\n".also(::`>`))
    iov[5].iov_len = 2UL

}

fun handle_get_method(path: `*_`<ByteVar>, client_socket: Int): Unit = memScoped {
    _k `>` "handle_get_method"

    val final_path: ByteArray

    /*
     If a path ends in a trailing slash, the client probably wants the index
     file inside of that directory.
     */
    val path2 = path.toKStringFromUtf8().also(::`>`)
    final_path = if (path2.endsWith('/')) {
        "public/$path2/index.html".encodeToByteArray()
    } else {
        "public/$path2".encodeToByteArray()
    }

    /* The stat() system call will give you information about the file
     * like type (regular file, directory, etc), size, etc. */
    val path_stat: stat = nativeHeap.alloc()
    if (stat(final_path.toKString().also(::`>`), (_k `&` path_stat)) == -1) {
        printf("404 Not Found: %s (%s)\n", final_path.toKString(), path)
        handle_http_404(client_socket)
    } else {
        /* Check if this is a normal/regular file and not a directory or something else */
        if (S_ISREG(path_stat.st_mode)) {
            val req: `*_`<request> =
                nativeHeap.allocWithFlex(request::iov, 7).ptr  /* zh_malloc(sizeof(*req) + (sizeof(c:iove) * 7))*/
            (_k * req).iovec_count = 7
            (_k * req).client_socket = client_socket
            send_headers(final_path.toKString(), path_stat.st_size, (_k * req).iov.reinterpret())
            copy_file_contents(final_path.pin().addressOf(0), path_stat.st_size, (_k * req).iov[6].ptr.reinterpret())
            printf("200 %s %ld bytes\n", final_path.toKString(), path_stat.st_size)
            add_write_request(req)
        } else {
            handle_http_404(client_socket)
            printf("404 Not Found: %s\n", final_path.toKString().also(::`>`))
        }
    }
}

/*
 * This function looks at method used and calls the appropriate handler function.
 * Since we only implement GET and POST methods, it calls handle_unimplemented_method()
 * in case both these don't match. This sends an error to the client.
 * */

fun handle_http_method(method_buffer: `*_`<ByteVar>, client_socket: Int): Unit = memScoped{
 /*
    val method:`*k`<ByteVar>
    val path:`*k`<ByteVar>
    val saveptr:`*k`<ByteVar>

    method = strtok_r(method_buffer!!, " ", saveptr.reinterpret())!!
    strtolower(method)
    path = strtok_r(NULL, " ", (k`&` saveptr))
*/
    _k `>` "handle_http_method"

    val (Method, path) = method_buffer.toKStringFromUtf8().split("\\s+".toRegex(), 2)
    val method = Method.lowercase()

    if (strcmp(method, "get") == 0) {
        handle_get_method(path.replace("..", ".").pin().addressOf(0).reinterpret(), client_socket)
    } else {
        handle_unimplemented_method(client_socket)
    }
}

fun get_line(src: String, dest: `*_`<ByteVar>, dest_sz: Int): Int {
    for (i/*as int */ in 0 until dest_sz) {
        dest[i] = src[i].code.toByte()
        if (src[i] == '\r' && src[i + 1] == '\n') {
            dest[i] = 0.toByte()
            return 0
        }
    }
    return 1
}

fun handle_client_request(req: `*_`<request>): Int = memScoped {
    _k `>` "handle_client_request"
    val http_request = alloca(1024)
    /* Get the first line, which will be the request */
    posixRequires (!(get_line((_k * req).iov[0].iov_base.toString(), http_request!!.reinterpret(), 1024).nz)) { "Malformed request\n" }
    handle_http_method(http_request.reinterpret(), (_k * req).client_socket)
    return 0
}

fun server_loop(server_socket: Int) = nativeHeap.run {
     _k `>` "entering server_loop"
    val client_addr: sockaddr_in = alloc()
    val client_addr_len: socklen_tVar = alloc { value = sizeOf<sockaddr_in>().toUInt()  }
    add_accept_request(server_socket, (_k `&` client_addr), (_k `&` client_addr_len))
    val cqe = alloc<io_uring_cqe>().ptr

    while (true) {
        val ret: Int = io_uring_wait_cqe((_k `&` ring), cqe.reinterpret())
        posixRequires(ret >= 0) { "io_uring_wait_cqe" }

        val req: `*_`<request> = (_k * cqe).user_data.toLong().toCPointer()!!
        posixRequires((_k * cqe).res >= 0) { "Async request failed" }

        if ((_k * req).event_type == EVENT_TYPE_ACCEPT) {
            _k `>` "event_type == EVENT_TYPE_ACCEPT"

            add_accept_request(server_socket, (_k `&` client_addr), (_k `&` client_addr_len))

            _k `>` "handling Accept"
            add_read_request((_k * cqe).res)
            free(_k * req)
        }
        if ((_k * req).event_type == EVENT_TYPE_READ) {
            _k `>` "event_type == EVENT_TYPE_READ"
            if ((_k * cqe).res.nz) {
                close((_k * req).client_socket)
                posixFprintf(posixStderr, "Empty request!\n")
            } else {
                handle_client_request(req)
                free((_k * req).iov[0].iov_base)
                free(req)
            }
        }
        if ((_k * req).event_type == EVENT_TYPE_WRITE) {
            _k `>` "event_type == EVENT_TYPE_WRITE"
            add_read_request((_k * req).client_socket)
            for (i in 0 until (_k * req).iovec_count) {
                free((_k * req).iov[i].iov_base)
            }
            free(req)
        }
        _k `>` "Mark this request as processed"
        io_uring_cqe_seen(_k `&` ring, cqe)
    }
}

fun sigint_handler(signo: Int): Unit = nativeHeap.run {
    printf("^C pressed. Shutting down.\n")
    io_uring_queue_exit(_k `&` ring)
    posixExit(0)
}

fun httpd() = memScoped {
    val server_socket: Int = setup_listening_socket(DEFAULT_SERVER_PORT)
    _k `>` "server socket fd:  $server_socket"
    signal(SIGINT, staticCFunction<Int, Unit>(::sigint_handler))
    memScoped {
        val byteArray = ByteArray(2048)
        val cwd = getcwd(byteArray.toCValues(), byteArray.size.toULong())
        println("starting httpd in ${cwd!!.toKStringFromUtf8()}")
    }

    val ioUringQueueInit = io_uring_queue_init(QUEUE_DEPTH, _k `&` ring, 0)
    server_loop(server_socket)
}