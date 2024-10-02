#include <iostream>

int main() {
    int *ptr = new int[1000];

    // 模拟一些操作
    for (int i = 0; i < 1000; i++) {
        ptr[i] = i;
    }

    // delete[] ptr;
    return 0;
}