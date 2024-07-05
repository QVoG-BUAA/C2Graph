#include <dlfcn.h>

void fun(char ** argv) {

}

int main(int argc, char **argv)
{
    if (argc < 3) {
        fprintf(stderr, "Usage: %s <program> <library>\n", argv[0]);
        return 1;
    }

    char *lib = argv[2];
    // char *lib = argv[2] + 1;
    // once have "definelike", we can not check it.
    void *handle = dlopen(lib, RTLD_LAZY);
    if (!handle) {
        fprintf(stderr, "Failed to open library: %s\n", dlerror());
        return 1;
    }

    // 使用 dlopen 返回的句柄进行其他操作

    dlclose(handle); // 使用完毕后关闭句柄
    return 0;
}