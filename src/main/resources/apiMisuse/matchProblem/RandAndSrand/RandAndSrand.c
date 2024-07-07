#include <stdio.h>
#include <stdlib.h>
#include <time.h>

void init() {
//     srand(time(NULL));
}

int main() {
    int i;
    init();
    // 使用当前时间作为种子来初始化随机数生成器
    // srand(time(NULL));

    // 生成随机数
    for (i = 0; i < 10; i++) {
        int random_number = rand();
        printf("%d\n", random_number);
    }

    return 0;
}