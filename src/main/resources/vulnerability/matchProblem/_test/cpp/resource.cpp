int malloc_with_delete() {
    int* ptr = (int*)malloc(sizeof(int));

    // 程序的其他逻辑

    delete ptr;

    return 0;
}

int new_with_delete_array()
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

int new_with_free() {
    int* ptr = new int;

    // 程序的其他逻辑

    free(ptr);

    return 0;
}