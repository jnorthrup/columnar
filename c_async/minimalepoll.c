

#include <stdio.h>
#include <unistd.h>
#include <sys/epoll.h>
#include <fcntl.h>

int main(int, char **args) {

    int fd = open("/etc/sysctl.conf", O_RDONLY + O_ASYNC | O_NONBLOCK , O_ASYNC | O_NONBLOCK);

    struct epoll_event event;
    event.data.fd = fd;
    event.events = EPOLLIN  ;

    int epoll_fd = epoll_create1(0);
    int errcond = epoll_ctl(epoll_fd, EPOLL_CTL_ADD, event.data.fd, &event);
    if (errcond) {
        fprintf(stderr, "Failed to add file descriptor to epoll_loop - # %d\n",errcond);
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
        fprintf(stderr, "Failed to close epoll_loop file descriptor\n");
        return 1;
    }
    return 0;
}
