#include <stdio.h>
#include <string.h>

int main() {
    char src[] = "This is a very long string that may cause buffer overflow";
    char dest[10]; // 目标缓冲区太小

    // BAD: 未检查目标缓冲区大小，可能导致缓冲区溢出
    //if (strlen(src) < sizeof(dest))
    {
        memcpy(dest, src, strlen(src));
    }

    printf("Destination buffer: %s\n", dest);

    return 0;
}