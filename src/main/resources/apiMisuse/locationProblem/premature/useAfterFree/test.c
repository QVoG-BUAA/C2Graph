#include <stdio.h>
#include <stdlib.h>

int main() {
    int *ptr = (int *)malloc(sizeof(int));
    if (ptr != NULL) {
        *ptr = 10;
        printf("Value: %d\n", *ptr);

        free(ptr);

        // 尝试使用已经释放的内存
        *ptr = 20; // 这里会导致未定义的行为

        printf("New Value: %d\n", *ptr); // bad
    }
    return 0;
}