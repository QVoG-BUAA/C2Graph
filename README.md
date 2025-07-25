# C2Graph

C2Graph is a tool that leverages Eclipse CDT to construct a Simplified-CPG from C source code. 

## Project Structure

This section outlines the directory structure and the key components of the C2Graph project.

### `lib/`

Contains the Eclipse CDT library JAR file. This dependency is included directly within the project as it is not available on public Maven repositories.

### `src/`

Contains the complete source code for the C2Graph Java project.

#### `src/main/java/`

This directory holds the primary Java source code for the C2Graph application.

*   **`org.c2graph.config`**:
    Manages configuration settings (e.g., loaded from `config.json`) and initializes the Eclipse CDT parser for C files.

*   **`org.c2graph.model`**:
    Defines the core data structures for the C2Graph property graph, including nodes and edges.
    *   **`bindings/`**:
      Provides mechanisms for attaching metadata to graph nodes and edges.
    *   **`concurrent/`**:
        Handles multi-threaded operations, particularly for concurrent graph data uploads to a database.
    *   **`edge/`**:
        Contains definitions for various types of graph edges.
          *   `CASTEdge`: The base class for all C2Graph edges, representing relationships between the node elements.
    *   **`stmt/`**:
        Represents different types of C statements within the graph, designed to abstract and extract information from corresponding AST nodes.
          *   `Statement`: The abstract base class for all statement representations in the graph.
    *   `C2Graph`: The primary class responsible for parsing C files and constructing the C2Graph property graph.
    *   `CASTNode`: Represents a node in the C2Graph, typically corresponding to an element in the C Abstract Syntax Tree (AST), the statement level.
    *   `JsonNode`, `JsonArrayNode`: Utility classes for constructing JSON objects and arrays, used for graph serialization or database uploads.

*   **`org.c2graph.util`**:
    Provides a collection of utility methods to assist in C file parsing, graph construction, and data management.
      *   **`debug/`**:
          Aids in debugging by providing functionalities related to C AST parsing and visualization.
      *   **`env/`**:
          Manages environment contexts for Control Flow Graph (CFG) construction.
            *   `Env`: Abstract base class for CFG environments.
            *   `IfEnv`: Models the CFG environment for `if` statements, aiding control flow determination.
            *   `WhileEnv`: Models the CFG environment for loop-like statements (e.g., `while`), assisting control flow determination.
      *   **`exceptions/`**:
          Contains custom exception classes specific to C2Graph operations.
      *   `ConcurrentUtil`: Facilitates concurrent operations, including data verification during multi-threaded graph uploads.
      *   `ExprUtil`: Assists in processing C expressions and extracting relevant expression information.
      *   `FunctionCallUtil`: Provides utilities for extracting detailed information about function calls.
      *   `GremlinUtil`: Facilitates graph interaction, particularly for uploading graph data to a database using the Gremlin traversal language.
      *   `JsonUtil`: Provides utility methods for constructing JSON representations of graph elements, especially statements.
      *   `MacroUtil`: Manages the handling and expansion of C preprocessor macros.
      *   `NodeUtil`: Assists in managing graph nodes, including their creation and upload processes.
      *   `ParserUtil`: Provides helper methods for parsing C source files using Eclipse CDT.
      *   `StatementUtil`: Offers utilities for processing and managing statement-related data within the graph.

*   `Main`:
    The primary entry point for the C2Graph application, orchestrating the parsing of C files and the subsequent graph construction.

#### `src/main/resources/`

Contains supplementary resources for C2Graph, including official C header files, test samples, and utility scripts (e.g., for data cleanup)

## Requirements

- JDK 17

- Maven, install all dependencies

- Gremin-Server

#### Gremlin-Server Install

The docker now is available, [click here to access.](https://zenodo.org/records/16418079/files/gremlin_image.tar?download=1)

> Neo4j 3.3.9
 
```bash
wget -O - https://debian.neo4j.com/neotechnology.gpg.key | sudo apt-key add -
echo 'deb https://debian.neo4j.com stable legacy' | sudo tee /etc/apt/sources.list.d/neo4j.list
sudo apt-get update
apt list -a neo4j
apt install neo4j=1:3.3.9
```

> Gemlin-server 3.7.0

```bash
wget https://dlcdn.apache.org/tinkerpop/3.7.0/apache-tinkerpop-gremlin-server-3.7.0-bin.zip
unzip apache-tinkerpop-gremlin-server-3.7.0-bin.zip
cd apache-tinkerpop-gremlin-server-3.7.0-bin
bin/gremlin-server.sh install org.apache.tinkerpop neo4j-gremlin 3.7.0 #Installing the Neo4j control plugin for Gremlin.
```

> Gremlin-Server config
>> gremlin-server-neo4j.yaml

```yaml
host: 0.0.0.0
port: 7687
```

> Gremlin-Server config
>> neo4j-empty.properties

```yaml
gremlin.neo4j.directory=/opt/neo4j/data/databases/graph.db #neo4j location, use `neo4j console`
```

> Start Gremlin-Server
>> if there is no error shown in console, the service is ready to use.

```bash
./bin/gremlin-server.sh conf/gremlin-server-neo4j.yaml console
```

#### Before move on

you need a config.json in project working dir (root dir) like belows(windows format), below is the introduction of parameters:

- host, port: the host and port of the Gremlin-Server 

- project: the root dir, it may contain multi concrete dirs

- dir: the concrete dir/file
  - project + dir = the folder or file you would like to analyse

- includePath: the path of the header file, it may contain multi paths
  - it does not support relative path

- highPrecision: set to False, waiting to be implemented

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
        
## Some tech details

##### Something about Eclipse CDT

Different with the IDE Eclipse CDT, this is a jar we use to parse C
/C++ file. But for some reason, the data about it is simple, many functions don't work, even the official document is empty, we have to grope in the dark. Fortunately, there is always something helps. 

- [CDT API](https://help.eclipse.org/latest/index.jsp?topic=/org.eclipse.cdt.doc.isv/reference/api/overview-summary.html)
  - it doesn't make too much usage.

- [CDT Visualizer](https://github.com/ricardojlrufino/eclipse-cdt-standalone-astparser)

    - the main idea of parse comes from this.

- [ShiftLeftSecurity CPG build](https://github.com/ShiftLeftSecurity/codepropertygraph)
     
- [Fraunhofer-AISEC CPG build](https://github.com/Fraunhofer-AISEC/cpg)
    - they use CDT to parse C also, the code style is better than Joern, some ideas comes from here also.

- [Joern CPG build](https://github.com/joernio/joern)
    - Joern is made by the team that come up with the CPG, they use CDT to parse C also.
    - By the way, Joern has many repos, it is hard to read.
- `GCC`, some ideas come from `gcc -E` to check the result after preprocess

## Run steps

After the part of `Before move on`, just run `Main` is ok.

- the console will show if it has running successfully