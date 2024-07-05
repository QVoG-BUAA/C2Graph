#include <iostream>

int main() {
    int *ptr = new int;
    if (ptr != nullptr) {
        delete ptr;

        // 程序的其他逻辑
        int* ptr2 = new int;
        // ptr = new int;

        delete ptr;
        ptr = nullptr;
    }
    return 0;
}