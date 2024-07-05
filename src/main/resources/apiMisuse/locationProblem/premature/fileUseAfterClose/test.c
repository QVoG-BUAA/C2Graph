#include <stdio.h>

File *file;

int main() {
    file = fopen("example.txt", "w");
    if (file == NULL) {
        printf("Failed to open the file.\n");
        return 1;
    }

    fprintf(file, "Hello, World!\n");
    fclose(file);

    // 尝试在关闭文件后写入文件，这是错误的做法
    fprintf(file, "This will cause undefined behavior.\n");

    return 0;
}