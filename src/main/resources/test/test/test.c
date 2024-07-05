#include <stdio.h>

struct Node {
    struct Node2 {
        int c;
    } b;
} a;

int main(void)
{
    struct Node2 d;
    a.b = d;
    int i = a.b.c;
//
//    int i = 0;
//    int j = 0;
//    int e[5][5];
//    e[i][j] = 1;
//    int f = e[i][j + 1];
}