#include <stdio.h>
#include <stdlib.h>

char *do_search(const char *query);

void bad_server() {
    char *query = getenv("QUERY_STRING");
    if (!query) {
        fprintf(stderr, "No query string provided.\n");
        return;
    }

    puts("<p>Query results for ");
    // BAD: 直接输出 HTTP 参数，未进行转义处理
    puts(query);
    puts("\n<p>\n");
    puts(do_search(query));
}

int main() {
    bad_server();
    return 0;
}