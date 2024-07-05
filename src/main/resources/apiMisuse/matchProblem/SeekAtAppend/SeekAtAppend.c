#include <stdio.h>

int main() {
    FILE *file = fopen("example.txt", "a");
    if (file == NULL) {
        perror("Error opening file");
        return 1;
    }

    fseek(file, 0, SEEK_SET); // 尝试将文件指针移动到文件开头

    // 尝试读取文件内容
    char buffer[100];
    fgets(buffer, 100, file);
    printf("Read: %s\n", buffer);

    fclose(file);
    return 0;
}