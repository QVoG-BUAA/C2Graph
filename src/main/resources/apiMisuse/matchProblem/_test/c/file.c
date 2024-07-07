int read_from_file_only() {
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

int seek_at_append() {
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

int strlen_after_malloc() {
    char *str;
    str = (char *)malloc(10 * sizeof(char));
    if (str == NULL) {
        printf("Memory allocation failed\n");
        return 1;
    }

    //strcpy(str, "Hello");

    // 错误：调用 strlen，但内存块未以 null 结尾
    int length = strlen(str);
    printf("Length: %d\n", length);

    free(str);
    return 0;
}

int strlen_in_malloc() {
    char *str = "Hello";
    // 错误：分配内存大小不足
    char *new_str = (char *)malloc(strlen(str));
    char *new_str = (char *)malloc(strlen(str) + 1);
    if (new_str == NULL) {
        printf("Memory allocation failed\n");
        return 1;
    }

    strcpy(new_str, str);
    return 0;
}

int write_to_read_only() {
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