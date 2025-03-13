package org.c2graph.model;

import javafx.util.Pair;
import org.c2graph.config.ProjectConfig;
import org.c2graph.model.binding.AstBinding;
import org.c2graph.model.concurrent.ConcurrentTask;
import org.c2graph.model.edge.impl.CfgEdge;
import org.c2graph.model.edge.impl.CgEdge;
import org.c2graph.model.stmt.*;
import org.c2graph.model.stmt.decl.DeclarationStatement;
import org.c2graph.model.stmt.funDef.FunctionDefStatement;
import org.c2graph.model.stmt.loop.*;
import org.c2graph.util.*;
import org.c2graph.util.debug.AnsiColors;
import org.c2graph.util.debug.DebugUtil;
import org.c2graph.util.env.Env;
import org.c2graph.util.env.IfEnv;
import org.c2graph.util.env.WhileEnv;
import org.c2graph.util.exceptions.IllegalException;
import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeleteExpression;
import org.eclipse.cdt.core.dom.ast.gnu.IGNUASTCompoundStatementExpression;
import org.eclipse.cdt.internal.core.dom.parser.c.*;
import org.eclipse.cdt.internal.core.dom.parser.cpp.*;
import org.eclipse.osgi.internal.debug.Debug;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.c2graph.model.edge.impl.CfgEdge.*;
import static org.c2graph.util.ExprUtil.checkIfAssignLeftV;
import static org.c2graph.util.NodeUtil.*;
import static org.c2graph.util.StatementUtil.checkEnvLikeStmt;

/**
 * We can model a file to some parts:
 * 1. some global variable declarations
 * 2. some functions
 * We parse the variable first, and then parse the functions from
 * the top to bottom by line number.
 * in other words, when parse a function, it follows the order of cfg
 */
public class C2Graph {
    private final String filePath;
    private final IASTTranslationUnit translationUnit;

    /**
     * some vars help determine where we are
     * 1. declarations is the global decls
     * 2. nodeArrayList is the array that collects stmt that will
     * be saved to db
     */
    private int currentFunId = -1;
    private String currentFunName;
    private final IASTNode[] declarations;
    private final CopyOnWriteArrayList<CASTNode> nodeArrayList;

    /**
     * if/ loop handling containers
     */
    private Stack<Env> envStack;

    /**
     * some edge task handling containers
     * 1. iAST2cAST map the CDT ast node to the idx of nodeArrayList
     * 2. cgTask, as a stmt may have multiple function calls, it needs an array
     * Also, we need to record the callee idx in array and callee name.
     * Since it may be a cross-file project, all function that cross file needs a header,
     * but in some situations, without header is absolutely ok. FIXME
     * 3. cfgTask
     * from the idx of nodeArray to an Array
     * it means the subsequence of a node many be multi, like if
     * 4. ddgTask, like cgTask, it is used to add the dfg from
     * form parameter -> real parameter in function
     */
    private final Map<IASTNode, Integer> iAST2cAST;
    private final Map<IASTNode, ArrayList<Pair<IBinding, String>>> cgTask;
    private final Map<IBinding, ArrayList<Pair<IASTNode, String>>> invCgTask;

    private final Map<Integer, ArrayList<Pair<Integer, String>>> cfgTask;
    private final Map<IASTNode, ArrayList<Pair<IBinding, String>>> dfgTask;
    private final Map<IBinding, ArrayList<Pair<IASTNode, String>>> invDfgTask;

    private final Map<IBinding, Integer> labelRecord;
    private final Map<IBinding, ArrayList<Integer>> gotoRecord;
    private final ProjectConfig config;

    public C2Graph(IASTTranslationUnit translationUnit, String filePath) {
        this.config = ProjectConfig.loadConfig();
        this.filePath = filePath;

        this.translationUnit = translationUnit;
        this.declarations = translationUnit.getDeclarations();

        this.iAST2cAST = new ConcurrentHashMap<>();
        this.nodeArrayList = new CopyOnWriteArrayList<>();

        this.envStack = new Stack<>();

        this.cfgTask = new HashMap<>();
        this.cgTask = new HashMap<>();
        this.invCgTask = new HashMap<>();
        this.dfgTask = new HashMap<>();
        this.invDfgTask = new HashMap<>();

        this.labelRecord = new HashMap<>();
        this.gotoRecord = new HashMap<>();
    }

    /**
     * the start of the first traversal, the processing sequence is:
     * 1. global decl
     * 2. functions
     */
    public void build() throws InterruptedException {
//        Thread dfgThread = new Thread(new DfgConsumer());
        Thread nodeAddThread = new Thread(new DbNodeConsumer());
        Thread edgeAddThread = new Thread(new DbEdgeConsumer());

//        dfgThread.start();
        nodeAddThread.start();
        edgeAddThread.start();

        for (IASTNode declaration : declarations) {
            if (declaration instanceof IASTFunctionDefinition functionDefinition) {
                IBinding iBinding = functionDefinition.getDeclarator().getName().resolveBinding();
                bindingDefCache.put(iBinding, declaration);
            }
        }

        for (IASTNode declaration : declarations) {
            DebugUtil.printNodeInfoEveryThousand(declaration);
            if (declaration instanceof IASTSimpleDeclaration simpleDeclaration) {
                handleDeclStmt(simpleDeclaration);
            } else if (declaration instanceof IASTFunctionDefinition) {
                this.buildAST(List.of(declaration).toArray(new IASTNode[1]));
            } else if (declaration instanceof CPPASTLinkageSpecification linkageSpecification) {
                handleLinkageSpecification(linkageSpecification);
            } else if (!(declaration instanceof IASTProblemDeclaration)) {
                System.out.println(AnsiColors.ANSI_RED + declaration + " :(decl global scope) has not implement yet. ");
                DebugUtil.printNodeInfo(declaration, "not implemented yet" + AnsiColors.ANSI_RESET);
            }
        }
        System.out.println(filePath + " buildAst success at " + DebugUtil.getCurrentTime());

        ConcurrentTask concurrentTask = new ConcurrentTask(0, nodeArrayList.size() - 1);
        concurrentTask.setIsSystemFinish(true);
//        dfgBuffer.add(concurrentTask);
        dbNodeBuffer.add(concurrentTask);

//        dfgThread.join();
        nodeAddThread.join();
        edgeAddThread.join();

        ExprUtil.selfCache.clear();
        StatementUtil.callCache.clear();
        StatementUtil.stmtCache.clear();
    }

    private void handleLinkageSpecification(CPPASTLinkageSpecification linkageSpecification) {
        for (IASTDeclaration decl : linkageSpecification.getDeclarations()) {
            if (decl instanceof IASTSimpleDeclaration simpleDeclaration) {
                handleDeclStmt(simpleDeclaration);
            } else if (decl instanceof IASTFunctionDefinition funDef) {
                this.buildAST(List.of(funDef).toArray(new IASTNode[1]));
            } else if (decl instanceof CPPASTLinkageSpecification linkage) {
                handleLinkageSpecification(linkage);
            } else if (!(decl instanceof IASTProblemDeclaration)) {
                System.out.println(AnsiColors.ANSI_RED + decl + " :(decl global scope) has not implement yet. ");
                DebugUtil.printNodeInfo(decl, "not implemented yet" + AnsiColors.ANSI_RESET);
            }
        }
    }

    public void edgeCleaUp() {
        for (CASTNode castNode : nodeArrayList) {
            GremlinUtil.addEdge(castNode, false);
        }
        GremlinUtil.addEdge(null, true);
    }

    /**
     * the second traversal to clean up
     * 1. add the cg-return edge, because all nodes are in db now
     * 2. eliminate the copy-paste line change problem
     */
    public void neo4jCleanUp() {
//        int dfgsum = 0;
//        int cgsum = 0;
//        int cfgsum = 0;
        ArrayList<CASTNode> cgReturnList = new ArrayList<>();
        ArrayList<CASTNode> nodeCleanList = new ArrayList<>();
        for (CASTNode castNode : nodeArrayList) {
//            dfgsum += castNode.getDfgEdges().size();
//            cfgsum += castNode.getCfgEdges().size();
//            cgsum += castNode.getCgEdges().size();
            if (castNode.getStmt() instanceof FunctionExitStmt
                    || (castNode.getStmt() instanceof DeclarationStatement decl && decl.isFunDecl())) {
                cgReturnList.add(castNode);
            } else if (castNode.getStmt().isExtern && castNode.getStmt().isDefinition) {
                nodeCleanList.add(castNode);
            }

            GremlinUtil.addEdge(castNode, false);
        }
        GremlinUtil.addEdge(null, true);
//        GremlinUtil.cgTotal += cgsum;

        GremlinUtil.addCgReturnEdge(cgReturnList, false);
        GremlinUtil.nodeCleanUp(nodeCleanList, false);

//        System.out.println("dfg siz(without cg-return dfg): " + dfgsum);
//        System.out.println("dfg siz(with cg-return dfg): " + GremlinUtil.cgDfg);
//        System.out.println("dfg siz: " + (dfgsum + GremlinUtil.cgDfg));
//        System.out.println("cfg siz: " + cfgsum);
//        System.out.println("cg siz: " + cgsum * 2);
//        System.out.println("v siz: " + nodeArrayList.size());

//        GremlinUtil.nodeCount = 0;
//        GremlinUtil.edgeCount = 0;

//        GremlinUtil.cgDfg = 0;
//        GremlinUtil.dfgCount = 0;
    }

    /**
     * build ast recursively, when the {} appears, the fun is called.
     * we would take care of the last stmt in {} here for cfg jump.
     */
    private void buildAST(IASTNode[] nodes) {
        for (int i = 0; i < nodes.length; i++) {
            IASTNode node = nodes[i];
            DebugUtil.printNodeInfoEveryThousand(node);

            // it many has many {} end at the same time, like:
            // for { ... if {} }
            while (i == nodes.length - 1 && !(nodes[i] instanceof IASTCompoundStatement) && !envStack.isEmpty()) {
                Env lastEnv = envStack.pop();
                if (lastEnv.isFinished) {
                    addCfgTaskTodo(lastEnv);
                    continue;
                }
                // just like the eg, the `if` and `if` share the cfg address. eg:
                // if { ... if {}}
                // but something different in `for { .. if {}}`, so `childMergeUp` is a broad concept
                if (checkEnvLikeStmt(node)) {
                    lastEnv.childMergeUp = true;
                }

                // if { ... `not if/for/break/continue/return` }, share address
                if (lastEnv instanceof IfEnv peek) {
                    if (!checkEnvLikeStmt(node) && !shouldNotAddCfgEmptyEdge(node)) {
                        peek.getIfEmptyList().add(nodeArrayList.size());
                    }
                    envStack.push(peek);
                } else if (lastEnv instanceof WhileEnv peek) {
                    // for { ... `not if/for/break/continue/return` }, mark the last stmt in loop
                    // like IfEmptyList
                    if (!checkEnvLikeStmt(node) && !shouldNotAddCfgEmptyEdge(node)) {
                        peek.setLastStmtIdx(nodeArrayList.size());
                    }
                    envStack.push(peek);
                }
                break;
            }

            try {

                if (node instanceof IASTDeclarationStatement) {
                    handleDeclStmt(node);
                } else if (node instanceof IASTIfStatement) {
                    handleIfStmt(node);
                } else if (node instanceof IASTFunctionDefinition) {
                    handleFunDefStmt(node);
                } else if (node instanceof IASTReturnStatement) {
                    handleReturnStmt(node);
                } else if (node instanceof IASTExpressionStatement) {
                    handleExprStmt(node);
                } else if (node instanceof IASTWhileStatement) {
                    handleWhileStmt(node);
                } else if (node instanceof IASTBreakStatement) {
                    handleBreakStmt(node);
                } else if (node instanceof IASTContinueStatement) {
                    handleContinueStmt(node);
                } else if (node instanceof IASTForStatement) {
                    handleForStmt(node);
                } else if (node instanceof IASTDoStatement) {
                    handleDoStmt(node);
                } else if (node instanceof IASTSwitchStatement) {
                    handleSwitchStmt(node);
                } else if (node instanceof IASTCaseStatement) {
                    handleCaseStmt(node);
                } else if (node instanceof IASTDefaultStatement) {
                    handleDefaultStmt(node);
                } else if (node instanceof IASTNullStatement) {
                    handleNullStmt(node);
                    // nothing needs to do
                } else if (node instanceof IASTCompoundStatement) {
                    handleCompoundStmt(node);
                } else if (node instanceof IASTGotoStatement gotoStatement) {
                    handleGotoStmt(gotoStatement);
                } else if (node instanceof IASTLabelStatement labelStatement) {
                    handleLabelStmt(labelStatement);
                } else if (node instanceof IASTProblemStatement) {
//                System.out.println("problemStmt: \t" + node.getContainingFilename() + ":" + node.getFileLocation().getStartingLineNumber());
                } else {
                    System.out.println(AnsiColors.ANSI_RED + node + " :the stmt is todo.");
                }
            } catch (Exception e) {

            }
        }
    }

    private ArrayList<IASTName> getAstReferences(IASTName name) {
        IBinding iBinding = name.resolveBinding();
        ArrayList<IASTName> names = bindingReferenceCache.get(iBinding);
        if (names == null) {
            IASTName[] references = translationUnit.getReferences(iBinding);
            names = new ArrayList<>(Arrays.asList(references));
            bindingReferenceCache.put(iBinding, names);
        }
        return names;
    }

//    private ArrayList<IASTName> getAstDeclarations(IASTName name) {
//        IBinding iBinding = name.resolveBinding();
//        ArrayList<IASTName> names = bindingDeclCache.get(iBinding);
//        if (names == null) {
//            IASTName[] references = translationUnit.getDeclarationsInAST(iBinding);
//            names = new ArrayList<>(Arrays.asList(references));
//            bindingDeclCache.put(iBinding, names);
//        }
//        return names;
//    }

    private CASTNode build4CastNode(IASTNode node, Statement stmt) {
        CASTNode cur = new CASTNode(stmt);

        cur.setFunctionId(currentFunId);
        nodeArrayList.add(cur);
        int siz = nodeArrayList.size() - 1;
        iAST2cAST.put(node, siz);

        ArrayList<AstBinding> astBindings = ExprUtil.checkIfSelfOperation(node);
        if (!astBindings.isEmpty()) {
            for (AstBinding astBinding : astBindings) {
                if (astBinding.getIsLiteral()) {
                    continue;
                }
                cur.addDfgEdge(astBinding.toString(), cur);
            }
        }
        // in case of concurrent
        cur.getStmt().buildJson();

        return cur;
    }

    private void handleNullStmt(IASTNode node) {
        NullStatement nullStatement = new NullStatement(node);
        CASTNode cur = build4CastNode(node, nullStatement);
        handleCfgTask(cur);
    }

    private void handleLabelStmt(IASTLabelStatement labelStatement) {
        LabelStatement labelStmt = new LabelStatement(labelStatement);
        CASTNode cur = build4CastNode(labelStatement, labelStmt);
        handleCfgTask(cur);

        IBinding iBinding = labelStatement.getName().resolveBinding();
        ArrayList<Integer> integers = gotoRecord.get(iBinding);
        if (integers != null) {
            for (Integer idx : integers) {
                nodeArrayList.get(idx).addCfgEdge(EMPTY, cur);
            }
            gotoRecord.put(iBinding, null);
        }
        labelRecord.put(iBinding, nodeArrayList.size() - 1);

        this.buildAST(Collections.singletonList(labelStatement.getNestedStatement()).toArray(new IASTNode[1]));
    }

    private void handleGotoStmt(IASTGotoStatement gotoStatement) {
        GotoStatement gotoStmt = new GotoStatement(gotoStatement);
        CASTNode cur = build4CastNode(gotoStatement, gotoStmt);
        handleCfgTask(cur);

        IBinding iBinding = gotoStatement.getName().resolveBinding();
        Integer labelId = labelRecord.get(iBinding);
        if (labelId != null) {
            cur.addCfgEdge(EMPTY, nodeArrayList.get(labelId));
        } else {
            ArrayList<Integer> integers = gotoRecord.get(iBinding);
            if (integers == null) {
                integers = new ArrayList<>();
            }
            integers.add(nodeArrayList.size() - 1);
            gotoRecord.put(iBinding, integers);
        }
    }

    /**
     * local var decl, compare with global var, it needs cfg
     * var -> cfg
     * var -> dfg
     * var <- dfg
     * var -> cg
     */
    private void handleDeclStmt(IASTNode node) {
        IASTDeclarationStatement decl = (IASTDeclarationStatement) node;
        DeclarationStatement declarationStatement;
        try {
            declarationStatement = new DeclarationStatement(decl);
        } catch (Exception e) {
            return;
        }

        IASTDeclaration declaration = decl.getDeclaration();
        CASTNode cur = build4CastNode(declaration, declarationStatement);

        handleTask(declaration, cur);
    }

    /**
     * global declaration is
     * 1. function decl
     * 2. var decl, it may have definition
     * edges:
     * they do not need cfg edge
     * var <- dfg
     * var -> dfg
     * var -> cg
     * fun <- cg
     * fun <- dfg, parameters
     * the return edge of cg is added in cleanup-operation
     */
    private void handleDeclStmt(IASTSimpleDeclaration declaration) {
        DeclarationStatement declarationStatement = new DeclarationStatement(declaration, true);

        if (declarationStatement.isFunDecl()) {
            IASTName functionName = declarationStatement.getDeclVarName().get(0);
            declarationStatement.setFunctionName(functionName);
            try {
                IASTNode node = bindingDefCache.get(functionName.resolveBinding());
                if (node != null) {
                    return;
                }
                addCgAndDfgTaskToDo(functionName, declarationStatement.isHasArgs());
            } catch (Exception ignored) {
                failedCount++;
                return;
            }

            bindingDefCache.put(functionName.resolveBinding(), declaration);
            build4CastNode(declaration, declarationStatement);
        } else if (!declarationStatement.getTypeDeclAstName().isEmpty()) {
            CASTNode cur = build4CastNode(declaration, declarationStatement);

            handleCgTask(declaration, cur);
        } else {
            CASTNode cur = build4CastNode(declaration, declarationStatement);

            handleCgTask(declaration, cur);
            handleMultiLineMacroTask(cur);
        }
//        handleDfgTask(declaration, nodeArrayList.get(nodeArrayList.size() - 1));
        ConcurrentTask concurrentTask = new ConcurrentTask(nodeArrayList.size() - 1, nodeArrayList.size() - 1);
        concurrentTask.setIsGlobalDecl(true);
//        dfgBuffer.add(concurrentTask);
        dbNodeBuffer.add(concurrentTask);
    }

    private void handleIfStmt(IASTNode node) {
        IASTIfStatement ifStatement = (IASTIfStatement) node;
        IASTExpression conditionExpression = ifStatement.getConditionExpression();
        IfStatement ifStmt = new IfStatement(conditionExpression);

        // FIXME it is weird
        if (conditionExpression == null) {
            DebugUtil.printNodeInfo(node, "if cond is null");
            return;
        }
        CASTNode cur = build4CastNode(conditionExpression, ifStmt);
        Integer idx = nodeArrayList.size() - 1;

        handleTask(conditionExpression, cur);

        IfEnv ifEnv = new IfEnv();
        if (!envStack.isEmpty()) {
            if (envStack.peek().childMergeUp || envStack.peek().siblingMergeUp) {
                ifEnv.shouldEmptyMergeUp = true;
                if (envStack.peek().childMergeUp) {
                    envStack.peek().childMergeUp = false;
                }
            }
        }

        ifEnv.setAddTrueList(true);
        ifEnv.getTrueList().add(idx);
        envStack.push(ifEnv);

        IASTStatement thenClause = ifStatement.getThenClause();
        if (thenClause != null) {
            if (thenClause instanceof IASTCompoundStatement) {
                handleCompoundStmt(thenClause);
            } else {
                handleSingleCompoundStmt(thenClause);
            }
        }

        IASTStatement elseClause = ifStatement.getElseClause();
        if (elseClause != null) {
            ifEnv.setAddFalseList(true);
            ifEnv.getFalseList().add(idx);

            // it means the current if-else is the last stmt of some envs.
            if (ifEnv.shouldEmptyMergeUp) {
                ifEnv.siblingMergeUp = true;
            }

            if (elseClause instanceof IASTCompoundStatement) {
                handleCompoundStmt(elseClause);
            } else if (elseClause instanceof IASTIfStatement) {
                handleIfStmt(elseClause);
            } else {
                handleSingleCompoundStmt(elseClause);
            }
        } else {
            // work with isFinished, just as mergeUp
            ifEnv.mergeUpFalseList.add(idx);
        }

        handleMergeUp();
        ifEnv.isFinished = true;
    }

    // handle the stmts in {}
    private void handleCompoundStmt(IASTNode node) {
        IASTCompoundStatement clause = (IASTCompoundStatement) node;
        CompoundStatement compoundStatement = new CompoundStatement(clause);
        IASTStatement[] statements = clause.getStatements();

        if (statements.length == 0) {
            CASTNode castNode = build4CastNode(node, compoundStatement);
            handleTask(node, castNode);
            if (!envStack.isEmpty()) {
                if (envStack.peek() instanceof IfEnv ifEnv) {
                    ifEnv.mergeUpEmptyList.add(nodeArrayList.size() - 1);
                } else {
                    WhileEnv peek = (WhileEnv) envStack.peek();
                    peek.setLastStmtIdx(nodeArrayList.size() - 1);
                    peek.getFalseList().clear();
                }
            }
        } else {
            buildAST(statements);
        }
    }

    private void handleSingleCompoundStmt(IASTNode node) {
        ArrayList<IASTNode> nodesArrayList = new ArrayList<>();
        nodesArrayList.add(node);
        IASTNode[] nodesArray = new IASTNode[1];
        buildAST(nodesArrayList.toArray(nodesArray));
    }

    private void handleFunDefStmt(IASTNode node) {
        envStack.clear();
        gotoRecord.clear();
        labelRecord.clear();

        IASTFunctionDefinition functionDefinition = (IASTFunctionDefinition) node;
        FunctionDefStatement funStmt = new FunctionDefStatement(functionDefinition);

        CASTNode cur = build4CastNode(functionDefinition, funStmt);

        ArrayList<Pair<IASTNode, String>> pairs = invCgTask.get(funStmt.getFunctionName().resolveBinding());
        if (pairs != null) {
            for (Pair<IASTNode, String> pair : pairs) {
                IASTNode key = pair.getKey();
                Integer i = iAST2cAST.get(key);
                nodeArrayList.get(i).addCgEdge(pair.getValue(), cur);
            }
        }
        pairs = invDfgTask.get(funStmt.getFunctionName().resolveBinding());
        if (pairs != null) {
            for (Pair<IASTNode, String> pair : pairs) {
                IASTNode key = pair.getKey();
                Integer i = iAST2cAST.get(key);
                nodeArrayList.get(i).addDfgEdge(pair.getValue(), cur);
            }
        }

        currentFunId = nodeArrayList.size() - 1;
        cur.setFunctionId(currentFunId);
        currentFunName = funStmt.getFunctionName().toString();

        addCgAndDfgTaskToDo(funStmt.getFunctionName(), !funStmt.getParamsName().isEmpty());

        // nodes
        IASTStatement body = functionDefinition.getBody();
        if (body != null) {
            handleCompoundStmt(body);
            handleUserDefineExitStmt(cur.getFunctionId());
        }

        ConcurrentTask concurrentTask = new ConcurrentTask(currentFunId, nodeArrayList.size() - 1, true);
//        dfgBuffer.add(concurrentTask);
        dbNodeBuffer.add(concurrentTask);

        currentFunId = -1;
        currentFunName = null;
    }

    private void handleReturnStmt(IASTNode node) {
        IASTReturnStatement stmt = (IASTReturnStatement) node;
        ReturnStatement returnStmt = new ReturnStatement(stmt);

        CASTNode cur = build4CastNode(stmt, returnStmt);

        handleTask(stmt, cur);

        CASTNode funNode = nodeArrayList.get(currentFunId);
        FunctionDefStatement funNodeStmt = (FunctionDefStatement) funNode.getStmt();

        funNodeStmt.getReturnIds().add(nodeArrayList.size() - 1);
    }

    /**
     * SystemExit: the latter node of return in function Main
     * FunctionExit: the latter node of return in function except Main
     */
    private void handleUserDefineExitStmt(int functionDefId) {
        CASTNode funDef = nodeArrayList.get(currentFunId);
        CASTNode exitNode;
        if ("main".equals(currentFunName)) {
            SystemExitStmt systemExitStmt = new SystemExitStmt();
            systemExitStmt.functionDefId = functionDefId;
            systemExitStmt.functionDefGlobalId = nodeArrayList.get(functionDefId).getId();
            systemExitStmt.setFunctionDefName(currentFunName);
            systemExitStmt.setBelongFile(funDef.getStmt().getBelongFile());

            exitNode = new CASTNode(systemExitStmt);
            exitNode.getStmt().setLineno(nodeArrayList.get(nodeArrayList.size() - 1).getStmt().getLineno() + 1);
            nodeArrayList.add(exitNode);
            handleCfgTask(exitNode);
        } else {
            FunctionExitStmt functionExitStmt = new FunctionExitStmt();
            functionExitStmt.functionDefId = functionDefId;
            functionExitStmt.functionDefGlobalId = nodeArrayList.get(functionDefId).getId();
            functionExitStmt.setFunctionDefName(currentFunName);
            functionExitStmt.setFunSig(funDef.getSignature());
            functionExitStmt.setBelongFile(funDef.getStmt().getBelongFile());
            FunctionDefStatement functionDefStatement
                    = (FunctionDefStatement) nodeArrayList.get(functionDefId).getStmt();
            Boolean paramHasPointer = functionDefStatement.isParamHasPointer();
            functionExitStmt.setHasFunctionParamPointer(paramHasPointer);

            exitNode = new CASTNode(functionExitStmt);
            exitNode.getStmt().setLineno(nodeArrayList.get(nodeArrayList.size() - 1).getStmt().getLineno() + 1);
            nodeArrayList.add(exitNode);
            handleCfgTask(exitNode);

//            handleFunctionReturnDfgEdge(functionDefId, nodeArrayList.size() - 1, exitNode);
            // it seems that we do not need to care about if the last stmt is return
//            CopyOnWriteArrayList<CfgEdge> cfgEdges = exitNode.getCfgEdges();
//            for (CfgEdge cfgEdge : cfgEdges) {
//                CASTNode toNode = cfgEdge.toNode;
//                Statement stmt = toNode.getStmt();
//                if (stmt instanceof ReturnStatement) {
//                    continue;
//                }
//                handleFunctionReturnDfgEdge(functionDefId, nodeArrayList.size() - 1, exitNode);
//                break;
//            }
        }

        ArrayList<Integer> returnIds = ((FunctionDefStatement) funDef.getStmt()).getReturnIds();
        for (Integer returnId : returnIds) {
            nodeArrayList.get(returnId).addCfgEdge(EMPTY, exitNode);
            exitNode.addDfgEdge("return", nodeArrayList.get(returnId));
        }
    }

    private void handleExprStmt(IASTNode node) {
        IASTExpressionStatement expr = (IASTExpressionStatement) node;
        ExpressionStatement expressionStatement = new ExpressionStatement(expr);

        IASTExpression expression = expr.getExpression();
        CASTNode cur = build4CastNode(expression, expressionStatement);

        handleTask(expression, cur);

        // function pointer functionCall;
        // TODO other stmt may contains functionCall
        if (expression instanceof IASTFunctionCallExpression functionCallExpression) {
            boolean hasDfg = functionCallExpression.getArguments().length > 0;
            IASTExpression functionNameExpression = functionCallExpression.getFunctionNameExpression();
            if (functionNameExpression instanceof IASTIdExpression idExpression) {
                IASTName functionName = idExpression.getName();
                IASTName[] declarationsInAST = translationUnit.getDeclarationsInAST(functionName.resolveBinding());
                if (declarationsInAST.length == 0) {
                    return;
                }
                IASTName declaration = declarationsInAST[0];
                IASTNode declarationParent = StatementUtil.getStatementParent(declaration);
                Integer declIdx = iAST2cAST.get(declarationParent);
                if (declIdx == null || declIdx <= currentFunId) {
                    return;
                }

                IBinding iBinding = functionName.resolveBinding();
                IASTName[] definitionsInAST = translationUnit.getReferences(iBinding);
                int value = -1;
                for (IASTName iastName : definitionsInAST) {
                    IASTNode statementParent = StatementUtil.getStatementParent(iastName);
                    Integer idx = iAST2cAST.get(statementParent);
                    if (idx == null || idx == nodeArrayList.size() - 1) {
                        continue;
                    }
                    value = Math.max(idx, value);
                }
                if (value != -1) {
                    CASTNode castNode = nodeArrayList.get(value);
                    CopyOnWriteArrayList<CgEdge> cgEdges = castNode.getCgEdges();
                    for (CgEdge cgEdge : cgEdges) {
                        cur.addCgEdge(cgEdge.getData(), cgEdge.getToNode());
                        if (hasDfg) {
                            cgEdge.getToNode().addDfgEdge("functionArgs", cur);
                        }
                    }
                } else {
                    definitionsInAST = translationUnit.getDefinitionsInAST(functionName.resolveBinding());
                    for (IASTName iastName : definitionsInAST) {
                        IASTNode statementParent = StatementUtil.getStatementParent(iastName);
                        Integer idx = iAST2cAST.get(statementParent);
                        if (idx == null || idx == nodeArrayList.size() - 1) {
                            continue;
                        }
                        CASTNode castNode = nodeArrayList.get(idx);
                        CopyOnWriteArrayList<CgEdge> cgEdges = castNode.getCgEdges();
                        for (CgEdge cgEdge : cgEdges) {
                            cur.addCgEdge(cgEdge.getData(), cgEdge.getToNode());
                            if (hasDfg) {
                                cgEdge.getToNode().addDfgEdge("functionArgs", cur);
                            }
                        }
                    }
                }
            }
        }
    }

    private void handleWhileStmt(IASTNode node) throws Exception {
        IASTWhileStatement whileStmt = (IASTWhileStatement) node;
        IASTExpression condition = whileStmt.getCondition();
        WhileStatement whileStatement = new WhileStatement(condition);

        CASTNode cur = build4CastNode(condition, whileStatement);
        handleTask(condition, cur);

        WhileEnv whileEnv = new WhileEnv();
        if (!envStack.isEmpty() && envStack.peek().childMergeUp) {
            whileEnv.shouldFalseMergeUp = true;
            envStack.peek().childMergeUp = false;
        }

        int idx = nodeArrayList.size() - 1;
        whileEnv.setConditionIdx(idx);
        whileEnv.getFalseList().add(idx);
        whileEnv.setShouldTrueJump2(true);
        envStack.push(whileEnv);

        IASTStatement body = whileStmt.getBody();
        boolean hasCompoundBody = false;
        if (body instanceof IASTCompoundStatement) {
            handleCompoundStmt(body);
            hasCompoundBody = true;
        } else {
            handleSingleCompoundStmt(body);
        }

        if (!whileEnv.childMergeUp) {
            CASTNode lastStmt = nodeArrayList.get(whileEnv.getLastStmtIdx());
            if (lastStmt.getStmt() instanceof BreakStatement
                    || lastStmt.getStmt() instanceof ContinueStatement) {

            } else if (whileEnv.getLastStmtIdx() != 0) {
                lastStmt.addCfgEdge(EMPTY, cur);
            }
        }
        handleMergeUp();
        whileEnv.isFinished = true;

        if (hasCompoundBody) {
            int i = nodeArrayList.size() - 1;
            addDfgTask2Thread(whileEnv.conditionIdx, i, whileEnv.conditionIdx, i);
        }
    }

    private void handleBreakStmt(IASTNode node) {
        IASTBreakStatement iastBreakStatement = (IASTBreakStatement) node;
        BreakStatement breakStatement = new BreakStatement(iastBreakStatement);

        CASTNode cur = build4CastNode(iastBreakStatement, breakStatement);

        handleCfgTask(cur);

        for (int i = envStack.size() - 1; i >= 0; i--) {
            Env env = envStack.get(i);
            if (env instanceof WhileEnv whileEnv) {
                whileEnv.getFalseList().add(nodeArrayList.size() - 1);
                break;
            }
            if (env instanceof IfEnv ifEnv && ifEnv.isSwitch) {
                ifEnv.getIfEmptyList().add(nodeArrayList.size() - 1);
                break;
            }
        }
    }

    private void handleContinueStmt(IASTNode node) {
        IASTContinueStatement iastContinueStatement = (IASTContinueStatement) node;
        ContinueStatement continueStatement = new ContinueStatement(iastContinueStatement);

        CASTNode cur = build4CastNode(iastContinueStatement, continueStatement);
        handleCfgTask(cur);

        for (int i = envStack.size() - 1; i >= 0; i--) {
            if (envStack.get(i) instanceof WhileEnv whileEnv) {
                if (whileEnv.isDo) {
                    whileEnv.getContinueList().add(nodeArrayList.size() - 1);
                } else {
                    cur.addCfgEdge(EMPTY, nodeArrayList.get(whileEnv.getConditionIdx()));
                }
                break;
            }
        }
    }

    /**
     * the init and the iteration may be empty. FIXME
     */
    private void handleForStmt(IASTNode node) {
        // 初始化
        IASTForStatement iastForStatement = (IASTForStatement) node;
        IASTStatement stmt = iastForStatement.getInitializerStatement();

        CASTNode cur;
        if (stmt instanceof IASTDeclarationStatement declStmt) {
            DeclarationStatement declarationStatement = new DeclarationStatement(declStmt);
            cur = build4CastNode(declStmt.getDeclaration(), declarationStatement);
            handleTask(stmt, cur);
        } else if (stmt instanceof IASTExpressionStatement exprStmt) {
            ExpressionStatement expressionStatement = new ExpressionStatement(exprStmt);
            cur = build4CastNode(exprStmt.getExpression(), expressionStatement);
            handleTask(stmt, cur);
        } else if (!(stmt instanceof IASTNullStatement)) {
            throw new IllegalException(IllegalException.NOT_IMPLEMENT_YET, node);
        }

        WhileEnv forEnv = new WhileEnv();

        // TODO how to represent this is for loop?
        IASTExpression conditionExpression
                = iastForStatement.getConditionExpression();
        CASTNode curCond;
        if (conditionExpression != null) {
            ExpressionStatement cond
                    = new ExpressionStatement(conditionExpression);
            curCond = build4CastNode(conditionExpression, cond);
            handleTask(conditionExpression, curCond);
        } else {
            ForStatement forStatement = new ForStatement(iastForStatement);
            curCond = build4CastNode(iastForStatement, forStatement);
            handleTask(iastForStatement, curCond);
        }
        int condId = nodeArrayList.size() - 1;
        forEnv.getFalseList().add(condId);

        // env 处理
        forEnv.isFor = true;
        forEnv.conditionIdx = condId;
        if (!envStack.isEmpty() && envStack.peek().childMergeUp) {
            forEnv.shouldFalseMergeUp = true;
            envStack.peek().childMergeUp = false;
        }

        forEnv.setShouldTrueJump2(true);
        envStack.push(forEnv);

        boolean hasCompoundBody = false;
        IASTStatement body = iastForStatement.getBody();
        if (body instanceof IASTCompoundStatement) {
            handleCompoundStmt(body);
            hasCompoundBody = true;
        } else {
            handleSingleCompoundStmt(body);
        }

        IASTExpression iterationExpression
                = iastForStatement.getIterationExpression();
        if (iterationExpression != null) {
            ExpressionStatement iteration
                    = new ExpressionStatement(iterationExpression);
            CASTNode curIter = build4CastNode(iterationExpression, iteration);
            handleTask(iterationExpression, curIter);

            curIter.addCfgEdge(EMPTY, curCond);
        } else {
            ArrayList<Pair<Integer, String>> pairs = cfgTask.get(nodeArrayList.size());
            if (pairs != null) {
                for (Pair<Integer, String> pair : pairs) {
                    nodeArrayList.get(pair.getKey()).addCfgEdge(pair.getValue(), curCond);
                }
                cfgTask.remove(nodeArrayList.size());
            }
        }
        forEnv.iterationIdx = nodeArrayList.size() - 1;

        if (forEnv.shouldFalseMergeUp) {
            forEnv.mergeUpFalseList.add(condId);
        }
        if (!forEnv.childMergeUp) {
            CASTNode lastStmt = nodeArrayList.get(forEnv.getLastStmtIdx());
            if (lastStmt.getStmt() instanceof BreakStatement
                    || lastStmt.getStmt() instanceof ContinueStatement) {

            } else {
                if (forEnv.getLastStmtIdx() != 0 && forEnv.getLastStmtIdx() != forEnv.iterationIdx) {
                    lastStmt.addCfgEdge(EMPTY, nodeArrayList.get(forEnv.iterationIdx));
                }
            }
        }
        handleMergeUp();
        forEnv.isFinished = true;

        if (hasCompoundBody) {
            addDfgTask2Thread(forEnv.conditionIdx, forEnv.iterationIdx,
                    forEnv.conditionIdx, forEnv.iterationIdx);
        }
    }

    private void handleDoStmt(IASTNode node) {
        IASTDoStatement iastDoStatement = (IASTDoStatement) node;
        IASTExpression condition = iastDoStatement.getCondition();
        DoWhileStatement doWhileStatement = new DoWhileStatement(condition);

        WhileEnv doEnv = new WhileEnv();
        doEnv.isDo = true;
        if (!envStack.isEmpty() && envStack.peek().childMergeUp) {
            doEnv.shouldFalseMergeUp = true;
            envStack.peek().childMergeUp = false;
        }
        envStack.push(doEnv);

        // 1. 先处理 body
        int firstStmt = nodeArrayList.size();
        IASTStatement body = iastDoStatement.getBody();
        if (body instanceof IASTCompoundStatement) {
            handleCompoundStmt(body);
        } else {
            handleSingleCompoundStmt(body);
        }

        // 2. 再处理 cond
        CASTNode cur = build4CastNode(condition, doWhileStatement);

        int condId = nodeArrayList.size() - 1;
        doEnv.setConditionIdx(condId);
        doEnv.getFalseList().add(condId);

        handleTask(condition, cur);
        cur.addCfgEdge(TRUE, nodeArrayList.get(firstStmt));

        // continueList special for do-while
        for (Integer i : doEnv.getContinueList()) {
            nodeArrayList.get(i).addCfgEdge(EMPTY, cur);
        }

        if (doEnv.shouldFalseMergeUp) {
            doEnv.mergeUpFalseList.add(condId);
        }

        if (!doEnv.childMergeUp) {
            CASTNode lastStmt = nodeArrayList.get(nodeArrayList.size() - 2);
            if (lastStmt.getStmt() instanceof BreakStatement
                    || lastStmt.getStmt() instanceof ContinueStatement) {

            } else {
                lastStmt.addCfgEdge(EMPTY, nodeArrayList.get(condId));
            }
        }
        handleMergeUp();
        doEnv.isFinished = true;

        addDfgTask2Thread(firstStmt, condId, firstStmt, condId);
    }

    private void handleSwitchStmt(IASTNode node) {
        IASTSwitchStatement iastSwitchStatement = (IASTSwitchStatement) node;
        SwitchStatement switchStatement = new SwitchStatement(iastSwitchStatement.getControllerExpression());

        CASTNode cur = build4CastNode(iastSwitchStatement.getControllerExpression(), switchStatement);
        handleTask(iastSwitchStatement, cur);

        IfEnv switchEnv = new IfEnv();
        switchEnv.isSwitch = true;
        switchEnv.setConditionIdx(nodeArrayList.size() - 1);
        if (!envStack.isEmpty()) {
            if (envStack.peek().childMergeUp || envStack.peek().siblingMergeUp) {
                switchEnv.shouldEmptyMergeUp = true;
                if (envStack.peek().childMergeUp) {
                    envStack.peek().childMergeUp = false;
                }
            }
        }
        envStack.push(switchEnv);

        IASTStatement body = iastSwitchStatement.getBody();
        if (body instanceof IASTCompoundStatement) {
            handleCompoundStmt(body);
        } else {
            handleSingleCompoundStmt(body);
        }

        handleMergeUp();
        switchEnv.isFinished = true;
    }

    private void handleCaseStmt(IASTNode node) {
        IASTCaseStatement iastCaseStatement = (IASTCaseStatement) node;
        CaseStatement caseStatement = new CaseStatement(iastCaseStatement);

        CASTNode cur = build4CastNode(iastCaseStatement, caseStatement);
        handleTask(iastCaseStatement, cur);

        for (int i = envStack.size() - 1; i >= 0; i--) {
            Env env1 = envStack.get(i);
            if (env1 instanceof IfEnv ifEnv) {
                if (ifEnv.isSwitch) {
                    nodeArrayList.get(ifEnv.getConditionIdx()).addCfgEdge(EMPTY, cur);
                    break;
                }
            }
        }
    }

    private void handleDefaultStmt(IASTNode node) {
        IASTDefaultStatement iastDefaultStatement = (IASTDefaultStatement) node;
        DefaultStatement defaultStatement = new DefaultStatement(iastDefaultStatement);

        CASTNode cur = build4CastNode(iastDefaultStatement, defaultStatement);
        handleTask(iastDefaultStatement, cur);

        IfEnv env = (IfEnv) envStack.peek();
//        try {
//            env = (IfEnv) envStack.peek();
//        } catch (Exception e) {
//            DebugUtil.printNodeInfo(iastDefaultStatement);
//            env = (IfEnv) envStack.peek();
//        }
        nodeArrayList.get(env.getConditionIdx()).addCfgEdge(EMPTY, cur);
    }

    private void handleMergeUp() {
        if (!envStack.isEmpty() &&
                (envStack.peek().shouldFalseMergeUp
                        || envStack.peek().shouldEmptyMergeUp)) {
            Env pop = envStack.pop();

            if (!envStack.isEmpty()) {
                Env peek = envStack.peek();
                if (pop instanceof WhileEnv current) {
                    if (peek instanceof WhileEnv parent) {
                        // only care about the falseList(the false of condition and breaks)
                        // the condition of do-while is unknown And the iteration of for is unknown
                        CASTNode condition = nodeArrayList.get(current.conditionIdx);
                        if (parent.isFor || parent.isDo) {
                            ArrayList<Pair<Integer, String>> objects = new ArrayList<>();
                            objects.add(new Pair<>(current.conditionIdx, FALSE));
                            for (Integer i : current.getFalseList()) {
                                objects.add(new Pair<>(i, EMPTY));
                            }

                            // the iteration of parent is unknown
                            cfgTask.put(nodeArrayList.size(), objects);
                        } else {
                            condition.addCfgEdge(FALSE, nodeArrayList.get(parent.conditionIdx));
                            for (Integer i : current.getFalseList()) {
                                nodeArrayList.get(i).addCfgEdge(EMPTY, condition);
                            }
                        }
                    }

                    if (peek instanceof IfEnv parent) {
                        parent.mergeUpFalseList.add(current.conditionIdx);
                        parent.mergeUpFalseList.addAll(current.getFalseList());
                        parent.mergeUpFalseList.addAll(current.mergeUpFalseList);
                    }
                }

                // for { ... if {}}
                if (pop instanceof IfEnv current) {
                    if (peek instanceof WhileEnv parent) {
                        if (parent.isDo || parent.isFor) {
                            ArrayList<Pair<Integer, String>> objects = new ArrayList<>();
                            for (Integer i : current.getFalseList()) {
                                objects.add(new Pair<>(i, FALSE));
                            }
                            for (Integer i : current.getIfEmptyList()) {
                                objects.add(new Pair<>(i, EMPTY));
                            }
                            for (Integer i : current.mergeUpEmptyList) {
                                objects.add(new Pair<>(i, EMPTY));
                            }
                            for (Integer i : current.mergeUpFalseList) {
                                objects.add(new Pair<>(i, FALSE));
                            }
                            cfgTask.put(nodeArrayList.size(), objects);
                        } else {
                            addCFGEdgeByList(current.getFalseList(), parent.getConditionIdx(), FALSE);
                            addCFGEdgeByList(current.getIfEmptyList(), parent.getConditionIdx(), EMPTY);
                            addCFGEdgeByList(current.mergeUpEmptyList, parent.getConditionIdx(), EMPTY);
                            addCFGEdgeByList(current.mergeUpFalseList, parent.getConditionIdx(), FALSE);
                        }
                    }
                    if (peek instanceof IfEnv parent) {
                        parent.mergeUpFalseList.addAll(current.mergeUpFalseList);
                        parent.mergeUpFalseList.addAll(current.getFalseList());
                        parent.mergeUpEmptyList.addAll(current.mergeUpEmptyList);
                        parent.mergeUpEmptyList.addAll(current.getIfEmptyList());
                    }
                }
            }
        }
    }

    private void addCFGEdgeByList(ArrayList<Integer> list, Integer toIdx, String value) {
        CASTNode toNode = nodeArrayList.get(toIdx);
        for (Integer idx : list) {
            CASTNode fromNode = nodeArrayList.get(idx);
            fromNode.addCfgEdge(value, toNode);
        }
    }

    private int failedCount = 0;

    private void handleTask(IASTNode node, CASTNode cur) {
        handleCfgTask(cur);
        handleCgTask(node, cur);

        handleMultiLineMacroTask(cur);
        // handleDecl2UsageDfgTask(node, cur);

        addDfgTask2Thread(node);
    }

    private void handleDecl2UsageDfgTask(IASTNode node, CASTNode cur) {
        // use multiThread
        ArrayList<AstBinding> astBindings = cur.getStmt().resolveVarRef();
        if (astBindings == null) {
            astBindings = new ArrayList<>();
        }
        if (cur.getStmt() instanceof ExpressionStatement expressionStatement) {
            ArrayList<AstBinding> leftBindings = expressionStatement.extractLvalue();
            if (leftBindings != null) {
                for (AstBinding leftBinding : leftBindings) {
                    leftBinding.setDefineOperationLike("LeftValueInAssign");
                }
                astBindings.addAll(leftBindings);
            }
        } else if (cur.getStmt() instanceof DeclarationStatement declarationStatement) {
            ArrayList<IASTName> typeDeclAstName = declarationStatement.getTypeDeclAstName();
            for (IASTName iastName : typeDeclAstName) {
                AstBinding astBinding = new AstBinding();
                astBinding.setName(iastName);
                astBindings.add(astBinding);
            }
        }

        if (this.translationUnit instanceof CASTTranslationUnit) {
            handleDecl2UsageDfgInC(astBindings, cur);
        } else {
            handleDecl2UsageDfgInCPP(astBindings, cur);
        }
    }


    private void handleMultiLineMacroTask(CASTNode cur) {
        ArrayList<IGNUASTCompoundStatementExpression> res = MacroUtil.getMultiLineMacroUsage(cur.getStmt());
        if (res.isEmpty()) {
            return;
        }

        cur.setLastValidNode(true);
        cur.setHasMultiLineMacro(true);

        Stack<Env> copy = (Stack<Env>) envStack.clone();
        for (IGNUASTCompoundStatementExpression macroStmt : res) {
            int first = nodeArrayList.size();

            envStack.clear();

            IASTCompoundStatement compoundStatement = macroStmt.getCompoundStatement();
            handleCompoundStmt(compoundStatement);
            int last = nodeArrayList.size() - 1;

            for (int i = first; i <= last; i++) {
                nodeArrayList.get(i).setHasMultiLineMacro(true);
            }
            nodeArrayList.get(last).addCfgEdge("macro", cur);
            nodeArrayList.get(last).addDfgEdge("macro", cur);
        }
        envStack = copy;
    }

    /**
     * if the node has cfgTask, it is special node if/for-like.
     * it many have an array to add
     */
    private void handleCfgTask(CASTNode cur) {
        int idx = nodeArrayList.size() - 1;
        ArrayList<Pair<Integer, String>> pairs = cfgTask.get(idx);
        if (pairs == null) {
            addCfgEdge2Node(cur);
            return;
        }

        for (Pair<Integer, String> pair : pairs) {
            CASTNode fromNode = nodeArrayList.get(pair.getKey());
            fromNode.addCfgEdge(pair.getValue(), cur);
        }
        cfgTask.remove(idx);
    }


    private void addDfgTask2Thread(IASTNode node) {
        Integer id = iAST2cAST.get(node);
        if (id != null) {
            handleDfgTask(node, nodeArrayList.get(id));
//            ConcurrentTask concurrentTask = new ConcurrentTask(id, id);
//            dfgBuffer.add(concurrentTask);
        }
    }

    private void addDfgTask2Thread(int st, int ed, int minv, int maxv) {
//        ConcurrentTask concurrentTask = new ConcurrentTask(st, ed, minv, maxv);
        buildDfgByReachability(st, ed, minv, maxv);
//        dfgBuffer.add(concurrentTask);
    }

    /**
     * dfg is complex.
     * some use ddgTask container add, function parameters
     * some use cfg-path add, the last modified stmt or decl -> cur
     */
    private void handleDfgTask(IASTNode node, CASTNode cur) {
        if (node instanceof IASTFunctionDefinition || shouldNotAddDfgEmptyEdge(node)) {
            return;
        }

        handleDecl2UsageDfgTask(node, cur);

        Integer id = iAST2cAST.get(node);
        buildDfgByReachability(id, id, cur.getFunctionId(), id);
    }

    // TODO optimize
    private void handleDecl2UsageDfgInCPP(ArrayList<AstBinding> astBindings, CASTNode cur) {
        for (AstBinding astBinding : astBindings) {
            if (astBinding.getIsLiteral())
                continue;
            IASTName name = astBinding.getName();
            IBinding iBinding = name.resolveBinding();
            if (iBinding instanceof CPPVariable
                    || iBinding instanceof CPPTypedef
                    || iBinding instanceof CPPEnumerator
                    || iBinding instanceof CPPEnumeration) {
                IASTNode definition;
                if (iBinding instanceof CPPVariable variable) {
                    definition = variable.getDefinition();
                } else {
                    if (iBinding instanceof CPPTypedef typedef) {
                        definition = typedef.getDefinition();
                    } else if (iBinding instanceof CPPEnumerator enumerator) {
                        definition = enumerator.getPhysicalNode();
                    } else {
                        CPPEnumeration enumeration = (CPPEnumeration) iBinding;
                        definition = enumeration.getDefinition();
                    }
                }
                IASTNode statementParent = StatementUtil.getStatementParent(definition);
                Integer cIdx = iAST2cAST.get(statementParent);
                if (cIdx == null) {
                    continue;
                }
                CASTNode castNode = nodeArrayList.get(cIdx);
                if (castNode.equals(cur)) {
                    continue;
                }
                cur.addDfgEdge(DECL2USAGE, nodeArrayList.get(cIdx));
            } else if (iBinding instanceof CPPParameter || iBinding instanceof CPPFunction) {
                CPPFunction owner;
                if (iBinding instanceof CPPParameter parameter) {
                    owner = (CPPFunction) parameter.getOwner();
                } else {
                    owner = ((CPPFunction) iBinding);
                }

                if (owner != null) {
                    IASTNode funDefNode = bindingDefCache.get(owner);
                    if (funDefNode != null) {
                        Integer idx = iAST2cAST.get(funDefNode);
                        if (idx != null) {
                            cur.addDfgEdge(DECL2USAGE, nodeArrayList.get(idx));
                        }
                    }
                }
            } else {
                String string = iBinding.toString();
                if (
                        !string.startsWith("Attempt to use symbol failed")
                                && !string.startsWith("A definition was not found for")
                                && !string.startsWith("Invalid type encountered in")
                                && !string.isEmpty()) {
                    failedCount++;
//                    System.out.println(AnsiColors.ANSI_CYAN + iBinding + " unknown ibinding." + AnsiColors.ANSI_RESET);
                }
            }
        }
    }

    // FIXME we can not add any other attr over the edge
    private void handleDecl2UsageDfgInC(ArrayList<AstBinding> astBindings, CASTNode cur) {
        for (AstBinding astBinding : astBindings) {
            if (astBinding.getIsLiteral())
                continue;
            IASTName name = astBinding.getName();
            IBinding iBinding = name.resolveBinding();
            if (iBinding instanceof ICInternalBinding || iBinding instanceof CEnumerator) {
                IASTNode definition;
                if (iBinding instanceof ICInternalBinding internalBinding) {
                    definition = internalBinding.getDefinition();
                } else {
                    definition = ((CEnumerator) iBinding).getPhysicalNode();
                }

                if (definition != null) {
                    IASTNode statementParent = StatementUtil.getStatementParent(definition);
                    Integer cIdx = iAST2cAST.get(statementParent);
                    if (cIdx == null) {
                        // something special, like typedef before struct def.
                        ArrayList<Pair<IBinding, String>> pairs = dfgTask.get(statementParent);
                        if (pairs == null) {
                            pairs = new ArrayList<>();
                        }
                        pairs.add(new Pair<>(iBinding, DECL2USAGE));
                        continue;
                    }
                    CASTNode castNode = nodeArrayList.get(cIdx);
                    if (castNode.equals(cur)) {
                        continue;
                    }
                    cur.addDfgEdge(DECL2USAGE, nodeArrayList.get(cIdx), astBinding.getDefineOperationLike());
                }
            } else if (iBinding instanceof CParameter parameter) {
                CFunction owner = (CFunction) parameter.getOwner();
                if (owner != null) {
                    IASTNode funDefNode = bindingDefCache.get(owner);
                    if (funDefNode != null) {
                        Integer idx = iAST2cAST.get(funDefNode);
                        cur.addDfgEdge(DECL2USAGE, nodeArrayList.get(idx), astBinding.getDefineOperationLike());
                    }
                }
            } else {
                failedCount++;
//                String string = iBinding.toString();
//                if (!string.startsWith("Attempt to use symbol failed")
//                        && !string.startsWith("A definition was not found for")
//                        && !string.startsWith("Invalid type encountered in")
//                        && !string.isEmpty()) {
//                    System.out.println(AnsiColors.ANSI_CYAN + iBinding + " unknown ibinding." + AnsiColors.ANSI_RESET);
//                }
            }
        }
    }

    private int handleAstBindingArray(ArrayList<AstBinding> refBindings, IASTNode node, CASTNode cur
            , int st, int ed) {
        if (refBindings.isEmpty()) {
            return -1;
        }
        boolean hasIastName = false;
        for (AstBinding refBinding : refBindings) {
            if (refBinding.getName() != null) {
                hasIastName = true;
                break;
            }
        }
        if (!hasIastName) {
            return st;
        }

        int last = -1;
        int lastCnt = -1;
        ArrayList<Integer> defIds = new ArrayList<>();
        ArrayList<AstBinding> refBindingsBackup = new ArrayList<>(refBindings);
        while (!refBindings.isEmpty()) {
            // merge all the bindings, this is the parent of names
            // names can be used as the dfg-attr
            ArrayList<IASTNode> refNodes = mergeReferIAstName(refBindings, node, st, ed);
            for (IASTNode refNode : refNodes) {
                Integer idx = iAST2cAST.get(refNode);
                if (idx == null) {
                    continue;
                }
                CASTNode castNode = nodeArrayList.get(idx);
                if (castNode.getFunctionId() != -1 && (idx < st || idx > ed)) {
                    continue;
                }

                boolean isDelete = false;
                boolean isFunCall = false;
                boolean isArgUseAmper = false;
                if (refNode instanceof ICPPASTDeleteExpression) {
                    isDelete = true;
                } else {
                    ArrayList<IASTFunctionCallExpression> functionCallParent = StatementUtil.getFunctionCallParent(refNode);
                    if (!functionCallParent.isEmpty()) {
                        isFunCall = true;
                    }
                    for (IASTFunctionCallExpression functionCall : functionCallParent) {
                        isArgUseAmper |= PointerUtil.checkIfArgUseAmper(functionCall, refBindings);
                    }
                }

                // check if the var may be modified here.
                // 1. assign
                // 2. var is pointer, passed by functionCall
                if (
                        checkIfAssignLeftV(refBindings, castNode)
                        || (isFunCall && refBindings.get(0).isPointer())
                        || (isFunCall && isArgUseAmper)
                        || ExprUtil.checkIfSelfOperation(refBindings, refNode)
                        || isDelete
                ) {
                    // the stmt should be the last stmt, it means in this path, there should no another stmt modifies
                    // the var
                    if (config.getHighPrecision()) {
                        if (cfgCanReach(castNode, cur, new HashSet<>())) {
                            defIds.add(idx);
                        }
                    } else {
                        if (idx > last) {
                            last = idx;
                        }
                    }
                }
            }

//            if (config.getHighPrecision() && !defIds.isEmpty() || !config.getHighPrecision() && last != -1) {
//                break;
//            }

            AstBinding astBinding = refBindings.get(0);
            refBindings.remove(0);
            int value = astBinding.getValue();
            while (!refBindings.isEmpty() && value == 1) {
                AstBinding tmpBinding = refBindings.get(0);
                if (tmpBinding.getValue() != 0) {
                    break;
                }
                refBindings.remove(0);
            }
        }
        if (config.getHighPrecision()) {
            if (!defIds.isEmpty()) {
                HashMap<Integer, Boolean> id2Bool = new HashMap<>();
                for (Integer defId1 : defIds) {
                    for (Integer defId2 : defIds) {
                        if (defId1.equals(defId2)) {
                            continue;
                        }
                        if (cfgCanReach(nodeArrayList.get(defId1), nodeArrayList.get(defId2), new HashSet<>())) {
                            id2Bool.put(defId1, true);
                            break;
                        }
                    }
                }
                int maxv = -1;
                for (Integer defId : defIds) {
                    if (id2Bool.get(defId) != null) {
                        continue;
                    }
                    maxv = Math.max(maxv, defId);
                    for (AstBinding refBinding : refBindingsBackup) {
                        cur.addDfgEdge(refBinding.toString(), nodeArrayList.get(defId), refBinding.getDefineOperationLike());
                    }
                }
                return maxv;
            }
        } else {
            if (last != -1) {
                for (AstBinding refBinding : refBindingsBackup) {
                    cur.addDfgEdge(refBinding.toString(), nodeArrayList.get(last), refBinding.getDefineOperationLike());
                }
                return last;
            }
        }
        return -1;
    }

    // cfg from-to is reverse
    private boolean cfgCanReach(CASTNode to, CASTNode from, HashSet<Integer> hasVisited) {
        hasVisited.add(from.getId());
        for (CfgEdge cfgEdge : from.getCfgEdges()) {
            CASTNode edgeTo = cfgEdge.getToNode();
            if (hasVisited.contains(edgeTo.getId())) {
                continue;
            }
            if (edgeTo.getId() == to.getId()) {
                return true;
            }
            if (cfgCanReach(to, edgeTo, hasVisited)) {
                return true;
            }
        }
        hasVisited.remove(from.getId());
        return false;
    }

    private void handleFunctionReturnDfgEdge(int st, int ed, CASTNode cur) {
        for (int i = st; i <= ed; i++) {
            CASTNode toNode = nodeArrayList.get(i);
            IASTNode toNodeI = toNode.getStmt().getIASTNode();
            if (toNodeI == null) {
                continue;
            }
            final Boolean[] shouldAddDfgEdge = {false};
            toNodeI.accept(new ASTVisitor() {
                {
                    shouldVisitExpressions = true;
                }

                @Override
                public int visit(IASTExpression expression) {
                    if (expression instanceof IASTFunctionCallExpression functionCall) {
                        IASTInitializerClause[] arguments = functionCall.getArguments();
                        for (IASTInitializerClause arg : arguments) {
                            if (arg instanceof IASTExpression argExpr) {
                                try {
                                    if (argExpr.getExpressionType() instanceof IPointerType) {
                                        shouldAddDfgEdge[0] = true;
                                    }
                                } catch (Exception ignored) {
                                    failedCount++;
                                }
                            }
                        }
                    } else if (expression instanceof IASTBinaryExpression binaryExpression) {
                        if (ExprUtil.isAssign(binaryExpression.getOperator())) {
                            IASTExpression lhs = binaryExpression.getOperand1();
                            try {
                                if (lhs.getExpressionType() instanceof IPointerType
                                        || lhs.getExpressionType() instanceof IArrayType) {
                                    shouldAddDfgEdge[0] = true;
                                }
                            } catch (Exception ignored) {
                                failedCount++;
                            }
                        }
                    }
                    return PROCESS_CONTINUE;
                }
            });
            if (shouldAddDfgEdge[0]) {
                cur.addDfgEdge("FunctionReturn", toNode);
            }
        }
    }

    private void buildDfgByReachability(int st, int ed, int minv, int maxv) {
        for (int k = st; k <= ed; k++) {
            CASTNode cur = nodeArrayList.get(k);
            IASTNode node = cur.getStmt().getIASTNode();

            ArrayList<AstBinding> astBindings = cur.getStmt().resolveVarRef();
            if (astBindings == null) {
                return;
            }
            for (int i = 0, j = 0; i < astBindings.size(); i = Math.max(i + 1, j)) {
                AstBinding astBinding = astBindings.get(i);
                if ("NULL".equals(astBinding.toString())) {
                    continue;
                }

                j = i + 1;
                ArrayList<AstBinding> refBindings = new ArrayList<>();
                refBindings.add(astBinding);
                int value = astBinding.getValue();
                while (j < astBindings.size() && value > 0) {
                    AstBinding tmpBinding = astBindings.get(j);
                    refBindings.add(tmpBinding);
                    value += tmpBinding.getValue();
                    j++;
                    if (value == 0) {
                        break;
                    }
                }

                // only operator
                if (refBindings.size() == 1 && refBindings.get(0).getIsLiteral()) {
                    continue;
                }

                // array subs
                for (int i1 = 0; i1 < refBindings.size() - 1; i1++) {
                    AstBinding refBinding = refBindings.get(i1);
                    if (refBinding.getIsCollection()) {
                        ArrayList<AstBinding> tmpAstBindings = new ArrayList<>();
                        tmpAstBindings.add(refBinding);
                        minv = Math.max(minv, handleAstBindingArray(tmpAstBindings, node, cur, minv, maxv));
                    }
                }
                for (AstBinding refBinding : refBindings) {
                    refBinding.setInitialSize(refBindings.size());
                }
                handleAstBindingArray(refBindings, node, cur, minv, maxv);
            }
        }
    }

    private void handleCgTask(IASTNode node, CASTNode cur) {
        // FIXME it caused v e range
//        ArrayList<FunctionCallBinding> functionCallBindings = FunctionCallUtil.resolveFunRef(node);
//        for (FunctionCallBinding functionCallBinding : functionCallBindings) {
//            IASTName functionName = functionCallBinding.functionName();
//            IBinding iBinding = functionName.resolveBinding();
//            if (iBinding instanceof CParameter parameter) {
//                iBinding = parameter.getOwner();
//            }
//            if (iBinding instanceof ProblemBinding) {
//                // System.out.println(iBinding);
//                continue;
//            }
//            ICInternalBinding binding = (ICInternalBinding) iBinding;
//            IASTNode definition;
//            definition = binding.getDefinition();
//            if (definition == null) {
//                IASTNode[] declarations = binding.getDeclarations();
//                if (declarations == null || declarations.length == 0) {
//                    continue;
//                }
//                definition = declarations[0];
//            }
//            IASTNode statementParent = StatementUtil.getStatementParent(definition);
//            Integer idx = iAST2cAST.get(statementParent);
//            if (idx != null) {
//                cur.addCgEdge(functionName.toString(), nodeArrayList.get(idx));
//            } else {
//                cgTask.computeIfAbsent(statementParent, k -> new ArrayList<>())
//                        .add(new Pair<>(iBinding, functionName.toString()));
//                if (functionCallBinding.hasArgs()) {
//                    dfgTask.computeIfAbsent(statementParent, k -> new ArrayList<>())
//                            .add(new Pair<>(iBinding, "FunctionArgs"));
//                }
//            }
//        }

        ArrayList<Pair<IBinding, String>> pairs = cgTask.get(node);
        if (pairs != null) {
            for (Pair<IBinding, String> pair : pairs) {
                IASTNode iastNode = bindingDefCache.get(pair.getKey());
                Integer i = iAST2cAST.get(iastNode);
                if (i != null) {
                    cur.addCgEdge(pair.getValue(), nodeArrayList.get(i));
                } else {
                    ArrayList<Pair<IASTNode, String>> pairs1 = invCgTask.get(pair.getKey());
                    if (pairs1 == null) {
                        pairs1 = new ArrayList<>();
                    }
                    pairs1.add(new Pair<>(node, pair.getValue()));
                    invCgTask.put(pair.getKey(), pairs1);
                }
            }
            cgTask.remove(node);
        }

        // functionArgs
        pairs = dfgTask.get(node);
        if (pairs != null) {
            for (Pair<IBinding, String> pair : pairs) {
                IASTNode iastNode = bindingDefCache.get(pair.getKey());
                Integer i = iAST2cAST.get(iastNode);
                if (i != null) {
                    nodeArrayList.get(i).addDfgEdge(pair.getValue(), cur);
                } else {
                    ArrayList<Pair<IASTNode, String>> pairs1 = invDfgTask.get(pair.getKey());
                    if (pairs1 == null) {
                        pairs1 = new ArrayList<>();
                    }
                    pairs1.add(new Pair<>(node, pair.getValue()));
                    invDfgTask.put(pair.getKey(), pairs1);
                }
            }
            dfgTask.remove(node);
        }
    }

    private void addNormalEmptyCfgEdge(CASTNode cur) {
        if (nodeArrayList.size() < 2)
            return;

        int idx = -1;
        for (int i = nodeArrayList.size() - 2; i >= 0; i--) {
            if (!nodeArrayList.get(i).isHasMultiLineMacro()) {
                idx = i;
                break;
            } else if (nodeArrayList.get(i).isLastValidNode()) {
                idx = i;
                break;
            }
        }

        CASTNode fromNode = nodeArrayList.get(idx);
        if (!shouldNotAddCfgEmptyEdge(fromNode)) {
            fromNode.addCfgEdge(EMPTY, cur);
        }
    }

    private void addNormalEmptyCfgEdge(CASTNode cur, ArrayList<Integer> list) {
        for (Integer idx : list) {
            CASTNode fromNode = nodeArrayList.get(idx);
//            if (!checkEmptyForbidden(fromNode))
            {
                fromNode.addCfgEdge(EMPTY, cur);
            }
        }
    }

    /**
     * cfg -> cur
     */
    private void addCfgEdge2Node(CASTNode cur) {
        if (cur.getStmt() instanceof FunctionDefStatement) {
            envStack.clear();
            return;
        }

        if (envStack.isEmpty()) {
            addNormalEmptyCfgEdge(cur);
        }

        Env last = null;
        while (!envStack.isEmpty()) {
            Env env = envStack.peek();
            if (env == last) {
                break;
            }
            last = env;

            if (env instanceof IfEnv env1) {
                handleIfEnv(env1, cur);
            } else if (env instanceof WhileEnv env1) {
                handleWhileEnv(env1, cur);
                break;
            }
        }
    }

    private void handleIfEnv(IfEnv ifEnv, CASTNode cur) {
        boolean hasV = false;
        if (ifEnv.addTrueList) {
            hasV = true;
            for (Integer idx : ifEnv.getTrueList()) {
                CASTNode node = nodeArrayList.get(idx);
                node.addCfgEdge(TRUE, cur);
            }
            ifEnv.addTrueList = false;
            ifEnv.getTrueList().clear();
        } else if (ifEnv.addFalseList) {
            hasV = true;
            for (Integer idx : ifEnv.getFalseList()) {
                CASTNode node = nodeArrayList.get(idx);
                node.addCfgEdge(FALSE, cur);
            }
            ifEnv.addFalseList = false;
            ifEnv.getFalseList().clear();
        } else if (ifEnv.isFinished) {
            hasV = true;
            // it has the function of addFalseList
            ifEnv.getIfEmptyList().addAll(ifEnv.mergeUpEmptyList);
            addNormalEmptyCfgEdge(cur, ifEnv.getIfEmptyList());
            for (Integer idx : ifEnv.mergeUpFalseList) {
                CASTNode node = nodeArrayList.get(idx);
                node.addCfgEdge(FALSE, cur);
            }

            // If finish
            envStack.pop();
        }

        if (!hasV) {
            addNormalEmptyCfgEdge(cur);
        }
    }

    private void handleWhileEnv(WhileEnv peek, CASTNode cur) {
        if (peek.shouldTrueJump2) {
            CASTNode condition = nodeArrayList.get(peek.conditionIdx);
            condition.addCfgEdge(TRUE, cur);

            peek.shouldTrueJump2 = false;
        } else if (peek.isFinished) {
            CASTNode condition = nodeArrayList.get(peek.conditionIdx);
            // 处理 break 与 cond-false
            for (Integer idx : peek.getFalseList()) {
                if (idx.equals(peek.conditionIdx)) {
                    nodeArrayList.get(idx).addCfgEdge(FALSE, cur);
                } else {
                    nodeArrayList.get(idx).addCfgEdge(EMPTY, cur);
//                    addNormalEmptyCfgEdge(nodeArrayList.get(idx), cur);
                }
            }
            addNormalEmptyCfgEdge(cur, peek.mergeUpEmptyList);
            for (Integer idx : peek.mergeUpFalseList) {
                nodeArrayList.get(idx).addCfgEdge(FALSE, condition);
            }

            // finish
            envStack.pop();
        } else {
            addNormalEmptyCfgEdge(cur);
        }
    }

    /**
     * the env is finished, but the cfg-edge have not added yet.
     * we add the task to the node in future(there must be a node).
     */
    private void addCfgTaskTodo(Env env) {
        ArrayList<Pair<Integer, String>> pairs = new ArrayList<>();
        if (env instanceof IfEnv ifEnv) {
            for (Integer idx : ifEnv.getFalseList()) {
                pairs.add(new Pair<>(idx, FALSE));
            }
            for (Integer idx : ifEnv.mergeUpFalseList) {
                pairs.add(new Pair<>(idx, FALSE));
            }
            for (Integer idx : ifEnv.mergeUpEmptyList) {
                pairs.add(new Pair<>(idx, EMPTY));
            }
            for (Integer idx : ifEnv.getIfEmptyList()) {
                pairs.add(new Pair<>(idx, EMPTY));
            }
        } else {
            WhileEnv whileEnv = (WhileEnv) env;
            for (Integer idx : whileEnv.getFalseList()) {
                if (idx.equals(whileEnv.conditionIdx)) {
                    pairs.add(new Pair<>(idx, FALSE));
                } else {
                    pairs.add(new Pair<>(idx, EMPTY));
                }
            }
            for (Integer idx : env.mergeUpFalseList) {
                pairs.add(new Pair<>(idx, FALSE));
            }
            for (Integer idx : env.mergeUpEmptyList) {
                pairs.add(new Pair<>(idx, EMPTY));
            }
        }

        ArrayList<Pair<Integer, String>> original = cfgTask.get(nodeArrayList.size());
        if (original == null) {
            cfgTask.put(nodeArrayList.size(), pairs);
        } else {
            original.addAll(pairs);
            cfgTask.put(nodeArrayList.size(), original);
        }
    }

    // this is quiet easy. each function call is unique, just add task
    private void addCgAndDfgTaskToDo(IASTName funName, Boolean hasParam) {
        ArrayList<IASTName> names = getAstReferences(funName);
        for (IASTName name : names) {
            IASTNode parent = StatementUtil.getStatementParent(name);

            ArrayList<Pair<IBinding, String>> tasks = cgTask.get(parent);
            if (tasks == null) {
                tasks = new ArrayList<>();
            }
            tasks.add(new Pair<>(funName.resolveBinding(), funName.toString()));
            cgTask.put(parent, tasks);

            if (hasParam) {
                tasks = this.dfgTask.get(parent);
                if (tasks == null) {
                    tasks = new ArrayList<>();
                }

                tasks.add(new Pair<>(funName.resolveBinding(), "FunctionArgs"));
                this.dfgTask.put(parent, tasks);
            }
        }
    }

    //    public ConcurrentHashMap<IBinding, ArrayList<IASTName>> bindingDeclCache = new ConcurrentHashMap<>();
//    public ConcurrentHashMap<IBinding, ArrayList<IASTName>> bindingDeclBackup;
    public HashMap<IBinding, IASTNode> bindingDefCache = new HashMap<>();
    public ConcurrentHashMap<IBinding, IASTNode> bindingDefBackup;
    public HashMap<IBinding, ArrayList<IASTName>> bindingReferenceCache = new HashMap<>();
    public ConcurrentHashMap<IBinding, ArrayList<IASTName>> bindingReferenceBackup;
    public HashMap<Pair<Pair<Pair<Integer, Integer>, IASTNode>, ArrayList<AstBinding>>, ArrayList<IASTNode>> mergeCache = new HashMap<>();

    // all binding appear and it is not node.
    public ArrayList<IASTNode> mergeReferIAstName(ArrayList<AstBinding> bindings,
                                                  IASTNode node, int st, int ed) {
        Pair<Pair<Pair<Integer, Integer>, IASTNode>, ArrayList<AstBinding>> cacheKey = null;
        if (node != null) {
            cacheKey = new Pair<>(new Pair<>(new Pair<>(st, ed), node), bindings);
            ArrayList<IASTNode> res = mergeCache.get(cacheKey);
            if (res != null) {
                return res;
            }
        }

        HashMap<IASTNode, Integer> map = new HashMap<>();
        int count = 0;
        for (AstBinding binding : bindings) {
            if (binding.getIsLiteral()) {
                continue;
            }

            count++;
            ArrayList<IASTName> names = getAstReferences(binding.getName());
            HashMap<IASTNode, Boolean> visited = new HashMap<>();
            for (IASTName iastName : names) {
                IASTNode referStmt = StatementUtil.getStatementParent(iastName);
                if (referStmt.equals(node)) {
                    continue;
                }
                Integer idx = iAST2cAST.get(referStmt);
                if (idx == null) {
                    continue;
                }
                CASTNode castNode = nodeArrayList.get(idx);
                if (castNode.getFunctionId() != -1 && (idx < st || idx > ed)) {
                    continue;
                }

                Boolean hasVisit = visited.get(referStmt);
                if (hasVisit != null && hasVisit) {
                    continue;
                }
                visited.put(referStmt, true);

                Integer tmpValue = map.get(referStmt);
                if (tmpValue == null) {
                    tmpValue = 1;
                } else {
                    tmpValue++;
                }
                map.put(referStmt, tmpValue);
            }
        }

        ArrayList<IASTNode> beReferred = new ArrayList<>();
        Integer finalCount = count;
        map.forEach((key, value) -> {
            if (value.equals(finalCount)) {
                beReferred.add(key);
            }
        });
        if (cacheKey != null) {
            mergeCache.put(cacheKey, beReferred);
        }
        return beReferred;
    }

    private final Queue<ConcurrentTask> dfgBuffer = new ConcurrentLinkedDeque<>();

    private class DfgConsumer implements Runnable {
        @Override
        public void run() {
            while (true) {
                ConcurrentTask task = dfgBuffer.poll();
                if (task == null) {
                    continue;
                }
                if (task.getIsSystemFinish()) {
                    dbNodeBuffer.add(task);
                    System.out.println(filePath + " dfg exits at " + DebugUtil.getCurrentTime());
                    break;
                }

                int st = task.getSt();
                int ed = task.getEd();
                if (task.getIsFunctionFinish()) {
                    dbNodeBuffer.add(task);
                } else {
                    if (st == ed) {
                        CASTNode castNode = nodeArrayList.get(st);
                        handleDfgTask(castNode.getStmt().getIASTNode(), castNode);
                        if (task.getIsGlobalDecl()) {
                            dbNodeBuffer.add(task);
                        }
                    } else {
                        buildDfgByReachability(st, ed, st, ed);
                    }
                }
            }
        }
    }

    private final Queue<ConcurrentTask> dbNodeBuffer = new ConcurrentLinkedDeque<>();

    private class DbNodeConsumer implements Runnable {
        private int lastCommitIdx = -1;

        @Override
        public void run() {
            while (true) {
                ConcurrentTask task = dbNodeBuffer.poll();
                if (task == null) {
                    continue;
                }
                int st = task.getSt();
                int ed = task.getEd();

                // maybe the file is node Code stmt.
                if (st > ed && ed != -1) {
                    continue;
                }
                if (task.getIsSystemFinish()) {
                    GremlinUtil.addNode(null, true);
                    if (lastCommitIdx <= ed) {
                        task.setSt(lastCommitIdx + 1);
                    }
                    dbEdgeBuffer.add(task);
                    System.out.println(filePath + " node exits at " + DebugUtil.getCurrentTime());
                    break;
                }

                for (int i = st; i <= ed; i++) {
                    boolean hasInsert = GremlinUtil.addNode(nodeArrayList.get(i), false);
                    if (hasInsert) {
                        task.setSt(lastCommitIdx + 1);
                        task.setEd(i);
                        dbEdgeBuffer.add(task);
                        lastCommitIdx = i;
                    }
                }
            }
        }
    }

    private final Queue<ConcurrentTask> dbEdgeBuffer = new ConcurrentLinkedDeque<>();

    private class DbEdgeConsumer implements Runnable {
        @Override
        public void run() {
            while (true) {
                ConcurrentTask poll = dbEdgeBuffer.poll();
                if (poll == null) {
                    continue;
                }

                int st = poll.getSt();
                int ed = poll.getEd();

                for (int i = st; i <= ed; i++) {
                    GremlinUtil.addEdge(nodeArrayList.get(i), false);
                }

                if (poll.getIsSystemFinish()) {
                    GremlinUtil.addEdge(null, true);
                    System.out.println(filePath + " edge exits at " + DebugUtil.getCurrentTime());
                    break;
                }
            }
        }
    }
}