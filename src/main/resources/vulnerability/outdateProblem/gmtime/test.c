#include <stdio.h>
#include <time.h>

int main() {
    time_t rawtime;
    struct tm *timeinfo;

    time(&rawtime);

    // 使用非线程安全的 gmtime() 函数
    timeinfo = gmtime(&rawtime); // 非线程安全

    // 使用线程安全的 gmtime_r() 函数
    // struct tm result;
    // gmtime_r(&rawtime, &result); // 线程安全

    return 0;
}