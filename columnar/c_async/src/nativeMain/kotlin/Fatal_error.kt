import platform.posix.exit
import platform.posix.perror

/**
     One function that prints the system call and the error details
     and then exits with error code 1. Non-zero meaning things didn't go well.
     */
    fun fatal_error(syscall: String): Int =
        perror(syscall).let{
    1.also { exit(1) }
}