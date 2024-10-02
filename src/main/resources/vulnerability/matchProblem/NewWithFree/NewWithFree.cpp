#include <cstdlib>

int main() {
    int* ptr = new int;

    // 程序的其他逻辑

    free(ptr);

    return 0;
}