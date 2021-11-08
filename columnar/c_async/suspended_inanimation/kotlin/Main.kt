import c_async.*
import interop.DEFAULT_SERVER_PORT
import interop.QUEUE_DEPTH
import interop.the_ring_on_the_stack

import kotlinx.cinterop.memScoped

import kotlinx.cinterop.ptr
import kotlinx.cinterop.staticCFunction
import platform.posix.SIGINT
import platform.posix.__sighandler_t
import platform.posix.printf
import platform.posix.signal


fun main(args: Array<String>): Int {
    if (args.isEmpty()) {
             // the_ring_on_the_stack = alloc(io_uring)
            val server_socket = setup_listening_socket(DEFAULT_SERVER_PORT)
            val __handler = { signo: Int ->
                printf("^C pressed. Shutting down. signal %d\n", signo)
                io_uring_queue_exit(the_ring_on_the_stack.ptr)
                exit(0)
                Unit
            }
            signal(SIGINT,staticCFunction( __handler) as __sighandler_t /* = kotlinx.cinterop.CPointer<kotlinx.cinterop.CFunction<(kotlin.Int) -> kotlin.Unit>> */
            )
            io_uring_queue_init(QUEUE_DEPTH, the_ring_on_the_stack.ptr, 0)
            server_loop(server_socket)
            return 0
        }
     else return 1
}