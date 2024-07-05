# taskTodo

1. nametype-Specifier in decl may be functionCall
   1. rand()
   2. decl may contains functionCall
2. cvariable has static/extern function
3. astbinding/comment
```java
this.type = expr.getExpressionType().toString();

// problem binding
Invalid redeclaration of the name
```

# taskToFix

1. the hash in Neo4j is weak, it may be double
   1. decl、funDef 的sign过弱
3. JsonUtil/ float && int overflow
   1. it is using string now 
8. scanf return type
   1. cdt bug
2. `usr/include` problemStmt in server
3. dfg2usage in handleDfg < in handleTask
4. handleCg refactor cause count decrease
5. attr may be a list
```c
// how to record a[0]?
memcpy(a[0], b, 10);
```
6. `cpp` supported is not enough
7. static function cross in juliet CWE401-68+2and21
8. cwe401 合并测试出错，01文件夹正常