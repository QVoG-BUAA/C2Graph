#include "math_utils.h"

int add(int a, int b) {
    return a + b;
}

int sub(int a, int b) {
    return a - b;
}

static int mul(int a, int b) {
    return a * b;
}

void demoUtils() {
    int a = 20, b = 15;
    mul(a, b);
    printf("Util Addition: %d\n", add(a, b));
    printf("Util Subtraction: %d\n", sub(a, b));
}