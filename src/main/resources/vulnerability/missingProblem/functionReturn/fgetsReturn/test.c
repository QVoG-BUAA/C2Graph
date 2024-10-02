#include <stdio.h>

int main() {
    char buffer[100];
    printf("Enter a string: ");
    fgets(buffer, 100, stdin); // 没有检查返回值
    printf("You entered: %s\n", buffer); // 可能会输出未初始化的buffer内容
    return 0;
}