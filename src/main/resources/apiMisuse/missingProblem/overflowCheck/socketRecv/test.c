#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/socket.h>

#define BUFFER_SIZE 10

int main() {
    int sockfd;
    char buffer[BUFFER_SIZE];

    // 创建套接字
    if ((sockfd = socket(AF_INET, SOCK_STREAM, 0)) < 0) {
        perror("socket creation failed");
        exit(EXIT_FAILURE);
    }

    // 连接到服务器...

    // 接收数据
    int bytes_received = recv(sockfd, buffer, 11, 0);
    if (bytes_received < 0) {
        perror("recv failed");
        exit(EXIT_FAILURE);
    }

    buffer[bytes_received] = '\0'; // 添加字符串结束符
    printf("Received data: %s\n", buffer);

    close(sockfd);

    return 0;
}