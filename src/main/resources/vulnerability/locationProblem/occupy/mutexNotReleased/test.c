#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>

pthread_mutex_t mutex;
int shared_data = 0;

void* thread_function(void* arg) {
    pthread_mutex_lock(&mutex);

    // Increment shared data
    shared_data++;
    printf("Thread %ld: shared_data = %d\n", (long) arg, shared_data);

    // Simulate doing some work by sleeping
    sleep(1);

    // Introduce a conditional statement that might skip unlocking the mutex
    if (shared_data > 1) {
        printf("Thread %ld: Error condition met, exiting without unlocking mutex.\n", (long) arg);
        pthread_exit(NULL);  // Exit thread without unlocking the mutex
    }

//    pthread_mutex_unlock(&mutex);
    return NULL;
}

int main() {
    pthread_mutex_init(&mutex, NULL);

    const int NUM_THREADS = 2;
    pthread_t threads[NUM_THREADS];

    // Create multiple threads
    for (int i = 0; i < NUM_THREADS; i++) {
        pthread_create(&threads[i], NULL, thread_function, (void*)(long)i);
    }

    // Wait for all threads to complete
    for (int i = 0; i < NUM_THREADS; i++) {
        pthread_join(threads[i], NULL);
    }

    printf("Final value of shared_data: %d\n", shared_data);

    // Attempt to destroy the mutex
    if (pthread_mutex_destroy(&mutex) != 0) {
        fprintf(stderr, "Error destroying mutex. It may still be locked.\n");
        return EXIT_FAILURE;
    }

    return EXIT_SUCCESS;
}