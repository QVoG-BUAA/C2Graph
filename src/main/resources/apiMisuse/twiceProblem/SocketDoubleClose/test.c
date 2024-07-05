#include <stdio.h>
#include <sys/socket.h>

int main() {
    int sockfd = socket(AF_INET, SOCK_STREAM, 0);
    if (sockfd < 0) {
        perror("Socket creation failed");
        return -1;
    }

    // 关闭套接字
    close(sockfd);

    int sockfd2 = socket(AF_INET, SOCK_STREAM, 0);

    // 再次关闭套接字，这是一个错误的操作
    close(sockfd);

    return 0;
}