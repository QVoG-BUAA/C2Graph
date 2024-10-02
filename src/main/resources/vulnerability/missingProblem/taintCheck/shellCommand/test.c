#include <stdio.h>
#include <stdlib.h>

int main(int argc, char** argv) {
    char cmd[100];
    char* user_input = argv[1];

    // BAD: 用户输入直接传递给 system 函数
    sprintf(cmd, "ls %s", user_input);
    system(cmd);

    return 0;
}