#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <string.h>

#define SERVER_PORT 12345
#define BUFFER_SIZE 1024

int main() {
    int sockfd;
    struct sockaddr_in server_addr;

    // Create a socket
    sockfd = socket(AF_INET, SOCK_STREAM, 0);
//    if (sockfd < 0) {
//        perror("Error in socket creation");
//        exit(EXIT_FAILURE);
//    }
//
//    // Set up the server address structure
//    memset(&server_addr, 0, sizeof(server_addr));
//    server_addr.sin_family = AF_INET;
//    server_addr.sin_port = htons(SERVER_PORT);
//    server_addr.sin_addr.s_addr = INADDR_ANY;
//
//    // Connect to the server
//    if (connect(sockfd, (struct sockaddr *)&server_addr, sizeof(server_addr)) < 0) {
//        perror("Error connecting to server");
//        // Socket is not closed when connection fails
//        exit(EXIT_FAILURE);
//    }
//
    // Send data
    char message[] = "Hello, server!";
    if (send(sockfd, message, strlen(message), 0) < 0) {
        perror("Error sending message");
        // Socket is not closed when sending fails
        exit(EXIT_FAILURE);
    }

    // Receive data
    char buffer[BUFFER_SIZE];
    int bytes_received = recv(sockfd, buffer, BUFFER_SIZE, 0);
    if (bytes_received < 0) {
        perror("Error receiving data");
        // Socket is not closed when receiving fails
        exit(EXIT_FAILURE);
    }

    // Print received data
    buffer[bytes_received] = '\0';  // Null-terminate the string
    printf("Received from server: %s\n", buffer);

    // Simulate more operations and errors
    if (1) {  // Placeholder for some condition that might fail
        fprintf(stderr, "Simulated error after operations\n");
        // Socket not closed on this simulated error path
        exit(EXIT_FAILURE);
    }

    // Normally, we should close the socket here
    // close(sockfd);

    return 0;
}