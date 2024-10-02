#include <stdio.h>

int main() {
    FILE *file = fopen("test.txt", "w");
    if (file == NULL) {
        perror("Failed to open file");
        return 1;
    }

    // 从文件中读取数据
    char buffer[100];
    fgets(buffer, sizeof(buffer), file);
    printf("Read from file: %s\n", buffer);

    fclose(file);
    return 0;
}