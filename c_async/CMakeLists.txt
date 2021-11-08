cmake_minimum_required(VERSION 3.18)
project(clion C)

set(CMAKE_C_STANDARD 11)
add_executable(library.c)
#https://github.com/shuveb/loti-examples
add_executable(uring_ex.c)

link_libraries(uring)
add_executable(lib_uring_ex.c )
add_executable(lib_uring_poll_kernel_thread_ex.c )
add_executable(webserver_liburing.c )


#
#  the kotlin thing
#
#eval cinterop -def async_c.def -compiler-option\ -I{$PWD,/usr/include{,/x86_64-linux-gnu}}  -o c_async
##
#