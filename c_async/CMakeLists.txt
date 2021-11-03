cmake_minimum_required(VERSION 3.20)
project(clion C)

set(CMAKE_C_STANDARD 11)

add_executable(mainthing library.c)
add_executable(uring_demo uring_ex.c)

link_libraries(uring)
add_executable(lib_uring_ex   lib_uring_ex.c )
