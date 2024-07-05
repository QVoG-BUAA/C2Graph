package org.c2graph.util;

import javafx.util.Pair;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.c2graph.Main;
import org.c2graph.model.CASTNode;
import org.c2graph.model.edge.CASTEdge;
import org.c2graph.model.edge.impl.CfgEdge;
import org.c2graph.model.edge.impl.CgEdge;
import org.c2graph.model.edge.impl.DfgEdge;
import org.c2graph.model.stmt.FunctionExitStmt;
import org.c2graph.model.stmt.Statement;
import org.c2graph.model.stmt.SystemExitStmt;
import org.c2graph.model.stmt.decl.DeclarationStatement;
import org.c2graph.model.stmt.funDef.FunctionDefStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.outV;

public class GremlinUtil {
    public static int cgTotal = 0;
    public static int cgRecTotal = 0;
    public static int nodeCount = 0;
    public static int edgeCount = 0;

    public static ArrayList<String> asList = new ArrayList<>();
    public static ConcurrentHashMap<String, Long> vertexKey2Id = new ConcurrentHashMap<>();

    public static int NODE_BATCH_SIZE = 400;

    public static int EDGE_BATCH_SIZE = 200;

    public static GraphTraversal<Vertex, Object> vertexObjectGraphTraversal = null;
    public static GraphTraversal<Edge, Edge> edgeEdgeGraphTraversal = null;

    public static Boolean isExternNode(CASTNode node) {
        Statement stmt = node.getStmt();
        if (stmt instanceof DeclarationStatement declarationStatement) {
            return declarationStatement.isExtern();
        }
        if (stmt instanceof FunctionDefStatement functionDefStatement) {
            return functionDefStatement.isExtern();
        }
        return false;
    }

    public static ConcurrentLinkedDeque<Pair<CASTNode, CASTEdge>> waitArrayList = new ConcurrentLinkedDeque<>();

    public static void addEdge(CASTNode node, boolean isLast) {
        if (node != null) {
            for (CfgEdge cfgEdge : node.getCfgEdges()) {
                edgeEdgeGraphTraversal = addCfgEdge2Gremlin(node, cfgEdge.toNode, cfgEdge, edgeEdgeGraphTraversal);
            }
            for (DfgEdge dfgEdge : node.getDfgEdges()) {
                edgeEdgeGraphTraversal = addDfgEdge2Gremlin(node, dfgEdge.toNode, dfgEdge, edgeEdgeGraphTraversal);
            }
            for (CgEdge cgEdge : node.getCgEdges()) {
                edgeEdgeGraphTraversal = addCgEdge2Gremlin(node, cgEdge.toNode, cgEdge, edgeEdgeGraphTraversal);
            }
        }
        if (edgeCount >= EDGE_BATCH_SIZE || isLast) {
            if (edgeEdgeGraphTraversal != null) {
                edgeEdgeGraphTraversal.iterate();
            }

            edgeCount = 0;
            edgeEdgeGraphTraversal = null;
//            if (!waitArrayList.isEmpty() && isLast) {
//                System.out.println(waitArrayList.size() + " tasks count in waiting size.");
//                while (!waitArrayList.isEmpty()) {
//                    ArrayList<Pair<CASTNode, CASTEdge>> waitCopy = new ArrayList<>(waitArrayList);
//                    waitArrayList.clear();
//                    for (Pair<CASTNode, CASTEdge> waitingTasks : waitCopy) {
//                        CASTNode key = waitingTasks.getKey();
//                        CASTEdge value = waitingTasks.getValue();
//                        if (value instanceof DfgEdge dfgEdge) {
//                            edgeEdgeGraphTraversal = addDfgEdge2Gremlin(key, value.toNode, dfgEdge, edgeEdgeGraphTraversal);
//                        } else if (value instanceof CfgEdge cfgEdge) {
//                            edgeEdgeGraphTraversal = addCfgEdge2Gremlin(key, value.toNode, cfgEdge, edgeEdgeGraphTraversal);
//                        } else if (value instanceof CgEdge cgEdge) {
//                            edgeEdgeGraphTraversal = addCgEdge2Gremlin(key, value.toNode, cgEdge, edgeEdgeGraphTraversal);
//                        }
//
//                        if (edgeCount >= EDGE_BATCH_SIZE) {
//                            if (edgeEdgeGraphTraversal != null) {
//                                edgeEdgeGraphTraversal.iterate();
//                                edgeEdgeGraphTraversal = null;
//                            }
//                            edgeCount = 0;
//                        }
//                    }
//                }
//                if (edgeEdgeGraphTraversal != null) {
//                    edgeEdgeGraphTraversal.iterate();
//                    edgeEdgeGraphTraversal = null;
//                    edgeCount = 0;
//                }
//            }
        }
    }

    public static boolean isHeaderNode;

    public static boolean addNode(CASTNode node, boolean isLast) {
        if (node != null) {
            IASTNode iastNode = node.getStmt().getIASTNode();
            if (iastNode != null && iastNode.getContainingFilename().endsWith(".h")) {
                isHeaderNode = true;
            }
            // static statement
            if (!isExternNode(node)) {
                if (node.getStmt() instanceof FunctionDefStatement) {
                    vertexObjectGraphTraversal = addFunNode2Gremlin(node, vertexObjectGraphTraversal);
                } else {
                    vertexObjectGraphTraversal = addNode2Gremlin(node, vertexObjectGraphTraversal);
                }
            } else {
                if (!node.getStmt().isDefinition) {
                    vertexObjectGraphTraversal = addDeclExternNode2Gremlin(node, vertexObjectGraphTraversal);
                } else {
                    vertexObjectGraphTraversal = addDeclExternDefNode2Gremlin(node, vertexObjectGraphTraversal);
                }
            }
            isHeaderNode = false;
        }

        if (nodeCount >= NODE_BATCH_SIZE || isLast) {
            if (nodeCount == 1) {
                vertexObjectGraphTraversal.select(asList.get(0));
            } else if (nodeCount == 2) {
                vertexObjectGraphTraversal.select(asList.get(0), asList.get(1));
            } else if (nodeCount > 2) {
                vertexObjectGraphTraversal.select(asList.get(0), asList.get(1),
                        asList.subList(2, asList.size()).toArray(new String[asList.size() - 2]));
            }
            if (vertexObjectGraphTraversal == null) {
                return false;
            }

            List<Object> list = vertexObjectGraphTraversal.toList();
            for (Object object : list) {
                if (object instanceof Long) {
                    vertexKey2Id.put(asList.get(0), ((Long) object));
                } else {
                    LinkedHashMap hashMap = (LinkedHashMap) object;
                    hashMap.forEach((key, value) -> {
                        vertexKey2Id.put(((String) key), (Long) value);
                    });
                }
            }
            nodeCount = 0;
            asList.clear();
            vertexObjectGraphTraversal = null;
            return true;
        }
        return false;
    }

    /**
     * cg-return edge
     * 1. use funExit to find funDef (funDecl first)
     * 2. use funDecl to find funExit (funDef first)
     */
    public static int cgDfg = 0;

    /**
     * 1. function def first, it has known it is called
     * 2. function not def yet, use extern function to find it
     */
    public static void addCgReturnEdge(List<CASTNode> nodes, boolean isLast) {
        if (nodes != null) {
            for (CASTNode node : nodes) {
                if (node.getStmt() instanceof FunctionExitStmt functionExitStmt) {
                    Long fromId = vertexKey2Id.get(String.valueOf(node.getId()));
                    if (fromId == null) {
                        return;
                    }
                    JSONObject jsonObject = node.getStmt().buildJson();
                    String funSig = (String) jsonObject.get("funSig");
                    boolean shouldHaveDfg = !(funSig.startsWith("void"));
                    if (functionExitStmt.getHasFunctionParamPointer()) {
                        shouldHaveDfg = true;
                    }

                    Long functionDefId = vertexKey2Id.get(String.valueOf(((FunctionExitStmt) node.getStmt()).functionDefGlobalId));
                    var callVs = Main.g.V(functionDefId).inE("cg")
                            .project("e", "v")
                            .by().by(outV()).toStream().map(m -> new Pair<>(
                                    (Vertex) m.get("v"), (Edge) m.get("e"))).toList();

                    for (Pair<Vertex, Edge> next : callVs) {
                        Edge value = next.getValue();
                        Object callerId = value.outVertex().id();
                        if (next.getKey() == null) {
                            continue;
                        }

                        edgeCount++;
                        cgRecTotal++;
                        if (edgeEdgeGraphTraversal == null) {
                            edgeEdgeGraphTraversal = Main.g.addE("cg")
                                    .from(__.V(fromId))
                                    .to(next.getKey())
                                    .property("hashCode", callerId);
                        } else {
                            edgeEdgeGraphTraversal = edgeEdgeGraphTraversal.addE("cg")
                                    .from(__.V(fromId))
                                    .to(next.getKey())
                                    .property("hashCode", callerId);
                        }
                        if (shouldHaveDfg) {
                            cgDfg++;
                            edgeEdgeGraphTraversal = edgeEdgeGraphTraversal
                                    .addE("dfg")
                                    .to(__.V(fromId))
                                    .from(next.getKey())
                                    .property("description", "FunctionReturn")
                                    .property("defineOperationLike", "none");
                        }
                    }
                }
                if (node.getStmt() instanceof DeclarationStatement declStmt) {
                    if (!declStmt.isFunDecl()) {
                        return;
                    }
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("_type", "FunExit");
                    String funSig = declStmt.getSignature();
                    jsonObject.put("funSig", funSig);
                    Long exitId = ConcurrentUtil.crossFileExternFunctionQuery(jsonObject.toString());
                    if (exitId == null) {
                        return;
                    }

                    boolean shouldHaveDfg = !(funSig.startsWith("void"));
                    if (declStmt.getIsParamHasPointer()) {
                        shouldHaveDfg = true;
                    }

                    Long declId = vertexKey2Id.get(String.valueOf(node.getId()));

                    var callVs = Main.g.V(declId).inE("cg")
                            .project("e", "v")
                            .by().by(outV()).toStream().map(m -> new Pair<>(
                                    (Vertex) m.get("v"), (Edge) m.get("e"))).iterator();
                    while (callVs.hasNext()) {
                        Pair<Vertex, Edge> next = callVs.next();
                        if (next.getKey() == null) {
                            continue;
                        }
                        Edge value = next.getValue();
                        Object callerId = value.outVertex().id();
                        edgeCount++;
                        cgRecTotal++;
                        if (edgeEdgeGraphTraversal == null) {
                            edgeEdgeGraphTraversal = Main.g.addE("cg")
                                    .from(__.V(exitId))
                                    .to(next.getKey())
                                    .property("hashCode", callerId);
                            ;
                        } else {
                            edgeEdgeGraphTraversal = edgeEdgeGraphTraversal.addE("cg")
                                    .from(__.V(exitId))
                                    .to(next.getKey())
                                    .property("hashCode", callerId);
                        }
                        if (shouldHaveDfg) {
                            dfgCount++;
                            edgeEdgeGraphTraversal.addE("dfg")
                                    .from(next.getKey())
                                    .to(__.V(exitId))
                                    .property("description", "FunctionReturn")
                                    .property("defineOperationLike", "none");
                        }
                    }
                }
            }
        }

        if (edgeCount >= EDGE_BATCH_SIZE || isLast) {
            if (edgeEdgeGraphTraversal != null) {
                edgeEdgeGraphTraversal.iterate();
            }
            edgeCount = 0;
            edgeEdgeGraphTraversal = null;
        }
    }

    public static GraphTraversal<Vertex, Object> addFunNode2Gremlin(CASTNode node, GraphTraversal<Vertex, Object> g) {
        String hash = null;
        if (isHeaderNode) {
            hash = node.getSignature() + node.getStmt().getLineno() + node.getStmt().getBelongFile();
            String hasAdd2DbBuffer = ConcurrentUtil.concurrentMap.get(hash);
            if (hasAdd2DbBuffer != null) {
                return g;
            }
        }
        String value = String.valueOf(node.getId());
        FunctionDefStatement stmt = (FunctionDefStatement) node.getStmt();
        asList.add(value);
        nodeCount++;
        if (g == null) {
            g = Main.g.addV("Code")
                    .property("json", node.getStmt().buildJson().toString())
                    .property("code", node.getSignature())
                    .property("lineno", node.getStmt().getLineno())
                    .property("file", node.getStmt().getBelongFile())
                    .property("functionDefName", stmt.getFunctionName().toString())
                    .id().as(value);
        } else {
            g.addV("Code")
                    .property("json", node.getStmt().buildJson().toString())
                    .property("code", node.getSignature())
                    .property("lineno", node.getStmt().getLineno())
                    .property("file", node.getStmt().getBelongFile())
                    .property("functionDefName", stmt.getFunctionName().toString())
                    .id().as(value);
        }
        if (isHeaderNode) {
            ConcurrentUtil.concurrentMap.put(hash, value);
        }
        return g;
    }

    public static GraphTraversal<Vertex, Object> addDeclExternNode2Gremlin(CASTNode node, GraphTraversal<Vertex, Object> g) {
        String signature = node.getSignature();
        Long id = getIdFromVertexCache(node);
        String value = String.valueOf(node.getId());
        if (id != null) {
//            System.out.println(node.getSignature() + " inserted in " + node.getStmt().getBelongFile());
            return g;
        } else {
            String isInsert = ConcurrentUtil.concurrentMap.get(signature);
            if (isInsert != null) {
//                System.out.println(node.getStmt().getLineno() + " has not inserted");
                return g;
            }
        }
        asList.add(value);
        nodeCount++;
        if (g == null) {
            g = Main.
                    g.addV("Code")
                    .property("code", node.getSignature())
                    .property("isExtern", node.getStmt().isExtern)
                    .id().as(value);
        } else {
            g.addV("Code")
                    .property("code", node.getSignature())
                    .property("isExtern", node.getStmt().isExtern)
                    .id().as(value);
        }
        ConcurrentUtil.crossFileExternFunctionStore(signature, value);
        return g;
    }

    public static GraphTraversal<Vertex, Object> addDeclExternDefNode2Gremlin(CASTNode node, GraphTraversal<Vertex, Object> g) {
        String signature = node.getSignature();
        Long id = getIdFromVertexCache(node);
        String value = String.valueOf(node.getId());
        if (id != null) {
//            System.out.println(node.getStmt().getLineno() + " inserted");
            return g;
        } else {
            String isInsert = ConcurrentUtil.concurrentMap.get(signature);
            if (isInsert != null) {
//                System.out.println(node.getStmt().getLineno() + " has not inserted");
                return g;
            }
        }
        String json = node.getStmt().buildJson().toString();
        asList.add(value);
        nodeCount++;
        if (g == null) {
            g = Main.g.addV("Code")
                    .property("json", json)
                    .property("code", node.getSignature())
                    .property("isExtern", node.getStmt().isExtern)
                    .property("lineno", node.getStmt().getLineno())
                    .property("file", node.getStmt().getBelongFile())
                    .id().as(value);
        } else {
            g.addV("Code")
                    .property("json", json)
                    .property("code", node.getSignature())
                    .property("isExtern", node.getStmt().isExtern)
                    .property("lineno", node.getStmt().getLineno())
                    .property("file", node.getStmt().getBelongFile())
                    .id().as(value);
        }
        ConcurrentUtil.crossFileExternFunctionStore(signature, value);
        return g;
    }

    public static GraphTraversal<Vertex, Object> addNode2Gremlin(CASTNode node, GraphTraversal<Vertex, Object> g) {
        if (node.getStmt() instanceof SystemExitStmt || node.getStmt() instanceof FunctionExitStmt)
            return addUserDefineNode2Gremlin(node, g);
        String hash = null;
        if (isHeaderNode) {
            hash = node.getSignature() + node.getStmt().getLineno() + node.getStmt().getBelongFile();
            String hasAdd2DbBuffer = ConcurrentUtil.concurrentMap.get(hash);
            if (hasAdd2DbBuffer != null) {
                return g;
            }
        }
        String json = node.getStmt().buildJson().toString();
        String value = String.valueOf(node.getId());
        asList.add(value);
        nodeCount++;
        if (g == null) {
            g = Main.g.addV("Code")
                    .property("json", json)
                    .property("code", node.getSignature())
                    .property("lineno", node.getStmt().getLineno())
                    .property("file", node.getStmt().getBelongFile())
                    .id().as(value);
        } else {
            g.addV("Code")
                    .property("json", json)
                    .property("code", node.getSignature())
                    .property("lineno", node.getStmt().getLineno())
                    .property("file", node.getStmt().getBelongFile())
                    .id().as(value);
        }
        if (isHeaderNode) {
            ConcurrentUtil.concurrentMap.put(hash, value);
        }
        return g;
    }

    private static GraphTraversal<Vertex, Object> addUserDefineNode2Gremlin(String json, CASTNode node, GraphTraversal<Vertex, Object> g) {
        if (isHeaderNode) {
            String hasAdd2DbBuffer = ConcurrentUtil.concurrentMap.get(json);
            if (hasAdd2DbBuffer != null) {
                return g;
            }
        }
        String value = String.valueOf(node.getId());
        asList.add(value);
        nodeCount++;
        if (g == null) {
            g = Main.g.addV("Code")
                    .property("json", json)
                    .property("code", "—— function " + node.getStmt().getFunctionDefName() + " exit node ——")
                    .property("lineno", node.getStmt().getLineno())
                    .property("file", node.getStmt().getBelongFile())
                    .id().as(value);
        } else {
            g.addV("Code")
                    .property("json", json)
                    .property("code", "—— function " + node.getStmt().getFunctionDefName() + " exit node ——")
                    .property("lineno", node.getStmt().getLineno())
                    .property("file", node.getStmt().getBelongFile())
                    .id().as(value);
        }
        ConcurrentUtil.crossFileExternFunctionStore(json, value);
        return g;
    }

    private static GraphTraversal<Vertex, Object> addUserDefineNode2Gremlin(CASTNode node, GraphTraversal<Vertex, Object> g) {
        JSONObject jsonObject = node.getStmt().buildJson();
        String json = jsonObject.toString();
        return addUserDefineNode2Gremlin(json, node, g);
    }

    public static GraphTraversal<Edge, Edge> addCfgEdge2Gremlin(CASTNode fromNode, CASTNode toNode, CfgEdge cfgEdge,
                                                                GraphTraversal<Edge, Edge> g) {
        Long fromId = getIdFromVertexCache(fromNode);
        Long toId = getIdFromVertexCache(toNode);
        if (fromId == null || toId == null) {
//            if (fromId == null) {
//                System.out.println("cfg from:" + fromNode.getStmt().getLineno() + " " + toNode.getStmt().getLineno());
//            }
//            if (toId == null) {
//                System.out.println("cfg to:" + toNode.getStmt().getLineno() + " " + fromNode.getStmt().getLineno());
//            }
            waitArrayList.add(new Pair<>(fromNode, cfgEdge));
            return g;
        }
        if (checkEdgeInsert(fromId, toId, cfgEdge.value)) {
            return g;
        }
        edgeCount++;
        if (g == null) {
            g = Main.g
                    .addE("cfg")
                    .from(__.V(toId))
                    .to(__.V(fromId))
                    .property("CFG-Attr", cfgEdge.getValue());
        } else {
            g.addE("cfg")
                    .from(__.V(toId))
                    .to(__.V(fromId))
                    .property("CFG-Attr", cfgEdge.getValue());
        }
        return g;
    }

    public static boolean checkEdgeInsert(Long id1, Long id2, String value) {
        Pair<Pair<Long, Long>, String> pairStringPair = new Pair<>(new Pair<>(id1, id2), value);
        Boolean b = edgeInsertCache.get(pairStringPair);
        if (b != null) {
            return true;
        }
        edgeInsertCache.put(pairStringPair, true);
        return false;
    }

    public static Integer dfgCount = 0;
    public static HashMap<Pair<Pair<Long, Long>, String>, Boolean> edgeInsertCache = new HashMap<>();

    public static GraphTraversal<Edge, Edge> addDfgEdge2Gremlin(CASTNode fromNode, CASTNode toNode, DfgEdge dfgEdge,
                                                                GraphTraversal<Edge, Edge> g) {
        Long fromId = getIdFromVertexCache(fromNode);
        Long toId = getIdFromVertexCache(toNode);
        if (fromId == null || toId == null) {
//            if (fromId == null) {
//                System.out.println("dfg from:" + fromNode.getStmt().getLineno() + " " + toNode.getStmt().getLineno() + " " + dfgEdge.symbol);
//            }
//            if (toId == null) {
//                System.out.println("dfg to:" + toNode.getStmt().getLineno() + " " + fromNode.getStmt().getLineno() + " " + dfgEdge.symbol);
//            }
            waitArrayList.add(new Pair<>(fromNode, dfgEdge));
            return g;
        }
        if (checkEdgeInsert(fromId, toId, dfgEdge.symbol)) {
            return g;
        }
        edgeCount++;
        dfgCount++;
        if (g == null) {
            g = Main.g.addE("dfg")
                    .from(__.V(fromId))
                    .to(__.V(toId))
                    .property("description", dfgEdge.getSymbol())
                    .property("defineOperationLike", dfgEdge.getDefineOperationLike());
        } else {
            g.addE("dfg")
                    .from(__.V(fromId))
                    .to(__.V(toId))
                    .property("description", dfgEdge.getSymbol())
                    .property("defineOperationLike", dfgEdge.getDefineOperationLike());
        }
        return g;
    }

    public static Long getIdFromVertexCache(CASTNode node) {
        Long toId = vertexKey2Id.get(String.valueOf(node.getId()));
        if (toId == null) {
            if (node.getStmt() instanceof FunctionExitStmt || node.getStmt() instanceof SystemExitStmt) {
                toId = ConcurrentUtil.crossFileExternFunctionQuery(node.getStmt().buildJson().toString());
            } else {
                toId = ConcurrentUtil.crossFileExternFunctionQuery(node.getSignature()
                        + node.getStmt().getLineno()
                        + node.getStmt().getBelongFile());
                if (toId == null) {
                    toId = ConcurrentUtil.crossFileExternFunctionQuery(node.getSignature());
                }
            }
        }
        return toId;
    }

    public static GraphTraversal<Edge, Edge> addCgEdge2Gremlin(CASTNode fromNode, CASTNode toNode, CgEdge cgEdge,
                                                               GraphTraversal<Edge, Edge> g) {
        Long fromId = getIdFromVertexCache(fromNode);
        Long toId = getIdFromVertexCache(toNode);
        if (fromId == null || toId == null) {
//            if (fromId == null) {
//                System.out.println("cg from:" + fromNode.getStmt().getLineno() + " " + toNode.getStmt().getLineno());
//            }
//            if (toId == null) {
//                System.out.println("cg to:" + toNode.getStmt().getLineno() + " " + fromNode.getStmt().getLineno());
//            }
            waitArrayList.add(new Pair<>(fromNode, cgEdge));
            return g;
        }
        if (checkEdgeInsert(fromId, toId, cgEdge.getData())) {
            return g;
        }
        edgeCount++;
        if (g == null) {
            g = Main.
                    g.addE("cg")
                    .from(__.V(fromId))
                    .to(__.V(toId))
                    .property("attr", cgEdge.getData())
                    .property("hashCode", fromId);
        } else {
            g.addE("cg")
                    .from(__.V(fromId))
                    .to(__.V(toId))
                    .property("attr", cgEdge.getData())
                    .property("hashCode", fromId);
        }
        return g;
    }

    public static GraphTraversal<Vertex, ? extends Property<Object>> drop;

    public static void nodeCleanUp(List<CASTNode> nodes, boolean isLast) {
        if (nodes != null) {
            for (CASTNode node : nodes) {
                if (node.getStmt().isExtern) {
                    if (node.getStmt().isDefinition) {
                        nodeCount++;
                        if (drop == null) {
                            drop = Main.g.V(getIdFromVertexCache(node))
                                    .property("lineno", node.getStmt().getLineno())
                                    .properties("isExtern").drop();
                        } else {
                            drop.V(getIdFromVertexCache(node))
                                    .property("lineno", node.getStmt().getLineno())
                                    .properties("isExtern").drop();
                        }
                    }
                }
            }
        }
        if (nodeCount >= NODE_BATCH_SIZE || isLast) {
            if (drop != null) {
                drop.iterate();
            }
            nodeCount = 0;
            drop = null;
        }
    }
}
