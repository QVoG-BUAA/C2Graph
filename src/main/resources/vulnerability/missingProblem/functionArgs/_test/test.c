#include <stdio.h>

int fprintf_args() {
    FILE *file = fopen("output.txt", "w");
    int num1 = 10, num2 = 20, num3 = 30;
    fprintf(file, "Numbers: %d, %d\n", num1, num2, num3); // 参数过多
    fprintf(file, "\nend.\n")
    fclose(file);
    return 0;
}

#define FILE_NAME "example.txt"
int open_file_bad() {
   return open(FILE_NAME, O_CREAT);
}

int printf_args() {
    int num = 10;
    printf("%s\n", num); // 错误的参数类型，应该使用 %d 格式指示符

    return 0;
}

void encrypt_with_openssl(EVP_PKEY_CTX *ctx) {

  // 密钥长度过短
  EVP_PKEY_CTX_set_rsa_keygen_bits(ctx, 1024);
}