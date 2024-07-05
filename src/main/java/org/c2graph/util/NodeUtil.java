package org.c2graph.util;

import org.c2graph.config.ProjectConfig;
import org.c2graph.model.CASTNode;
import org.c2graph.model.stmt.GotoStatement;
import org.c2graph.model.stmt.ReturnStatement;
import org.c2graph.model.stmt.loop.BreakStatement;
import org.c2graph.model.stmt.loop.ContinueStatement;
import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.internal.core.dom.parser.cpp.*;

public class NodeUtil {
    public static final String EDGE_REVERSE = "_reverse_";
    public static final String DECL2USAGE = "decl2usage";

    public static int id = 0;

    public static Integer getCurrentId() {
        return id++;
    }

    public static Integer getLineno(IASTNode node) {
        if (node == null) {
            return -1;
        }
        return node.getFileLocation().getStartingLineNumber();
    }

    public static String getBelongFile(IASTNode node) {
        if (node == null) {
            return "-1";
        }
        ProjectConfig config = ProjectConfig.loadConfig();
        String containingFilename = node.getContainingFilename();
        String belongFile = containingFilename;
        if (containingFilename.startsWith(config.getProject())) {
            belongFile = containingFilename.substring(config.getProject().length());
        }
        belongFile = belongFile.substring(0, belongFile.lastIndexOf('.'));
        return belongFile;
    }

    public static boolean shouldNotAddDfgEmptyEdge(IASTNode fromNode) {
        return fromNode instanceof IASTBreakStatement
                || fromNode instanceof IASTContinueStatement
                || fromNode instanceof IASTGotoStatement
                || fromNode instanceof IASTLabelStatement
                || fromNode instanceof IASTNullStatement;
    }

    public static boolean shouldNotAddCfgEmptyEdge(IASTNode fromNode) {
        return fromNode instanceof IASTBreakStatement
                || fromNode instanceof IASTContinueStatement
                || fromNode instanceof IASTReturnStatement
                || fromNode instanceof IASTGotoStatement
                || fromNode instanceof IASTProblemStatement;
    }

    public static boolean shouldNotAddCfgEmptyEdge(CASTNode fromNode) {
        return fromNode.getStmt() instanceof BreakStatement
                || fromNode.getStmt() instanceof ContinueStatement
                || fromNode.getStmt() instanceof ReturnStatement
                || fromNode.getStmt() instanceof GotoStatement;
    }
}
