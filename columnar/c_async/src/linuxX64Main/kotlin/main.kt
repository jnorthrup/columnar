import platform.linux.*
import platform.posix.*
import platform.posix.free


fun main(int, char **args):Int{

    open_read();
    open_mmap();
    epoll_popen_loop_cat();
    glibc_aio_read();
    glibc_aio_suspend();
}

fun epoll_popen_loop_cat():Int{

    FILE *pIoFile = popen("cat /etc/sysctl.conf </dev/null", "r");   //works when stdin is piped, not dev/null tho

    event:epoll_event;
    event.data.fd = pIoFile.pointed._fileno ;
    event.events = EPOLLIN | EPOLLPRI | EPOLLERR;

    epoll_fd:Int = epoll_create1(0);
    if (epoll_ctl(epoll_fd, EPOLL_CTL_ADD, event.data.fd, event.ptr)) {
        fprintf(stderr, "Failed to add file descriptor to epoll_popen_loop_cat\n");
        close(epoll_fd);
        return 1;
    }

    const READ_SIZE:Int = 10;
    running:Int = 1;
    while (running) {
        printf("\nPolling for input...\n");
        const MAX_EVENTS:Int = 5;
        events:epoll_event[MAX_EVENTS];
        event_count:Int = epoll_wait(epoll_fd, events, MAX_EVENTS, 3000);

        printf("%d ready events\n", event_count);
        for (i:Int = 0; i < event_count; i++) {
            fd:Int = events[i].data.fd;
            printf("Reading file descriptor '%d' -- ", fd);

            char buf[READ_SIZE + 1];
            bytes_read:ssize_t = read(fd, buf, READ_SIZE);
            printf("%zd bytes read.\n", bytes_read);
            if (bytes_read < 1) {
                running = 0;
                break;
            }
            buf[bytes_read] = '\0';
            printf("Read '%s'\n", buf);

        }
    }

    if (close(epoll_fd)) {
        fprintf(stderr, "Failed to close epoll_popen_loop_cat file descriptor\n");
        return 1;
    }
    return 0;
};

fun glibc_aio_read():Int{

    fd:Int = open("/etc/sysctl.conf", O_RDONLY);
    if (fd < 0)
        perror("open");

    /* Zero out the aiocb structure (recommended)*/
    my_aiocb:CPointer<aiocb> = malloc(sizeof(b:aioc));
    const BUFSIZE:Int = 40;
    /*Allocate a data buffer for the aiocb request **/
    buf:CPointer<ByteVar>  = malloc(BUFSIZE + 1);
 my_aiocb.pointed.aio_buf  = buf;
    if (! my_aiocb.pointed.aio_buf )
    perror("malloc");

    /** Initialize the necessary fields in the aiocb **/
 my_aiocb.pointed.aio_fildes  = fd;
 my_aiocb.pointed.aio_nbytes  = BUFSIZE;
 my_aiocb.pointed.aio_offset  = 0;

    ret:Int = aio_read(my_aiocb);
    if (ret < 0)
        perror("aio_read");

    lag:Int = 0;
    while (aio_error(my_aiocb) == EINPROGRESS)lag++;
    i:__ssize_t = aio_return(my_aiocb);

    printf("spun %d times\n", lag);

    if (i > 0) {
        printf("buf:\n%s\n", (char *) buf);
    } else {
        printf("res code %d\n", ret);
    }

    free(buf);
    free(my_aiocb);
}

fun glibc_aio_suspend():Int{

    fd:Int = open("/etc/sysctl.conf", O_RDONLY);
    if (fd < 0)
        perror("open");

    /* Zero out the aiocb structure (recommended)*/
    my_aiocb:CPointer<aiocb> = malloc(sizeof(b:aioc));
    const BUFSIZE:Int = 40;

    /*Allocate a data buffer for the aiocb request **/
    buf:CPointer<ByteVar>  = malloc(BUFSIZE + 1);

 my_aiocb.pointed.aio_buf  = buf;

    if (! my_aiocb.pointed.aio_buf )
    perror("malloc");

    /** Initialize the necessary fields in the aiocb **/
 my_aiocb.pointed.aio_fildes  = fd;
 my_aiocb.pointed.aio_nbytes  = BUFSIZE;
 my_aiocb.pointed.aio_offset  = 0;

    ret:Int = aio_read(my_aiocb);
    if (ret < 0)
        perror("aio_read");

    MAX_LIST:Int = 1;

    cblist:CPointer<aioct>[MAX_LIST] ;

    /* Clear the list. */
    bzero((char *) cblist, sizeof(cblist));

    /* Load one or more references into the list */
    cblist[0] = my_aiocb;
    ret = aio_suspend(cblist, MAX_LIST, NULL);
    if (ret != 0) {
        printf("res code %d\n", ret);
    } else {
        printf("unsuspend:\n%s\n", (char *) buf);
    }

    free(buf);
    free(my_aiocb);
}

fun open_mmap():Unit{
    fd:Int = open("/etc/sysctl.conf", O_RDONLY);
    pagesize:Long = sysconf(_SC_PAGE_SIZE);
    pVoid:CPointer<ByteVar>  = mmap(NULL, pagesize,
        PROT_READ,
        MAP_PRIVATE,
        /** descriptor */ fd,
        /**ofsset*/ 0);

    closed:Int = close(fd);
    pVoid1:CPointer<ByteVar>  = malloc(41);//gives us a zero ending
    memcpy(pVoid1, pVoid, 40);
    printf("%s\n", (char *) pVoid1);
    free(pVoid1);
    munmap(pVoid, 0);

}

fun open_read():Unit{

    val fd:Int = open("/etc/sysctl.conf", O_RDONLY);
    buf:CPointer<ByteVar> = malloc(41);

    count:Int;
    disp:Int = 0;
    do {
        count = read(fd, buf, 40);
        if (disp == 0)printf("%s\n", buf);
        disp++;
    } while (count > 0);
    free(buf);
    close(fd);
}
