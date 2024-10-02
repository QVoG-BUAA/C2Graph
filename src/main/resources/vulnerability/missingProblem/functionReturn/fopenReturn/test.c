#include <stdio.h>

int main() {
    FILE *file = fopen("example.txt", "r");
//    if (file == NULL) {
//        printf("Failed to open the file.\n");
//        return 1;
//    }

    // 文件打开成功，可以进行读取或写入操作

    fclose(file); // 使用完文件后关闭文件

    return 0;
}
