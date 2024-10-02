#include <stdio.h>

int main() {
    char buffer[80];
    char userName[50];

    printf("Enter your name: ");
    scanf("%s", userName);

    // 将字符串格式化到 buffer 中
//    if (true)
     if (buffer)
//     if (userName)
    {
        sprintf(buffer, "Congratulations, %s!", userName);
    }

    // 输出格式化后的字符串
    printf("%s\n", buffer);

    return 0;
}