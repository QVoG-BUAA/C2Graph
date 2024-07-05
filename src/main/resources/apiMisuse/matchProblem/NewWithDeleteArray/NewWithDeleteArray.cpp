#include <iostream>
using namespace std;

int main()
{
    // 分配一个整型数组内存
    int *arr = new int[10];

    // 其他逻辑代码...
    for (int i = 0; i < 10; ++i)
    {
        arr[i] = i; // 对数组进行赋值
    }

    // 释放整型数组内存
    delete arr;

    return 0;
}