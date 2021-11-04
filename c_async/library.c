
#include <stdio.h>
#include <unistd.h>
#include <fcntl.h>
#include <string.h>
#include <malloc.h>
#include <sys/epoll.h>
#include <sys/mman.h>
#include <aio.h>
#include <asm-generic/errno.h>
#include <bits/types/struct_iovec.h>

void open_read();

void open_mmap();

int epoll_popen_loop_cat();

int glibc_aio_read();

int glibc_aio_suspend();

int main(int, char **args) {
 struct iovec v;
 open_read();
    open_mmap();
    epoll_popen_loop_cat();
    glibc_aio_read();
    glibc_aio_suspend();
}

int epoll_popen_loop_cat() {

    FILE *pIoFile = popen("cat /etc/sysctl.conf </dev/null", "r");   //works when stdin is piped, not dev/null tho

    struct epoll_event event;
    event.data.fd = pIoFile->_fileno;
    event.events = EPOLLIN | EPOLLPRI | EPOLLERR;

    int epoll_fd = epoll_create1(0);
    if (epoll_ctl(epoll_fd, EPOLL_CTL_ADD, event.data.fd, &event)) {
        fprintf(stderr, "Failed to add file descriptor to epoll_popen_loop_cat\n");
        close(epoll_fd);
        return 1;
    }

    const int READ_SIZE = 10;
    int running = 1;
    while (running) {
        printf("\nPolling for input...\n");
        const int MAX_EVENTS = 5;
        struct epoll_event events[MAX_EVENTS];
        int event_count = epoll_wait(epoll_fd, events, MAX_EVENTS, 3000);

        printf("%d ready events\n", event_count);
        for (int i = 0; i < event_count; i++) {
            int fd = events[i].data.fd;
            printf("Reading file descriptor '%d' -- ", fd);

            char buf[READ_SIZE + 1];
            ssize_t bytes_read = read(fd, buf, READ_SIZE);
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

int glibc_aio_read() {

    int fd = open("/etc/sysctl.conf", O_RDONLY);
    if (fd < 0)
        perror("open");

    /* Zero out the aiocb structure (recommended)*/
    struct aiocb *my_aiocb = malloc(sizeof(struct aiocb));
    const int BUFSIZE = 40;
    /*Allocate a data buffer for the aiocb request **/
    void *buf = malloc(BUFSIZE + 1);
    my_aiocb->aio_buf = buf;
    if (!my_aiocb->aio_buf)
        perror("malloc");

    /** Initialize the necessary fields in the aiocb **/
    my_aiocb->aio_fildes = fd;
    my_aiocb->aio_nbytes = BUFSIZE;
    my_aiocb->aio_offset = 0;

    int ret = aio_read(my_aiocb);
    if (ret < 0)
        perror("aio_read");

    int lag = 0;
    while (aio_error(my_aiocb) == EINPROGRESS)lag++;
    __ssize_t i = aio_return(my_aiocb);

    printf("spun %d times\n", lag);

    if (i > 0) {
        printf("buf:\n%s\n", (char *) buf);
    } else {
        printf("res code %d\n", ret);
    }

    free(buf);
    free(my_aiocb);
}

int glibc_aio_suspend() {

    int fd = open("/etc/sysctl.conf", O_RDONLY);
    if (fd < 0)
        perror("open");

    /* Zero out the aiocb structure (recommended)*/
    struct aiocb *my_aiocb = malloc(sizeof(struct aiocb));
    const int BUFSIZE = 40;

    /*Allocate a data buffer for the aiocb request **/
    void *buf = malloc(BUFSIZE + 1);

    my_aiocb->aio_buf = buf;

    if (!my_aiocb->aio_buf)
        perror("malloc");

    /** Initialize the necessary fields in the aiocb **/
    my_aiocb->aio_fildes = fd;
    my_aiocb->aio_nbytes = BUFSIZE;
    my_aiocb->aio_offset = 0;

    int ret = aio_read(my_aiocb);
    if (ret < 0)
        perror("aio_read");

    int MAX_LIST = 1;

    struct aioct *cblist[MAX_LIST] ;

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

void open_mmap() {
    int fd = open("/etc/sysctl.conf", O_RDONLY);
    long pagesize = sysconf(_SC_PAGE_SIZE);
    void *pVoid = mmap(NULL, pagesize,
                       PROT_READ,
                       MAP_PRIVATE,
            /** descriptor */ fd,
/**ofsset*/ 0);

    int closed = close(fd);
    void *pVoid1 = malloc(41);//gives us a zero ending
    memcpy(pVoid1, pVoid, 40);
    printf("%s\n", (char *) pVoid1);
    free(pVoid1);
    munmap(pVoid, 0);

}

void open_read() {

    int fd = open("/etc/sysctl.conf", O_RDONLY);
    char *buf = malloc(41);

    int count;
    int disp = 0;
    do {
        count = read(fd, buf, 40);
        if (disp == 0)printf("%s\n", buf);
        disp++;
    } while (count > 0);
    free(buf);
    close(fd);
}
