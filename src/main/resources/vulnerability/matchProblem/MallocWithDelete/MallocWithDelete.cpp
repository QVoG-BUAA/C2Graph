#include <cstdlib>

int main() {
    int* ptr = (int*)malloc(sizeof(int));

    // 程序的其他逻辑

    delete ptr;

    return 0;
}