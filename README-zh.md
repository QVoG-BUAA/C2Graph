# C2Graph

我们使用 Eclipse CDT 来构建 C/C++ 的 CPG

### 环境要求

- JDK 17

- Maven

### 环境配置

你需要在项目工作目录（根目录）中创建一个 `config.json` 文件，格式如下（Windows格式）：

- project: 项目的根目录，可能包含多个具体的子目录

- dir: 具体的子目录/文件
  - project + dir = 你想要分析的文件夹/文件

- includePath: 头文件的路径，可能包含多个路径
  - 不支持相对路径

- highPrecision: 设置为 False

```json
{
  "host": "192.168.175.128",
  "port": 7687,
  "includePath": [
        "G:\\C2Graph\\src\\main\\resources\\cxxHeaderLib",
        "G:\\C2Graph\\src\\main\\resources\\cxxHeaderLib\\ssp"
  ],
  "project": "G:\\C2Graph\\src\\main\\resources\\vulnerability\\",
  "dir": "twiceProblem\\SocketDoubleClose",
  "highPrecision": false
}
```
        
### 技术细节

##### Eclipse CDT 相关知识

与 Eclipse CDT IDE 不同，这里我们使用的是一个 jar 文件来解析 C/C++ 文件。但由于某些原因，关于它的数据非常简单，许多功能无法正常工作，甚至官方文档都是空的，因此我们只能自己摸索。幸运的是，总有一些资源可以提供帮助：

- [CDT API](https://help.eclipse.org/latest/index.jsp?topic=/org.eclipse.cdt.doc.isv/reference/api/overview-summary.html)
  - 它的实际用途不多。

- [CDT Visualizer](https://github.com/ricardojlrufino/eclipse-cdt-standalone-astparser)
  - 解析的主要思路来源于此。

- [ShiftLeftSecurity CPG 构建](https://github.com/ShiftLeftSecurity/codepropertygraph)

- [Fraunhofer-AISEC CPG 构建](https://github.com/Fraunhofer-AISEC/cpg)
  - 他们也使用 CDT 来解析 C/C++，代码风格比 Joern 好，一些思路也是从这里来的。

- [Joern CPG 构建](https://github.com/joernio/joern)
  - Joern 是 CPG 的提出团队开发的工具，他们同样使用 CDT 来解析 C/C++。
  - 顺便提一下，Joern 有很多代码库，比较难读。

- GCC，一些想法来自 `gcc -E` 以查看预处理后的结果。

### 运行步骤

在完成 "Before move on" 部分后，直接运行 `Main` 即可。