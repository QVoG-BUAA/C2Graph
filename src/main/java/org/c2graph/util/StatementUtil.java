package org.c2graph.util;

import org.c2graph.model.stmt.decl.DeclSpecifier;
import org.eclipse.cdt.core.dom.ast.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class StatementUtil {
    public static final String EXTERN = "extern";
    public static final String STATIC = "static";

    // TODO, is seems useless
    public static String getStorageClass(IASTDeclSpecifier declSpecifier) {
        int storageClass = declSpecifier.getStorageClass();
        return switch (storageClass) {
            case IASTDeclSpecifier.sc_static -> "static";
            case IASTDeclSpecifier.sc_extern -> "extern";
            default -> "none";
        };
    }

    public static boolean checkIfIsExtern(String signature, boolean isGlobal) {
        if (signature.startsWith(EXTERN)) {
            return true;
        }
        if (signature.startsWith(STATIC)) {
            return false;
        }
        return isGlobal;
    }

    public static String getDeclType(DeclSpecifier declSpecifier) {
        String raw = declSpecifier.getSignature().trim();
        if (raw.startsWith(EXTERN)) {
            raw = raw.substring(EXTERN.length()).trim();
        } else if (raw.startsWith(STATIC)) {
            raw = raw.substring(STATIC.length()).trim();
        } else if (raw.startsWith("const")) {
            raw = raw.substring("const".length()).trim();
        }
        return raw;
    }

    public static boolean checkStatementType(IASTNode node) {
        if (node instanceof IASTStatement) {
            return true;
        }
        if (node instanceof IASTFunctionDefinition) {
            return true;
        }
        return false;
    }

    public static boolean checkEnvLikeStmt(IASTNode node) {
        if (node instanceof IASTIfStatement)
            return true;
        if (node instanceof IASTWhileStatement)
            return true;
        if (node instanceof IASTSwitchStatement)
            return true;
        if (node instanceof IASTDoStatement)
            return true;
        if (node instanceof IASTForStatement)
            return true;

        return false;
    }

    public static HashMap<IASTNode, IASTNode> stmtCache = new HashMap<>();

    public static IASTNode getStatementParent(IASTNode node) {
        IASTNode cacheValue = stmtCache.get(node);
        if (cacheValue != null) {
            return cacheValue;
        }
        boolean isStmt = checkStatementType(node);
        if (isStmt || node instanceof IASTSimpleDeclaration
                && !(node.getParent() instanceof IASTDeclarationStatement)
                && !(node.getParent() instanceof IASTCompositeTypeSpecifier)) {
            stmtCache.put(node, node);
            return node;
        }
        // node 作为 stmt 的 cond/ initial/ iter
        if (checkEnvLikeStmt(node.getParent())
                || node.getParent() instanceof IASTTranslationUnit
                || node.getParent() instanceof IASTExpressionStatement
                || node.getParent() instanceof IASTDeclarationStatement) {
            stmtCache.put(node, node);
            return node;
        }
        IASTNode res = getStatementParent(node.getParent());
        stmtCache.put(node, res);
        return res;
    }

    public static HashMap<IASTNode, ArrayList<IASTFunctionCallExpression>> callCache = new HashMap<>();

    public static ArrayList<IASTFunctionCallExpression> getFunctionCallParent(IASTNode node) {
        if (callCache.get(node) != null) {
            return callCache.get(node);
        }
        ArrayList<IASTFunctionCallExpression> functionCalls = new ArrayList<>();
        node.accept(new ASTVisitor() {
            {
                shouldVisitExpressions = true;
            }

            @Override
            public int visit(IASTExpression expression) {
                if (expression instanceof IASTFunctionCallExpression functionCall) {
                    functionCalls.add(functionCall);
                }
                return PROCESS_CONTINUE;
            }
        });
        callCache.put(node, functionCalls);
        return functionCalls;
    }
}
