#include <stdio.h>
#include <pthread.h>

pthread_mutex_t mutex;

void* thread_function(void* arg) {
    pthread_mutex_lock(&mutex);

    // 在持有锁的情况下再次尝试获得锁
// 错误的做法，可能导致死锁
    pthread_mutex_lock(&mutex);

    // 其他操作...

    // 释放锁
    pthread_mutex_unlock(&mutex);
    return NULL;
}
