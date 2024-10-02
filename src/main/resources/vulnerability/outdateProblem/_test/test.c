#include <stdio.h>

void bzero_bad() {
    char buffer[100];

    // 使用 bzero() 清零内存
    bzero(buffer, sizeof(buffer));

    //buffer[0] = 1;
}

void encrypt3DES_bad() {
  HCRYPTPROV hCryptProv;
  HCRYPTKEY hKey;
  HCRYPTHASH hHash;

  // 不推荐使用 3DES
  CryptDeriveKey(hCryptProv, "CALG_3DES", hHash, 0, &hKey);

  // 推荐
  // CryptDeriveKey(hCryptProv, "CALG_AES_256", hHash, 0, &hKey);
}

void gets_bad() {
    char buffer[100];

    // 使用 gets() 函数读取用户输入
    gets(buffer); // 不推荐使用

    // 使用 fgets() 函数替代
    // fgets(buffer, sizeof(buffer), stdin);
}

void gmtime_bad() {
    time_t rawtime;
    struct tm *timeinfo;

    time(&rawtime);

    // 使用非线程安全的 gmtime() 函数
    timeinfo = gmtime(&rawtime); // 非线程安全

    // 使用线程安全的 gmtime_r() 函数
    // struct tm result;
    // gmtime_r(&rawtime, &result); // 线程安全
}

void memset_bad() {
    char buffer[100];

    // 使用 memset() 设置内存
    memset(buffer, 0, sizeof(buffer));
    // 使用 memset_s() 替代
    // memset_s(buffer,sizeof(buffer),0,sizeof(buffer));
}

void sprintf_bad() {
    char buffer[10];
    int num = 123456; // 超过缓冲区大小的数据

    // 使用 sprintf() 写入数据到 buffer 中
    sprintf(buffer, "%d", num); // 潜在的缓冲区溢出风险

    printf("Buffer: %s\n", buffer);
}

void strcpy_bad() {
    char source[] = "This is a very long string that will cause buffer overflow";
    char destination[20];

    // 使用 strcpy() 进行复制，可能导致缓冲区溢出
    strcpy(destination, source);
    printf("Destination string: %s\n", destination);
}