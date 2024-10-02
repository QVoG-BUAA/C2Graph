#include <stdio.h>

int scanf_bad() {
    int num1, num2;
    printf("Enter two integers: ");
    int val = scanf("%d %d", &num1, &num2); // 没有检查返回值
    if (num2 > 0) {
        printf("%d", val);
    }
    printf("Sum: %d\n", num1 + num2); // 可能会使用未初始化的变量值
    return 0;
}

int fgets_bad() {
    char buffer[100];
    printf("Enter a string: ");
    fgets(buffer, 100, stdin); // 没有检查返回值
    printf("You entered: %s\n", buffer); // 可能会输出未初始化的buffer内容
    return 0;
}

int fopen_bad() {
    FILE *file = fopen("example.txt", "r");
//    if (file == NULL) {
//        printf("Failed to open the file.\n");
//        return 1;
//    }

    // 文件打开成功，可以进行读取或写入操作

    fclose(file); // 使用完文件后关闭文件

    return 0;
}

int malloc_bad() {
    int *ptr = (int *)malloc(sizeof(int) * 1000000);

    // 需要检测返回值
//    if (ptr == NULL) {
//        printf("Memory allocation failed\n");
//        return 1;
//    }

    free(ptr);
    return 0;
}

int socket_bad() {
    int sockfd;
    sockfd = socket(AF_INET, SOCK_STREAM, 0);

    // 正确：检查 socket 函数的返回值
    // if (sockfd < 0) {
    //    perror("Error in socket creation");
    //    exit(EXIT_FAILURE);
    // }

    // 连接到服务器
    // ...

    close(sockfd);

    return 0;
}