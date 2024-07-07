#include <stdio.h>

int main() {
    int arr[5] = {1, 2, 3, 4, 5};
    int *ptr = arr;

    // 错误：偏移量计算不正确
    int *ptr_offset = ptr + sizeof(int);
    int b = sizeof(arr);
    printf("Value at offset: %d\n", *ptr_offset);

    return 0;
}