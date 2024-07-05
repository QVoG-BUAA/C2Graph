#include <stdio.h>
#include <mysql/mysql.h>

int main()
{
    MYSQL *conn = mysql_init(NULL);
    if (conn == NULL)
        return 1;

    if (mysql_real_connect(conn, "localhost", "user", "password", "database", 0, NULL, 0) == NULL)
    {
        fprintf(stderr, "Error connecting to database: %s\n", mysql_error(conn));
        mysql_close(conn);
        return 1;
    }
    mysql_close(conn);

    printf("Attempting to query database...\n");
    if (mysql_query(conn, "SELECT * FROM table") != 0)
    {
        fprintf(stderr, "Error querying database: %s\n", mysql_error(conn));
        return 1;
    }
    return 0;
}