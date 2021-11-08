import kotlinx.cinterop.*
import kotlinx.cinterop.nativeHeap.alloc
import platform.posix.*

fun setup_listening_socket(port: Int): Int {
    val sock = socket(PF_INET, SOCK_STREAM, 0)
    val srv_addr: sockaddr_in = nativeHeap.alloc()
    if (sock != -1) {
        val enable: Int = 1
        val __optval = 1.objcPtr() as CPointer<IntVar>
        val reinterpret = __optval.reinterpret<IntVar>()

        if (setsockopt(sock,
                SOL_SOCKET,
                SO_REUSEADDR,
                __optval as CValuesRef<IntVar>,
                Int.SIZE_BYTES.toUInt()   /* = kotlin.UInt */) >= 0) {
            memset(srv_addr.ptr, 0, sockaddr_in.size.toULong() as size_t /* = kotlin.ULong */)
            srv_addr.sin_family = AF_INET.toUShort() as sa_family_t /* = kotlin.UShort */
            srv_addr.sin_port = htons(port.toUShort() as uint16_t /* = kotlin.UShort */)
            srv_addr.sin_addr.s_addr = htonl(INADDR_ANY)

            /* We bind to a port and turn this socket into a listening
             * socket.
             * */
            if (bind(sock, srv_addr.ptr.reinterpret<sockaddr>() as CValuesRef<sockaddr>, sockaddr_in.size.toUInt()) >= 0) {
                if (listen(sock, 10) >= 0) {
                    return sock  //goal condition
                } else return fatal_error("listen()")
            } else {
                return fatal_error("bind()"); }
        } else return fatal_error("setsockopt(SO_REUSEADDR)")
    } else return fatal_error("socket()")
}