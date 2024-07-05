#include <stdio.h>
#include <stdlib.h>

void processFile(FILE *file) {
    // Perform some operations on the file
    char c;
    while ((c = fgetc(file)) != EOF) {
        putchar(c);
    }
}

int main() {
    FILE *file1 = fopen("example1.txt", "r");
    FILE *file2 = fopen("example2.txt", "r");
    if (file1 != NULL) {
        processFile(file1);
        // Forget to close file1
    }

    if (file2 != NULL) {
        processFile(file2);
        // Close file2
        fclose(file2);
    }

    // More code here...

    // Forget to close file1
    return 0;
}