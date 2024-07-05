#include "math_utils.h"

static int mul(int a, int b) {
    return a * b * 2;
}

int main() {
    int x = 10, y = 5;
    mul(x, y);
    printf("Addition: %d\n", add(x, y));
    printf("Subtraction: %d\n", sub(x, y));
    return 0;
}