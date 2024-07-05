#include <stdio.h>
#include <unistd.h>

char* g_storage;

int main(int argc, char *argv[]) {
    // g_storage = (char *) malloc(1);
	//init not called
	strcpy(g_storage, argv[1]); // g_storage is used before init() is called
}