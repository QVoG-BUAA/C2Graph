#include "include/test.h"
#include <stdio.h>
#include <stdlib.h>

int main() {
    int n;
    int *arr;

    scanf("%d", &n);
    arr = malloc(sizeof(int) * n);

    int sum = 0;
    for (int i = 0; i < n; i ++) {
        scanf("%d", &arr[i]);
        sum += arr[i];
    }

    printf("%d\n", sum);

    clear(arr);
    return 0;
}