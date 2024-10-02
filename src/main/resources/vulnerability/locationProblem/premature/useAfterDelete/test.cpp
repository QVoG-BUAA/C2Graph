#include <cstdio>

int main() {
    int *ptr = new int;

    if (ptr != nullptr) {
        *ptr = 10;
        printf("Value: %d\n", *ptr);

        delete ptr;

        // 尝试使用已经释放的内存
        *ptr = 20; // 这里会导致未定义的行为

        printf("New Value: %d\n", *ptr); // bad
    }

    return 0;
}
