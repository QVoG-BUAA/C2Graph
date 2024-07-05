#include <stdio.h>
#include <string.h>

int main() {
    char dest[20] = "Hello, ";
    char src[50];

    printf("Enter a string: ");
    scanf("%s", src); // 用户输入的字符串
    // 应当判断size是否符合
    strcat(dest, src); // 将用户输入的字符串追加到目标字符串的末尾

    printf("Concatenated string: %s\n", dest);

    return 0;
}