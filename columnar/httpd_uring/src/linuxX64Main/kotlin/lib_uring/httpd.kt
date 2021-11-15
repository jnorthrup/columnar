package lib_uring

import platform.linux.*
import kotlinx.cinterop.*
import kotlinx.cinterop.nativeHeap.alloc
import platform.linux.free
import platform.posix.*
import simple.HasDescriptor.Companion.S_ISREG
import simple.allocWithFlex
import simple.simple.CZero.nz
import simple.simple.CZero.z
import uring_httpd.*
import kotlin.math.max
import kotlin.math.min
import platform.posix.exit as posixExit
import platform.posix.fprintf as posixFprintf
import platform.posix.malloc as posixMalloc
import platform.posix.memset as posixMemset
import platform.posix.perror as posix_perror
import platform.posix.stderr as posixStderr

/**
 * Utility function to convert a string to lower case.
 */
fun strtolower(str1: CPointer<ByteVar>): Unit {
    var c = 0
    while (str1[c].nz) str1[c] = tolower(str1[c++].toInt()).toByte()
}

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

fun zh_malloc(size: size_t): CPointer<ByteVar> {
    val ptr = posixMalloc(size).toLong()
    if (ptr.z) {
        posixFprintf(posixStderr, "Fatal error: unable to allocate memory.\n")
        posixExit(1)
    }
    return ptr.toCPointer<ByteVar>()!!.reinterpret()
}

/**
 * This function is responsible for setting up the main listening socket used by the
 * web server.
 */

fun setup_listening_socket(port: Int): Int = nativeHeap.run {

    val srv_addr: sockaddr_in = alloc<sockaddr_in>()

    val sock = socket(PF_INET, SOCK_STREAM, 0)
    if (sock == -1)
        fatal_error("socket()")

    val enable: IntVar = alloc { value = 1 }


    if (setsockopt(sock,
            SOL_SOCKET, SO_REUSEADDR,
            enable.ptr, Int.SIZE_BYTES.toUInt()) < 0
    )
        fatal_error("setsockopt(SO_REUSEADDR)")


    posixMemset(srv_addr.ptr, 0, sizeOf<sockaddr_in>().toULong())
    srv_addr.sin_family = AF_INET.toUShort()
    srv_addr.sin_port = htons(port.toUShort())
    srv_addr.sin_addr.s_addr = htonl(INADDR_ANY)

    /* We bind to a port and turn this socket into a listening
     * socket.
     * */
    if (bind(sock, srv_addr.ptr.reinterpret(), sizeOf<sockaddr_in>().toUInt()) < 0)
        fatal_error("bind()")

    if (listen(sock, 10) < 0)
        fatal_error("listen()")

    return (sock)
}

fun add_accept_request(
    server_socket: Int,
    client_addr: CPointer<sockaddr_in>,
    client_addr_len: CPointer<socklen_tVar>,
): Int = nativeHeap.run {
    val sqe: CPointer<io_uring_sqe> = io_uring_get_sqe(ring.ptr)!!
    io_uring_prep_accept(sqe, server_socket, client_addr.reinterpret(), client_addr_len, 0)
    val req: CPointer<request> = allocWithFlex(request::iov, 1).ptr
    req.pointed.event_type = EVENT_TYPE_ACCEPT
    io_uring_sqe_set_data(sqe, req)
    io_uring_submit(ring.ptr)
    return 0
}

fun add_read_request(client_socket: Int): Int = nativeHeap.run {
    val sqe: CPointer<io_uring_sqe> = io_uring_get_sqe(ring.ptr)!!
    val req: CPointer<request> = allocWithFlex(request::iov, 1).ptr// malloc(sizeof(*req) + sizeof(c:iove))
    req.pointed.iov[0].iov_base = malloc(READ_SZ)
    req.pointed.iov[0].iov_len = READ_SZ.toULong()
    req.pointed.event_type = EVENT_TYPE_READ
    req.pointed.client_socket = client_socket
    posixMemset(req.pointed.iov[0].iov_base, 0, READ_SZ)
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

fun sendStaticStringContent(str: String, client_socket: Int): Unit = kotlinx.cinterop.nativeHeap.run {
    val req: CPointer<request> = allocWithFlex(request::iov, 1).ptr// malloc(sizeof(*req) + sizeof(c:iove))
    val slen: ULong = strlen(str)
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
    sendStaticStringContent(http_404_content!!.toKStringFromUtf8(), client_socket)
}

/*
 * Once a static file is identified to be served, this function is used to read the file
 * and write it over the client socket ~~using Linux's sendfile() system call. This saves us
 * the hassle of transferring file buffers from kernel to user space and back.~~
 */

fun copy_file_contents(file_path: CPointer<ByteVar>, file_size: off_t, iov: CPointer<iovec>): Unit = nativeHeap.run {
    val buf: CPointer<ByteVar> = malloc(file_size.toULong())!!.reinterpret()
    val fd = open(file_path.toKStringFromUtf8(), O_RDONLY)
    if (fd < 0)
        fatal_error("open")

    /* We should really check for short reads here */
    val i: ssize_t = read(fd, buf, file_size.toULong())
    if (i < file_size) {
        posixFprintf(posixStderr, "Encountered a short read.\n")
    }
    close(fd)
    iov.pointed.iov_base = buf
    iov.pointed.iov_len = file_size.toULong()
}

/*
 * Simple function to get the file extension of the file that we are about to serve.
 * */

fun get_filename_ext(filename: String): String =
    filename.drop(min(filename.length, max(0, filename.lastIndexOf('.', max(0, filename.length - 5))) + 1))

val sufCount: Int by lazy {
    var c = 0
    while (suf[c++].nz); c
}


/*
 * Sends the HTTP 200 OK header, the server string, for a few types of files, it can also
 * send the content type based on the file extension. It also sends the content length
 * header. Finally it send a '\r\n' in a line by itself signalling the end of headers
 * and the beginning of any content.
 * */

fun send_headers(path: String, len: off_t, iov: CPointer<iovec>): Unit = memScoped {
    val small_case_path = path.lowercase()
    val send_buffer = ByteArray(1024)
    val str = "HTTP/1.1 200 OK\r\n"
    var slen = (str.length).toULong()

    iov[0].iov_base = zh_malloc(slen)
    iov[0].iov_len = slen
    memcpy(iov[0].iov_base, str.utf8, slen)

    slen = (SERVER_STRING.length.toULong())
    iov[1].iov_base = zh_malloc(slen)
    iov[1].iov_len = slen
    memcpy(iov[1].iov_base, SERVER_STRING.utf8, slen)

    /*
     * Check the file extension for certain common types of files
     * on web pages and send the appropriate content-type header.
     * Since extensions can be mixed case like JPG, jpg or Jpg,
     * we turn the extension into lower case before checking.
     * */

    val ext: u_int32_tVar = alloc()
    strncpy(ext.ptr.reinterpret(), get_filename_ext(small_case_path), 4UL /* = kotlin.ULong */)

    val i: Int = 0
    for (i in 0 until sufCount)
        if (ext.value == suf[i])
            break
    val __s = ctype[i]!!.toKStringFromUtf8()

    strncpy(send_buffer.toCValues(), __s, slen)

    iov[2].iov_base = zh_malloc(slen)
    iov[2].iov_len = slen
    memcpy(iov[2].iov_base, send_buffer.toCValues(), slen)

    /* Send the content-length header, which is the file size in this case. */
    sprintf(send_buffer.toCValues(), "content-length: %ld\r\n", len)
    slen = strlen(send_buffer.toKString())
    iov[3].iov_base = zh_malloc(slen)
    iov[3].iov_len = slen
    memcpy(iov[3].iov_base, send_buffer.toCValues(), slen)

    /* Send the connection header. */
    sprintf(send_buffer.toCValues(), "connection: %s\r\n", "keep-alive")
    slen = strlen(send_buffer.toKString())
    iov[4].iov_base = zh_malloc(slen)
    iov[4].iov_len = slen
    memcpy(iov[4].iov_base, send_buffer.toCValues(), slen)

    /*
     * When the browser sees a '\r\n' sequence in a line on its own,
     * it understands there are no more headers. Content may follow.
     * */
    strcpy(send_buffer.toCValues(), "\r\n")
    slen = strlen(send_buffer.toKString())
    iov[5].iov_base = zh_malloc(slen)
    iov[5].iov_len = slen
    memcpy(iov[5].iov_base, send_buffer.toCValues(), slen)
}

fun handle_get_method(path: CPointer<ByteVar>, client_socket: Int) = nativeHeap.run {

    val final_path: ByteArray

    /*
     If a path ends in a trailing slash, the client probably wants the index
     file inside of that directory.
     */
    val path2 = path.toKStringFromUtf8()
    if (path2.endsWith('/')) {
        /*strcpy(final_path.toCValues(), "public")
        strcat(final_path.toCValues(), path2)
        strcat(final_path.toCValues(), "index.html")*/
        final_path = "public/$path2/index.html".encodeToByteArray()
    } else {
        /*    strcpy(final_path.toCValues(), "public")
            strcat(final_path.toCValues(), path2)
        */
        final_path = "public/$path2".encodeToByteArray()
    }

    /* The stat() system call will give you information about the file
     * like type (regular file, directory, etc), size, etc. */
    val path_stat: stat = nativeHeap.alloc()
    if (stat(final_path.toKString(), path_stat.ptr) == -1) {
        printf("404 Not Found: %s (%s)\n", final_path.toKString(), path)
        handle_http_404(client_socket)
    } else {
        /* Check if this is a normal/regular file and not a directory or something else */
        if (S_ISREG(path_stat.st_mode)) {
            val req: CPointer<request> =
                nativeHeap.allocWithFlex(request::iov, 7).ptr  /* zh_malloc(sizeof(*req) + (sizeof(c:iove) * 7))*/
            req.pointed.iovec_count = 7
            req.pointed.client_socket = client_socket
            send_headers(final_path.toKString(), path_stat.st_size, req.pointed.iov.reinterpret())
            copy_file_contents(final_path.pin().addressOf(0), path_stat.st_size, req.pointed.iov[6].ptr.reinterpret())
            printf("200 %s %ld bytes\n", final_path.toKString(), path_stat.st_size)
            add_write_request(req)
        } else {
            handle_http_404(client_socket)
            printf("404 Not Found: %s\n", final_path.toKString())
        }
    }
}

/*
 * This function looks at method used and calls the appropriate handler function.
 * Since we only implement GET and POST methods, it calls handle_unimplemented_method()
 * in case both these don't match. This sends an error to the client.
 * */

fun handle_http_method(method_buffer: CPointer<ByteVar>, client_socket: Int): Unit {
/*
    val method:CPointer<ByteVar>
    val path:CPointer<ByteVar>
    val saveptr:CPointer<ByteVar>

    method = strtok_r(method_buffer!!, " ", saveptr.reinterpret())!!
    strtolower(method)
    path = strtok_r(NULL, " ", saveptr.ptr)
*/
    val (Method, path) = method_buffer.toKStringFromUtf8().split(" ".toRegex(), 2)
    val method = Method.lowercase()

    if (strcmp(method, "get") == 0) {
        handle_get_method(path.replace("..", ".").pin().addressOf(0).reinterpret(), client_socket)
    } else {
        handle_unimplemented_method(client_socket)
    }
}

fun get_line(src: String, dest: CPointer<ByteVar>, dest_sz: Int): Int {
    for (i/*as int */ in 0 until dest_sz) {
        dest[i] = src[i].code.toByte()
        if (src[i] == '\r' && src[i + 1] == '\n') {
            dest[i] = 0.toByte()
            return 0
        }
    }
    return 1
}

fun handle_client_request(req: CPointer<request>): Int {
    val http_request = alloca(1024)
    /* Get the first line, which will be the request */
    if (get_line(req.pointed.iov[0].iov_base.toString(), http_request!!.reinterpret(), 1024).nz) {
        posixFprintf(posixStderr, "Malformed request\n")
        posixExit(1)
    }
    handle_http_method(http_request.reinterpret(), req.pointed.client_socket)
    return 0
}

fun server_loop(server_socket: Int) = nativeHeap.run {
    val cqe = alloc<io_uring_cqe>().ptr
    val client_addr: sockaddr_in = alloc()
    val client_addr_len: socklen_tVar = alloc { value = sizeOf<sockaddr_in>().toUInt(); }

    add_accept_request(server_socket, client_addr.ptr, client_addr_len.ptr)

    while (1.nz) {
        val ret: Int = io_uring_wait_cqe(ring.ptr, cqe.reinterpret())
        if (ret < 0)
            fatal_error("io_uring_wait_cqe")
        val req: CPointer<request> = cqe.pointed.user_data.toLong().toCPointer()!!
        if (cqe.pointed.res < 0) {
            posixFprintf(posixStderr, "Async request failed: %s for event: %d\n",
                strerror(-cqe.pointed.res), req.pointed.event_type)
            posixExit(1)
        }

        when (req.pointed.event_type) {
            EVENT_TYPE_ACCEPT -> {
                add_accept_request(server_socket, client_addr.ptr, client_addr_len.ptr)
                add_read_request(cqe.pointed.res)
                free(req.pointed)
                break
            }
            EVENT_TYPE_READ -> {

                if (cqe.pointed.res.nz) {
                    close(req.pointed.client_socket)
                    posixFprintf(posixStderr, "Empty request!\n")
                    break
                } else {
                    handle_client_request(req)
                    free(req.pointed.iov[0].iov_base)
                    free(req)
                    break
                }
            }
            EVENT_TYPE_WRITE -> {
                add_read_request(req.pointed.client_socket)
                for (i/*as int */ in 0 until req.pointed.iovec_count) {
                    free(req.pointed.iov[i].iov_base)
                }
                free(req)
                break
            }
        }
        /* Mark this request as processed */
        io_uring_cqe_seen(ring.ptr, cqe)
    }
}

fun sigint_handler(signo: Int): Unit {
    printf("^C pressed. Shutting down.\n")
    io_uring_queue_exit(ring.ptr)
    posixExit(0)
}

fun httpd() = memScoped {
    var server_socket: Int = setup_listening_socket(DEFAULT_SERVER_PORT)

    signal(SIGINT,
        ::sigint_handler.objcPtr().toLong().toCPointer<CFunction<(kotlin.Int) -> kotlin.Unit>>() as __sighandler_t)
    io_uring_queue_init(QUEUE_DEPTH, ring.ptr, 0)
    server_loop(server_socket)
}
