void clear(int* p) {
    if (*p > 0) {
        p = NULL;
        free(p);
        return;
    }
    //free(p);
    p = NULL;
    return;
}