#include <stdio.h>

int main() {
    char buffer[100];

    // 使用 bzero() 清零内存
    bzero(buffer, sizeof(buffer));

    //buffer[0] = 1;
    return 0;
}