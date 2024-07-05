#include <stdlib.h>

int main() {
    int *ptr = (int *)malloc(sizeof(int));
    if (ptr != NULL) {
        free(ptr);

        // 程序的其他逻辑
        int* ptr2 = (int *)malloc(sizeof(int));
        // ptr = (int *)malloc(sizeof(int));

        free(ptr);
    }
    return 0;
}