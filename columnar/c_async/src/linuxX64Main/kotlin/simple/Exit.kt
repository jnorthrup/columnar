package simple

/** simple macro*/
fun exit(status: Int): Int =status.also { platform.posix.exit(status) }