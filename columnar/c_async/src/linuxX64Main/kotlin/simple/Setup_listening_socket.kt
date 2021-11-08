package simple

import kotlinx.cinterop.*
import platform.posix.*

fun setup_listening_socket(port: Int): Int {
    val sock = socket(PF_INET, SOCK_STREAM, 0)
    val srv_addr: sockaddr_in = nativeHeap.alloc()
    if (sock != -1) {
        if (setsockopt(sock,
                SOL_SOCKET,
                SO_REUSEADDR,
                cValuesOf(1) as CValuesRef<*>,
                Int.SIZE_BYTES.toUInt()) >= 0
        ) {
            memset(srv_addr.ptr, 0, sockaddr_in.size.toULong() /* = kotlin.ULong */)
            srv_addr.sin_family = AF_INET.toUShort() /* = kotlin.UShort */
            srv_addr.sin_port = htons(port.toUShort() /* = kotlin.UShort */)
            srv_addr.sin_addr.s_addr = htonl(INADDR_ANY)

            /* We bind to a port and turn this socket into a listening
         * socket.
         * */
            if (bind(sock,
                    srv_addr.ptr.reinterpret<sockaddr>() as CValuesRef<sockaddr>,
                    sockaddr_in.size.toUInt()) >= 0
            ) {
                if (listen(sock, 10) >= 0) return sock
                return fatal_error("listen()")
            }
            return fatal_error("bind()")
        }
        return fatal_error("setsockopt(SO_REUSEADDR)")
    }
    return fatal_error("socket()")
}