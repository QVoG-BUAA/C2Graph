#include <stdio.h>

int main() {
    FILE *file;

    // 以 r 模式打开文件
    file = fopen("example.txt", "r");
    if (file == NULL) {
        printf("文件打开失败\n");
        return 1;
    }

    // 错误：以只读模式打开文件，但后续尝试写入
    fprintf(file, "这是一个测试\n");

    fclose(file);
    return 0;
}