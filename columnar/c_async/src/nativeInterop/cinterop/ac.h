#include <bits/stat.h>




#define QUEUE_DEPTH 1
#define BLOCK_SZ    1024

typedef struct file_info{
    off_t file_sz;
    struct iovec  iovecs[];      /* Referred by readv/writev */
} ;
