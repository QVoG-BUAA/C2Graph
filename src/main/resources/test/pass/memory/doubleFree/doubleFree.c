#include <stdio.h>

int main() {
    int* a = malloc(4);
    int* b = a;
    free(a);
    free(b);
    return 0;
}
// 0