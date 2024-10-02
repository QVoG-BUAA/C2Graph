#include <stdio.h>
#include <string.h>

int main() {
    char source[] = "This is a very long string that will cause buffer overflow";
    char destination[20];

    // 使用 strcpy() 进行复制，可能导致缓冲区溢出
    strcpy(destination, source);
    printf("Destination string: %s\n", destination);
    return 0;
}