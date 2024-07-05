#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>

int main() {
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