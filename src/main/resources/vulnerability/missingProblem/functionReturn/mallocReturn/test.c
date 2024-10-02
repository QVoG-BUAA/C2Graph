#include <stdio.h>
#include <stdlib.h>

int main() {
    int *ptr = (int *)malloc(sizeof(int) * 1000000);

    // 需要检测返回值
//    if (ptr == NULL) {
//        printf("Memory allocation failed\n");
//        return 1;
//    }

    free(ptr);
    return 0;
}