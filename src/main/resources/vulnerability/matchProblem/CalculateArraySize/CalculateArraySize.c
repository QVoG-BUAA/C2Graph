#include <stdio.h>

const int N = 5;

void printArraySize(int arr[], int n) {
    // 错误：在函数内部使用 sizeof，返回的是指针的大小
    printf("Size of array inside function: %lu\n", sizeof(arr));
    int *p = malloc(sizeof(int) * n);
    free(p);
}