#include <stdio.h>

int main() {
    char buffer[100];

    // 使用 gets() 函数读取用户输入
    gets(buffer); // 不推荐使用

    // 使用 fgets() 函数替代
    // fgets(buffer, sizeof(buffer), stdin);

    return 0;
}