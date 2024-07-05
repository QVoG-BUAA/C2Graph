#include <stdio.h>
#include <string.h>

int main() {
    char buffer[100];

    // 使用 memset() 设置内存
    memset(buffer, 0, sizeof(buffer));
    // 使用 memset_s() 替代
    // memset_s(buffer,sizeof(buffer),0,sizeof(buffer));

    return 0;
}