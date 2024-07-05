# C2Graph

we use Eclipse CDT to build C/C++ code property graph

### Requirements

- JDK 17

- Maven

### Before move on

you need a config.json in project working dir (root dir) like belows(windows format):

- project: the root dir, it may contain multi concrete dirs

- dir: the concrete dir
  - project + dir = the folder you would like to analyse

- includePath: the path of the header file, it may contain multi paths

- debug: in this mode, it does not connect to Neo4j. In other words, it does not save data to db, suitable for debugging

- highPrecision: in this mode, it will parse the file with high precision, it will take more time
  - default is false

```json
{
  "host": "192.168.175.128",
  "port": 7687,
  "includePath": [
    "F:\\DevTools\\Dev-Cpp\\MinGW64\\lib\\gcc\\x86_64-w64-mingw32\\4.9.2\\include",
    "F:\\DevTools\\Dev-Cpp\\MinGW64\\lib\\gcc\\x86_64-w64-mingw32\\4.9.2\\include\\ssp"
  ],
  "project": "G:\\Github\\Research-Classroom\\src\\Code2Graph\\C2Graph\\src\\main\\resources\\project\\",
  "dir": "zip",
  "highPrecision": false
}
```
        
### Some tech details

##### Something about Eclipse CDT

Different with the IDE Eclipse CDT, this is a jar we use to parse C
/C++ file. But for some reason, the data about it is simple, many functions don't work, even the official document is empty, we have to grope in the dark. Fortunately, there is always something helps. 

- [CDT API](https://help.eclipse.org/latest/index.jsp?topic=/org.eclipse.cdt.doc.isv/reference/api/overview-summary.html)
  - it doesn't make too much usage.

- [CDT Visualizer](https://github.com/ricardojlrufino/eclipse-cdt-standalone-astparser)

    - the main idea of parse comes from this.

- [ShiftLeftSecurity CPG build](https://github.com/ShiftLeftSecurity/codepropertygraph)
     
- [Fraunhofer-AISEC CPG build](https://github.com/Fraunhofer-AISEC/cpg)
    - they use CDT to parse C/C++ also, the code style is better than Joern, some ideas comes from here also.

- [Joern CPG build](https://github.com/joernio/joern)
    - Joern is made by the team that come up with the CPG, they use CDT to parse C/C++ also.
    - By the way, Joern has many repos, it is hard to read.
- `GCC`, some ideas come from `gcc -E` to check the result after preprocess

##### General idea

Since a directory may contain numerous files, we utilize regular expressions to replace the user-defined header file with its content into a temporary file named filename.tmp. Additionally, we can develop various scripts to enhance our convenience. Following this, we start parsing filename.tmp.

- one script is below

```cpp
// before transfrom
struct Node_ {
    char* first;
    char* second;
} a, b;

// after transfrom
struct Node_ {
    char* first;
    char* second;
};
struct Node_ a, b;
```

Despite these efforts, challenges persist in cross-file projects. For instance, when a variable is declared in one file but used in another, a data graph edge (dfg edge) must be established between them. To address this issue, we can divide the process into two parts: 

- first, parse each file individually using Neo4j as an intermediary; 
- second, replace the intermediary with the actual data.

##### Cfg Jump (the part of Env)

Conditional Branch:

```
if { 
    ... 
} else if () { 
    ... 
} else { 
    ... 
}
```

- we call `if` and `else if` as siblings as the last stmt in {} share the same `empty` jump address

```
switch( ... ) {
  case .:
    ...
    break;
  case .:
    ...
    break;
  default:
    ...
}
```

- all the break and the last stmt in `default` share the same jump address
- one thing to note, the entrance of `case` may be multiple

loop, btw, we parse the stmt by the order of execution( just like cfg) :

- while is simplest, we have added the condition before we parse the {}.
- for has `iteration` than while, when parse the {}, we haven't added iter yet.
- do-while like `for`, we parse the {}, we haven't added condition yet.
  - one thing to note, `break` is the same as false jump, `continue` is the same as `last-stmt`

there are 4 kinds of env combination also, like belows:

> we use `if` as if/ switch, `for` as while/do-while/for

1. if { ... if {} }
   1. the false jump of 2nd if and the last stmt in 2nd if share the same jump address. If the 2nd if has else-clause, the last stmt in it shares also.
2. if { ... for {} }
   1. the false jump of `for` share the same address of the false of `if` 
3. for { ... if {} }
   1. the false jump of `if` -> the iteration of `for` or the condition of `while/do-while` 
   2. the last stmt in `if` -> the iteration of `for` or the condition of `while/do-while`
4. for { ... for {} }
   1. the false jump of 2nd `for` -> the iteration of 1st `for` or the condition of `while/do-while`
   
As we know, if-like stmt may have many exits, falseList and emptyList. for-like stmt only need to care about the falseList. In order to simulate the actual, we use a stack to hold all stmt like for/if representing is handling now.

We make an operation called `mergeUp`, it means the nested address can be pushed to parent env to handle.

### Run steps

After the part of `Before move on`, just run Main is ok.