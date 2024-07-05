#include <stdio.h>

int main() {
    char buffer[10];
    int num = 123456; // 超过缓冲区大小的数据

    // 使用 sprintf() 写入数据到 buffer 中
    sprintf(buffer, "%d", num); // 潜在的缓冲区溢出风险

    printf("Buffer: %s\n", buffer);

    return 0;
}