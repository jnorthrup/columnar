import c_async.*
import c_async.iovec
import c_async.socklen_tVar
import kotlinx.cinterop.*
import platform.posix.*
import platform.posix.AF_INET
import platform.posix.PF_INET
import platform.posix.SOCK_STREAM
import platform.posix.SOL_SOCKET
import platform.posix.SO_REUSEADDR
import platform.posix.bind
import platform.posix.fprintf
import platform.posix.listen
import platform.posix.perror
import platform.posix.printf
import platform.posix.setsockopt
import platform.posix.sockaddr
import platform.posix.socket
import platform.posix.sprintf
import platform.posix.stat
import platform.posix.stderr
import kotlin.Int


//struct request *selectorKeyAttachment(const struct io_uring_cqe *completion_queue_entry_ptr);

//struct request *selectorKeyAttachment(const struct io_uring_cqe *completion_queue_entry_ptr) { return (struct request *) completion_queue_entry_ptr->user_data; }

fun selectorKeyAttachment:CPointerVar<request>(io_uring_cqe:CPointerVar<completion_queue_entry_ptr>) {

    memScoped {
        val userData = completion_queue_entry_ptr.user_data

    }
    return userData
}

/*
 * Utility function to convert a string to lower case.
 * */

fun strtolower(str1: CArrayPointer<ByteVar>) {

    var c: Int = 0
    var b: Int
    str1.let {
        do {
            b = it[c].toInt()
            if (b == 0) break
            it[c] = tolower(b).toByte()
            c++
        } while (b != 0)
    }
}

fun exit(x: Int) = x.also { platform.posix.exit(x) }

/*
 One function that prints the system call and the error details
 and then exits with error code 1. Non-zero meaning things didn't go well.
 */
fun fatal_error(syscall: String) =
    perror(syscall).let {
        exit(1)
    }


/*
 * Helper function for cleaner looking code.
 * */

fun zh_malloc(size: size_t) = malloc(size /* = kotlin.ULong */)!! /*!! ?: run{
            fprintf(stderr, "Fatal error: unable to allocate memory: %d\n", size)
            exit(1)
        }*/

/*
 * This function is responsible for setting up the main listening socket used by the
 * web server.
 * */

fun setup_listening_socket(port: Int): Int {
    val srv_addr = nativeHeap.alloc<sockaddr_in>()
    val sock = socket(PF_INET, SOCK_STREAM, 0)
    if (sock != -1) {
        val enable = 1
        if (setsockopt(sock,
                SOL_SOCKET,
                SO_REUSEADDR,
                StableRef.create<Int>(enable).asCPointer(),
                Int.SIZE_BYTES.toUInt() /* = kotlin.UInt */) >= 0
        ) {
            val ssize = sockaddr_in.size
            bzero(srv_addr.ptr/*, 0*/, ssize.toULong() /* = kotlin.ULong */)
            srv_addr.sin_family = AF_INET.toUShort() /* = kotlin.UShort */
            srv_addr.sin_port = htons(port.toUShort() /* = kotlin.UShort */)
            srv_addr.sin_addr.s_addr = htonl(INADDR_ANY)

            /* We bind to a port and turn this socket into a listening
             * socket.
             * */
            val rawValue = StableRef.create(srv_addr)
            val reinterpret = rawValue.asCPointer().reinterpret<sockaddr>()
            if (bind(sock,
                    reinterpret as CValuesRef<sockaddr>,
                    ssize.toUInt() /* = kotlin.UInt */) >= 0
            ) {
                if (listen(sock, 10) >= 0) {
                    return sock  //goal condition
                } else return fatal_error("listen()")
            } else return fatal_error("bind()")
        } else return fatal_error("setsockopt(SO_REUSEADDR)")
    } else return fatal_error("socket()")
}


fun add_accept_request(
    server_socket: Int,
    client_addr: CValuesRef<c_async.sockaddr>,
    client_addr_len: CValuesRef<socklen_tVar /* = kotlinx.cinterop.UIntVarOf<kotlin.UInt> */>
): Int {
    val submission_queue_entry: CPointer<io_uring_sqe>? = io_uring_get_sqe(the_ring_on_the_stack.ptr)
    io_uring_prep_accept(submission_queue_entry,
        server_socket,
        client_addr as CValuesRef<c_async.sockaddr>,
        client_addr_len as CValuesRef<socklen_tVar /* = kotlinx.cinterop.UIntVarOf<kotlin.UInt> */>,
        0)

    val req: request = nativeHeap.alloc {
        event_type = EVENT_TYPE_ACCEPT as Int
    }

    io_uring_sqe_set_data(submission_queue_entry, req.ptr as CValuesRef<*>)
    io_uring_submit(the_ring_on_the_stack.ptr)
    return 0
}

fun add_read_request(client_fd1: Int): Int {
    val submission_queue_entry = io_uring_get_sqe(the_ring_on_the_stack.ptr)
    val req: request = nativeHeap.alloc {
        iov[0].iov_base = malloc(READ_SZ)
        iov[0].iov_len = READ_SZ.toULong() as size_t /* = kotlin.ULong */
        event_type = EVENT_TYPE_READ
        client_fd = client_fd1
        memset(iov[0].iov_base, 0, READ_SZ)

        /* Linux kernel 5.5 has support for readv, but not for recv() or read() */
        io_uring_prep_readv(submission_queue_entry, client_fd, iov[0].ptr as CValuesRef<iovec>, 1, 0)
    }
    io_uring_sqe_set_data(submission_queue_entry, req.ptr)
    io_uring_submit(the_ring_on_the_stack.ptr)
    return 0
}

void _send_static_string_content (  str:String,  client_fd:Int) :Int{
    val req: CPointer<request>= zh_malloc (sizeof(*req)+sizeof(struct iovec))
    size_t slen = strlen (str)
    req->iovec_count = 1
    req->client_fd = client_fd
    req->iov[0].iov_base = zh_malloc(slen)
    req->iov[0].iov_len = slen
    memcpy(req->iov[0].iov_base, str, slen)
    add_write_request(req)
}

/*
 * When ZeroHTTPd encounters any other HTTP method other than GET or POST, this function
 * is used to inform the client.
 * */

void handle_unimplemented_method (fun client_fd) :Int{
    _send_static_string_content(unimplemented_content, client_fd)
}

/*
 * This function is used to send a "HTTP Not Found" code and message to the client in
 * case the file requested is not found.
 * */

void handle_http_404 (fun client_fd) :Int{
    _send_static_string_content(http_404_content, client_fd)
}

/*
 * Once a static file is identified to be served, this function is used to read the file
 * and write it over the client socket using Linux's sendfile() system call. This saves us
 * the hassle of transferring file buffers from kernel to user space and back.
 * */

void copy_file_contents ( file_path:String ,  file_size:ULong  ,  iov:CValuesRef<iovec>) {
    val buf = zh_malloc(file_size as size_t /* = kotlin.ULong */)
    val fd = open(file_path as String, O_RDONLY as Int)
    if (fd >= 0) {

        /* We should really check for short reads here */
        Int ret = read (fd, buf, file_size)
        if (ret < file_size) fprintf(stderr, "Encountered a short read.\n")
        close(fd)
        iov->iov_base = buf
        iov->iov_len = file_size
    } else fatal_error("open")
}

/*
 * Simple function to get the file extension of the file that we are about to serve.
 * */

val get_filename_ext: String  (
val filename: String )
{
    val dot: String = strrchr(filename, '.')
    if (!dot || dot == filename)
        return ""
    return dot + 1
}

/*
 * Sends the HTTP 200 OK header, the server string, for a few types of files, it can also
 * send the content type based on the file extension. It also sends the content length
 * header. Finally it send a '\r\n' in a line by itself signalling the end of headers
 * and the beginning of any content.
 * */

void send_headers (
val path: String , off_t len, var iov:CValuesRef<iovec>)
{
    char small_case_path [1024]
    char send_buffer [1024]
    strcpy(small_case_path, path)
    strtolower(small_case_path)

    val str: String = "HTTP/1.0 200 OK\r\n"
    unsigned long slen = strlen(str)
    iov[0].iov_base = zh_malloc(slen)
    iov[0].iov_len = slen
    memcpy(iov[0].iov_base, str, slen)

    slen = strlen(SERVER_STRING)
    iov[1].iov_base = zh_malloc(slen)
    iov[1].iov_len = slen
    memcpy(iov[1].iov_base, SERVER_STRING, slen)

    /*
     * Check the file extension for certain common types of files
     * on web pages and send the appropriate content-type header.
     * Since extensions can be mixed case like JPG, jpg or Jpg,
     * we turn the extension into lower case before checking.
     * */
    val file_ext: String = get_filename_ext(small_case_path)
    if (strcmp("jpg", file_ext) == 0)
        strcpy(send_buffer, "Content-Type: image/jpeg\r\n")
    if (strcmp("jpeg", file_ext) == 0)
        strcpy(send_buffer, "Content-Type: image/jpeg\r\n")
    if (strcmp("png", file_ext) == 0)
        strcpy(send_buffer, "Content-Type: image/png\r\n")
    if (strcmp("gif", file_ext) == 0)
        strcpy(send_buffer, "Content-Type: image/gif\r\n")
    if (strcmp("htm", file_ext) == 0)
        strcpy(send_buffer, "Content-Type: text/html\r\n")
    if (strcmp("html", file_ext) == 0)
        strcpy(send_buffer, "Content-Type: text/html\r\n")
    if (strcmp("js", file_ext) == 0)
        strcpy(send_buffer, "Content-Type: application/javascript\r\n")
    if (strcmp("css", file_ext) == 0)
        strcpy(send_buffer, "Content-Type: text/css\r\n")
    if (strcmp("txt", file_ext) == 0)
        strcpy(send_buffer, "Content-Type: text/plain\r\n")
    slen = strlen(send_buffer)
    iov[2].iov_base = zh_malloc(slen)
    iov[2].iov_len = slen
    memcpy(iov[2].iov_base, send_buffer, slen)

    /* Send the content-length header, which is the file size in this case. */
    sprintf(send_buffer, "content-length: %ld\r\n", len)
    slen = strlen(send_buffer)
    iov[3].iov_base = zh_malloc(slen)
    iov[3].iov_len = slen
    memcpy(iov[3].iov_base, send_buffer, slen)

    /*
     * When the browser sees a '\r\n' sequence in a line on its own,
     * it understands there are no more headers. Content may follow.
     * */
    strcpy(send_buffer, "\r\n")
    slen = strlen(send_buffer)
    iov[4].iov_base = zh_malloc(slen)
    iov[4].iov_len = slen
    memcpy(iov[4].iov_base, send_buffer, slen)
}

void handle_get_method (
val path: String ,
fun client_fd) :Int{
    char final_path [1024]
    /*
     If a path ends in a trailing slash, the client probably wants the index
     file inside that directory.
     */
    if (path[strlen(path) - 1] == '/') {
        strcpy(final_path, "public")
        strcat(final_path, path)
        strcat(final_path, "index.html")
    } else {
        strcpy(final_path, "public")
        strcat(final_path, path)
    }

    /* The stat() system call will give you information about the file
     * like type (regular file, directory, etc), size, etc. */
    struct stat path_stat
    if (stat(final_path, path_stat.ptr) == -1) {
        printf("404 Not Found: %s (%s)\n", final_path, path)
        handle_http_404(client_fd)
    } else {
        /* Check if this is a normal/regular file and not a directory or something else */
        if (S_ISREG(path_stat.st_mode)) {
            val req: CPointer<request>= zh_malloc (sizeof(*req)+sizeof(struct iovec) * 6)
            req->iovec_count = 6
            req->client_fd = client_fd
            send_headers(final_path, path_stat.st_size, req->iov)
            copy_file_contents(final_path, path_stat.st_size, req.ptr->iov[5])
            printf("200 %s %ld bytes\n", final_path, path_stat.st_size)
            add_write_request(req)
        } else {
            handle_http_404(client_fd)
            printf("404 Not Found: %s\n", final_path)
        }
    }
}

/*
 * This function looks at method used and calls the appropriate handler function.
 * Since we only implement GET and POST methods, it calls handle_unimplemented_method()
 * in case both these don't match. This sends an error to the client.
 * */

void handle_http_method (
val method_buffer: String ,
fun client_fd) :Int{
    val method: String , *path, *saveptr

    method = strtok_r(method_buffer, " ", saveptr.ptr)
    strtolower(method)
    path = strtok_r(NULL, " ", saveptr.ptr)

    if (strcmp(method, "get") == 0) {
        handle_get_method(path, client_fd)
    } else {
        handle_unimplemented_method(client_fd)
    }
}

fun get_line(val src: String, char *dest, Int dest_sz) :Int{
    for (fun i = 0; i < dest_sz; i++) :Int{
        dest[i] = src[i]
        if (src[i] == '\r' &src.ptr[i+1] == '\n') {
        dest[i] = '\0'
        return 0
    }
    }
    return 1
}

fun handle_client_request(var req: CValuesRef<request>): Int {
    char http_request [1024]
    /* Get the first line, which will be the request */
    if (!get_line(req->iov[0].iov_base, http_request, sizeof(http_request))) {
        handle_http_method(http_request, req->client_fd)
        return 0
    }
    fprintf(stderr, "Malformed request\n")
    exit(1)

}

void server_loop (fun server_socket_fd) :Int{
    var completion_queue_entry_ptr: CValuesRef<io_uring_cqe>
    struct sockaddr_in client_addr_on_stack
    socklen_t client_addr_len = sizeof (client_addr_on_stack)

    add_accept_request(server_socket_fd, client_addr_on_stack.ptr, client_addr_len.ptr)

    while (1) {
        Int ret = io_uring_wait_cqe (the_ring_on_the_stack.ptr, completion_queue_entry_ptr.ptr)
        if (ret >= 0) {
            val req_heap_ptr: CPointer<request>= selectorKeyAttachment (completion_queue_entry_ptr)
            __s32 resultCode = completion_queue_entry_ptr->res
            if (0 <= resultCode) {
                switch(req_heap_ptr->event_type) {
                    case EVENT_TYPE_ACCEPT : {
                        add_accept_request(server_socket_fd, client_addr_on_stack.ptr, client_addr_len.ptr)
                        add_read_request(resultCode)
                        free(req_heap_ptr)
                        break
                    }
                    case EVENT_TYPE_READ : {
                        __s32 result_code = resultCode
                                if (result_code != 0) {
                                    handle_client_request(req_heap_ptr)
                                    free(req_heap_ptr->iov[0].iov_base)
                                    free(req_heap_ptr)
                                } else fprintf(stderr, "Empty request!\n")
                        break
                    }
                    case EVENT_TYPE_WRITE :
                    for (Int i = 0; i < req_heap_ptr->iovec_count; i++) free(req_heap_ptr->iov[i].iov_base)
                    close(req_heap_ptr->client_fd)
                    free(req_heap_ptr)
                    break
                }
                /* Mark this request as processed */
                io_uring_cqe_seen(the_ring_on_the_stack.ptr, completion_queue_entry_ptr)
            } else {
                fprintf(stderr, "Async request failed: %s for event: %d\n", strerror(-resultCode),
                    req_heap_ptr->event_type)
                exit(1)
            }
        } else fatal_error("io_uring_wait_cqe")
    }
}


fun add_write_request(req: CValuesRef<request>): Int {
    val reinterpret: CPointer<io_uring>? = the_ring_on_the_stack?.ptr?.reinterpret<io_uring>()
    val ioUringGetSqe: CPointer<io_uring_sqe>? = io_uring_get_sqe(reinterpret)
    val submission_queue_entry: CPointer<io_uring_sqe>? = ioUringGetSqe

    struct_from_(req)

    req.
    memScoped {
        usePinned {

        }

        req.event_type = EVENT_TYPE_WRITE
        io_uring_prep_writev(submission_queue_entry, req->client_fd, req->iov, req->iovec_count, 0)


    }



    io_uring_sqe_set_data(submission_queue_entry, req)
    io_uring_submit(the_ring_on_the_stack.ptr)
    return 0
}
