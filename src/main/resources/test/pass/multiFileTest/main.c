#include <stdio.h>
#include "include/stack.h"

int main(void)
{
    for (int i = 0; i < 26; i ++) {
        push('a' + i);
    }
	return 0;
}
