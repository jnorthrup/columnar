package simple

import platform.posix.off_t
import platform.posix.socklen_tVar
import platform.posix.ssize_t
import platform.posix.stat
import platform.posix.*
import kotlinx.cinterop.*
import uring.*
import uring.iovec

fun main(args: Array<String>): Unit {
    val server_socket: Int = setup_listening_socket(DEFAULT_SERVER_PORT)

    uring.signal(uring.SIGINT,
        staticCFunction(
            fun(signo: Int) {
                println("^C pressed. Shutting down. signal $signo\n")
                io_uring_queue_exit(the_ring_on_the_stack.ptr)
                platform.posix.exit(0)
            }))
    io_uring_queue_init(QUEUE_DEPTH, the_ring_on_the_stack.ptr, 0)
    server_loop(server_socket)
//    return 0
}

fun Request(event_type: UInt = 0u, client_fd: UShort = 0u, iovec_count: UByte = 1u): CPointer<request> {


    val reinterpret = malloc((request.size + iovec.size * iovec_count.toInt()).toULong())?.reinterpret<request>()!!
    return reinterpret.pointed.apply {
        this.client_fd = client_fd
        this.event_type = event_type
        this.iovec_count = iovec_count
    }.ptr
}

/*
 * This function is responsible for setting up the simple.simple.main listening socket used by the
 * web server.
 * */

fun add_accept_request(
    server_socket1: Int,
    client_addr: CPointer<sockaddr_in>,
    client_addr_len: kotlinx.cinterop.CValuesRef<socklen_tVar>,
): Int {
    val submission_queue_entry = io_uring_get_sqe(the_ring_on_the_stack.ptr)!!
    io_uring_prep_accept(submission_queue_entry,
        server_socket1,
        client_addr.reinterpret<uring.sockaddr>() as CValuesRef<uring.sockaddr>,
        client_addr_len,
        0)
    val req: CPointer<request> = Request(EVENT_TYPE_ACCEPT.toUInt())
    io_uring_sqe_set_data(submission_queue_entry, req)
    io_uring_submit(the_ring_on_the_stack.ptr)
    return 0
}

fun add_read_request(client_fd1: Int): Int {
    val submission_queue_entry = io_uring_get_sqe(the_ring_on_the_stack.ptr)
    val req: request = Request(event_type = EVENT_TYPE_READ.toUInt(),
        client_fd = client_fd1.toUShort()
    ).pointed.apply {
        iov[0].iov_base = malloc(READ_SZ)
        iov[0].iov_len = READ_SZ.toULong() /* = kotlin.ULong */
        memset(iov[0].iov_base, 0, READ_SZ)
    }
    /* Linux kernel 5.5 has support for readv, but not for recv() or read() */
    io_uring_prep_readv(submission_queue_entry, client_fd1, req.iov[0].ptr, 1, 0)
    io_uring_sqe_set_data(submission_queue_entry, req.ptr)
    io_uring_submit(the_ring_on_the_stack.ptr)
    return 0
}

fun add_write_request(req: CPointer<request>): Int {
    val submission_queue_entry = io_uring_get_sqe(the_ring_on_the_stack.ptr)
    val pointed = req.pointed
    pointed.event_type = EVENT_TYPE_WRITE.toUInt()
    io_uring_prep_writev(submission_queue_entry,
        pointed.client_fd.toInt(),
        pointed.iov,
        pointed.iovec_count.toUInt(),
        0)
    io_uring_sqe_set_data(submission_queue_entry, req)
    io_uring_submit(the_ring_on_the_stack.ptr)
    return 0
}

fun _send_static_string_content(str: String, client_fd1: Int): Unit {
    val slen = strlen(str)
    val req = Request(EVENT_TYPE_WRITE.toUInt(),
        client_fd = client_fd1.toUShort())
    add_write_request(req.pointed.apply {
        iov[0].iov_base = zh_malloc(slen)
        iov[0].iov_len = slen
        memcpy(iov[0].iov_base, str.cstr as CValuesRef<*>, slen)

    }.ptr)
}

/*
 * When ZeroHTTPd encounters any other HTTP method other than GET or POST, this function
 * is used to inform the client.
 * */

fun handle_unimplemented_method(client_fd: Int): Unit {
    _send_static_string_content(unimplemented_content.toString(), client_fd)
}

/*
 * This function is used to send a "HTTP Not Found" code and message to the client in
 * case the file requested is not found.
 * */

fun handle_http_404(client_fd: Int): Unit {
    _send_static_string_content(http_404_content.toString(), client_fd)
}

/*
 * Once a static file is identified to be served, this function is used to read the file
 * and write it over the client socket using Linux's sendfile() system call. This saves us
 * the hassle of transferring file buffers from kernel to user space and back.
 * */

fun copy_file_contents(file_path: CPointer<ByteVar>, file_size: off_t, iov: CPointer<iovec>): Unit {


    val buf = zh_malloc(file_size.toULong() /* = kotlin.ULong */)
    val fd = open(file_path.toString(), O_RDONLY)
    if (fd >= 0) {

        /* We should really check for short reads here */
        val ret: ssize_t = read(fd, buf as CValuesRef<*>, file_size.toULong() /* = kotlin.ULong */)
        if (ret < file_size) fprintf(stderr, "Encountered a short read.\n")
        close(fd)
        iov.pointed.run {
            iov_base = buf
            iov_len = file_size.toULong() /* = kotlin.ULong */

        }
    } else fatal_error("open")
}

/*
 * Simple function to get the file extension of the file that we are about to serve.
 * */

fun get_filename_ext(filename: String): String {
    val dot: String = strrchr(filename, '.'.code).toString()
    when (dot) {
        filename -> return ""
        else -> return dot + 1
    }
}

/*
 * Sends the HTTP 200 OK header, the server string, for a few types of files, it can also
 * send the content type based on the file extension. It also sends the content length
 * header. Finally it send a '\r\n' in a line by itself signalling the end of headers
 * and the beginning of any content.
 * */

fun send_headers(path: String, len: off_t, iov: CPointer<iovec>): Unit {
    val small_case_path = alloca(1024.toULong() /* = kotlin.ULong */)!!.reinterpret<ByteVar>()
    val send_buffer = ByteArray(1024)
    strcpy(small_case_path, path)

    strtolower(small_case_path)


    var str = "HTTP/1.0 200 OK\r\n"
    var slen: ULong = strlen(str)
    iov[0].iov_base = zh_malloc(slen)
    iov[0].iov_len = slen
    memcpy(iov[0].iov_base, str.refTo(0) as CValuesRef<*>, slen)

    slen = strlen(SERVER_STRING)
    iov[1].iov_base = zh_malloc(slen)
    iov[1].iov_len = slen
    memcpy(iov[1].iov_base, SERVER_STRING.cstr, slen)

    /*
     * Check the file extension for certain common types of files
     * on web pages and send the appropriate content-type header.
     * Since extensions can be mixed case like JPG, jpg or Jpg,
     * we turn the extension into lower case before checking.
     * */
    val file_ext: String = get_filename_ext(small_case_path.toString())
    if (strcmp("jpg", file_ext) == 0)
        strcpy(send_buffer.toCValues(), "Content-Type: image/jpeg\r\n")
    if (strcmp("jpeg", file_ext) == 0)
        strcpy(send_buffer.toCValues(), "Content-Type: image/jpeg\r\n")
    if (strcmp("png", file_ext) == 0)
        strcpy(send_buffer.toCValues(), "Content-Type: image/png\r\n")
    if (strcmp("gif", file_ext) == 0)
        strcpy(send_buffer.toCValues(), "Content-Type: image/gif\r\n")
    if (strcmp("htm", file_ext) == 0)
        strcpy(send_buffer.toCValues(), "Content-Type: text/html\r\n")
    if (strcmp("html", file_ext) == 0)
        strcpy(send_buffer.toCValues(), "Content-Type: text/html\r\n")
    if (strcmp("js", file_ext) == 0)
        strcpy(send_buffer.toCValues(), "Content-Type: application/javascript\r\n")
    if (strcmp("css", file_ext) == 0)
        strcpy(send_buffer.toCValues(), "Content-Type: text/css\r\n")
    if (strcmp("txt", file_ext) == 0)
        strcpy(send_buffer.toCValues(), "Content-Type: text/plain\r\n")
    slen = strlen(send_buffer.toString())
    iov[2].iov_base = zh_malloc(slen)
    iov[2].iov_len = slen
    memcpy(iov[2].iov_base, send_buffer.toCValues(), slen)

    /* Send the content-length header, which is the file size in this case. */
    sprintf(send_buffer.toCValues(), "content-length: %ld\r\n", len)
    slen = strlen(send_buffer.toString())
    iov[3].iov_base = zh_malloc(slen)
    iov[3].iov_len = slen
    memcpy(iov[3].iov_base, send_buffer.toCValues(), slen)

    /*
     * When the browser sees a '\r\n' sequence in a line on its own,
     * it understands there are no more headers. Content may follow.
     * */
    strcpy(send_buffer.toCValues(), "\r\n")
    slen = strlen(send_buffer.toString())
    iov[4].iov_base = zh_malloc(slen)
    iov[4].iov_len = slen
    memcpy(iov[4].iov_base, send_buffer.toCValues(), slen)
}

fun handle_get_method(path: CPointer<ByteVar>, client_fd: Int): Unit {
    val final_path = malloc(1024)!!.reinterpret<ByteVar>()
    /*
     If a path ends in a trailing slash, the client probably wants the index
     file inside that directory.
     */
    val strlen = strlen(path.toString())
    if (path[(strlen.toInt() - 1.toInt()).toInt()].toInt() == '/'.code.toInt()) {
        strcpy(final_path, "public")
        strcat(final_path, path.toString())
        strcat(final_path, "index.html")
    } else {
        strcpy(final_path, "public")
        strcat(final_path, path.toString())
    }

    /* The stat() system call will give you information about the file
     * like type (regular file, directory, etc), size, etc. */
    val path_stat: stat = nativeHeap.alloc()
    /* Check if this is a normal/regular file and not a directory or something else */
    if (stat(final_path.toString(), path_stat.ptr) == -1) {
        printf("404 Not Found: %s (%s)\n", final_path, path)
        handle_http_404(client_fd)
    } else if (path_stat.st_mode and platform.posix.__S_IFREG.toUInt() != 0u) {
        val req = Request(client_fd = client_fd.toUShort(),
            iovec_count = 6.toUByte()).pointed.apply {
        }



        send_headers(final_path.toString(), path_stat.st_size, req.iov)
        copy_file_contents(final_path, path_stat.st_size, req.iov[5].ptr)
        printf("200 %s %ld bytes\n", final_path, path_stat.st_size)
        add_write_request(req.ptr)
    } else {
        handle_http_404(client_fd)
        printf("404 Not Found: %s\n", final_path)
    }
}

/*
 * This function looks at method used and calls the appropriate handler function.
 * Since we only implement GET and POST methods, it calls simple.handle_unimplemented_method()
 * in case both these don't match. This sends an error to the client.
 * */

fun handle_http_method(method_buffer: CPointer<ByteVar>, client_fd: Int): Unit {
    val saveptr = nativeHeap.alloc<CPointerVar<ByteVar>>()
    val method = strtok_r(method_buffer, " ", saveptr.ptr)!!
    strtolower(method.reinterpret())
    val path1 = strtok_r(null, " ", saveptr.ptr)!!
    if (strcmp(method.toString(), "get") == 0) {
        handle_get_method(path1, client_fd)
    } else {
        handle_unimplemented_method(client_fd)
    }
}

fun get_line(src: String, dest: CPointer<ByteVar>, dest_sz: Int): Int {

    for (i in 0 until dest_sz) {
        dest[i] = src[i].code.toByte()
        if (src[i] == '\r' && src[i + 1] == '\n') {
            dest[i] = 0.toByte()
            return 0
        }
    }
    return 1
}

fun handle_client_request(req: CPointer<request>): Int {
    val http_request = alloca(1024)!!.reinterpret<ByteVar>()
    /* Get the first line, which will be the request */
    if (0 == get_line(req.pointed.iov[0].iov_base.toString(), http_request,
            1024)
    ) {
        handle_http_method(http_request, req.pointed.client_fd.toInt())
        return 0
    }
    fprintf(stderr, "Malformed request\n")
    return exit(1)
}

fun server_loop(server_socket_fd: Int): Unit {
    val completion_queue_entry: io_uring_cqe = nativeHeap.alloc<io_uring_cqe>()
    val client_addr_on_stack: sockaddr_in = nativeHeap.alloc()
    val client_addr_len = nativeHeap.alloc<socklen_tVar>()

    add_accept_request(server_socket_fd, client_addr_on_stack.ptr, client_addr_len.ptr)

    while (true) {
        val ret: Int = io_uring_wait_cqe(the_ring_on_the_stack.ptr, completion_queue_entry.ptr.reinterpret())
        if (ret >= 0) {
            val req_heap_ptr = selectorKeyAttachment(completion_queue_entry.ptr)!!
            var resultCode1 = completion_queue_entry.res
            if (0 <= resultCode1) {
                when (req_heap_ptr.pointed.event_type.toInt()) {
                    EVENT_TYPE_ACCEPT -> {
                        add_accept_request(server_socket_fd, client_addr_on_stack.ptr, client_addr_len.ptr)
                        add_read_request(resultCode1)
                        free(req_heap_ptr)
                    }
                    EVENT_TYPE_READ -> {
                        val result_code = resultCode1
                        if (result_code != 0) {
                            handle_client_request(req_heap_ptr)
                            free(req_heap_ptr.pointed.iov[0].iov_base)
                            free(req_heap_ptr)
                        } else fprintf(stderr, "Empty request!\n")
                    }
                    EVENT_TYPE_WRITE ->
                        for (i in 0 until req_heap_ptr.pointed.iovec_count.toInt()) {
                            free(req_heap_ptr.pointed.iov[i].iov_base)
                            close(req_heap_ptr.pointed.client_fd.toInt())
                            free(req_heap_ptr)
                        }

                }
                /* Mark this request as processed */
                io_uring_cqe_seen(the_ring_on_the_stack.ptr, completion_queue_entry.ptr)
            } else {
                fprintf(stderr, "Async request failed: %s for event: %d\n", strerror(-resultCode1),
                    req_heap_ptr.pointed.event_type)
                exit(1)
            }
        } else fatal_error("io_uring_wait_cqe")
    }
}




