#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int main(int argc, char** argv) {
    char *userInput = argv[1];
    char query[256];

    // BAD: User input is directly concatenated into the SQL query string
    sprintf(query, "SELECT * FROM users WHERE username='%s'", userInput);

    // Execute the SQL query...

    return 0;
}