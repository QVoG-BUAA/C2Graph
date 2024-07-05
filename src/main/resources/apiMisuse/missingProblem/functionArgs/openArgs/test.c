#include <fcntl.h>

#define FILE_NAME "example.txt"

int open_file_bad() {
   return open(FILE_NAME, O_CREAT);
}