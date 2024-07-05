#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int main() {
    char *str;
    str = (char *)malloc(10 * sizeof(char));
    if (str == NULL) {
        printf("Memory allocation failed\n");
        return 1;
    }

    //strcpy(str, "Hello");

    // 错误：调用 strlen，但内存块未以 null 结尾
    int length = strlen(str);
    printf("Length: %d\n", length);

    free(str);
    return 0;
}