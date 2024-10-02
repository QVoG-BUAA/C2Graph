#include <stdio.h>

int main() {
    int num1, num2;
    printf("Enter two integers: ");
    int val = scanf("%d %d", &num1, &num2); // 没有检查返回值
    if (num2 > 0) {
        printf("%d", val);
    }
    printf("Sum: %d\n", num1 + num2); // 可能会使用未初始化的变量值
    return 0;
}