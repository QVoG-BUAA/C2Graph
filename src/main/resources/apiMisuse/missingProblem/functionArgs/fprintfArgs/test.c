#include <stdio.h>

int main() {
    FILE *file = fopen("output.txt", "w");
    int num1 = 10, num2 = 20, num3 = 30;
    fprintf(file, "Numbers: %d, %d\n", num1, num2, num3); // 参数过多
    fprintf(file, "\nend.\n")
    fclose(file);
    return 0;
}