# C2Graph

we use Eclipse CDT to build C code property graph

### Requirements

- JDK 17

- Maven

### Before move on

you need a config.json in project working dir (root dir) like belows(windows format):

- project: the root dir, it may contain multi concrete dirs

- dir: the concrete dir/file
  - project + dir = the folder or file you would like to analyse

- includePath: the path of the header file, it may contain multi paths
  - it does not support relative path

- highPrecision: set to False

```json
{
  "host": "192.168.175.128",
  "port": 7687,
  "includePath": [
        "G:\\C2Graph\\src\\main\\resources\\cxxHeaderLib",
        "G:\\C2Graph\\src\\main\\resources\\cxxHeaderLib\\ssp"
  ],
  "project": "G:\\C2Graph\\src\\main\\resources\\apiMisuse\\",
  "dir": "twiceProblem\\SocketDoubleClose",
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

### Run steps

After the part of `Before move on`, just run Main is ok.