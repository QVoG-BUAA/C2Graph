#include <stdio.h>

int main() {
    FILE *file = fopen("example.txt", "r");
    if (file != NULL) {
        // 进行文件操作

        fclose(file); // 第一次关闭文件

        // 程序的其他逻辑
        // file = fopen("example.txt", "r");

        fclose(file); // 第二次关闭同一个文件指针
    }
    return 0;
}