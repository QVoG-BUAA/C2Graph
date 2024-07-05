#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int main() {
    char *str = "Hello";
    // 错误：分配内存大小不足
    char *new_str = (char *)malloc(strlen(str));
    char *new_str = (char *)malloc(strlen(str) + 1);
    if (new_str == NULL) {
        printf("Memory allocation failed\n");
        return 1;
    }

    strcpy(new_str, str);
    return 0;
}