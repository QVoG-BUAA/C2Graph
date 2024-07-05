#include <stdio.h>
#include <stdlib.h>

// Function that allocates memory but does not free it
void allocate_and_modify(int **p, int size) {
    *p = (int *)malloc(size * sizeof(int));
    if (*p == NULL) {
        fprintf(stderr, "Failed to allocate memory in function.\n");
        return;
    }
    for (int i = 0; i < size; i++) {
        (*p)[i] = i * 2;  // Modify the array
    }
}

int main() {
    int *ptr1 = NULL;
    int *ptr2 = NULL;
    int condition = 1;

    // Allocate and modify in a separate function
    allocate_and_modify(&ptr1, 1000);

    // Conditional allocation
    if (condition) {
        ptr2 = (int *)malloc(sizeof(int) * 500);
        if (ptr2 != NULL) {
            for (int i = 0; i < 500; i++) {
                ptr2[i] = i;
            }
        }
    }

    // Reallocate ptr1 to a larger size
    ptr1 = (int *)realloc(ptr1, sizeof(int) * 2000);
    if (ptr1 == NULL) {
        fprintf(stderr, "Failed to reallocate memory.\n");
        free(ptr2);  // Ensure other allocated memory is freed
        return EXIT_FAILURE;
    }

    // Only free one of the pointers
    // free(ptr1);
    // ptr2 is intentionally not freed here

    return EXIT_SUCCESS;
}