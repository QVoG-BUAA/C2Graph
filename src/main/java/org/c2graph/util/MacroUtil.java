package org.c2graph.util;

import org.c2graph.model.stmt.*;
import org.c2graph.model.stmt.decl.DeclarationStatement;
import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.gnu.IGNUASTCompoundStatementExpression;

import java.util.ArrayList;

public class MacroUtil {
    public static ArrayList<IGNUASTCompoundStatementExpression> getMultiLineMacroUsage(Statement stmt) {
        if (stmt instanceof DeclarationStatement declStmt) {
            return declStmt.getMultiLineMacroUsage();
        } else if (stmt instanceof ExpressionStatement exprStmt) {
            return getMultiLineMacroUsage(exprStmt.getExpression());
        } else if (stmt instanceof IfStatement ifStmt) {
            return getMultiLineMacroUsage(ifStmt.getIastExpression());
        } else if (stmt instanceof ReturnStatement returnStmt) {
            return getMultiLineMacroUsage(returnStmt.getReturnStatement().getReturnValue());
        } else if (stmt instanceof SwitchStatement switchStmt) {
            return getMultiLineMacroUsage(switchStmt.getExpression());
        }
        return new ArrayList<>();
    }


    public static ArrayList<IGNUASTCompoundStatementExpression> getMultiLineMacroUsage(IASTExpression expression) {
        if (expression instanceof IGNUASTCompoundStatementExpression compoundStatementExpression) {
            ArrayList<IGNUASTCompoundStatementExpression> res = new ArrayList<>();
            res.add(compoundStatementExpression);
            return res;
        }
        if (expression instanceof IASTUnaryExpression unaryExpression)
            return getMultiLineMacroUsage(unaryExpression);
        if (expression instanceof IASTBinaryExpression binaryExpression)
            return getMultiLineMacroUsage(binaryExpression);
        if (expression instanceof IASTFunctionCallExpression functionCallExpression)
            return getMultiLineMacroUsage(functionCallExpression);
        if (expression instanceof IASTArraySubscriptExpression arraySubscriptExpression)
            return getMultiLineMacroUsage(arraySubscriptExpression);

        if (expression instanceof IASTCastExpression castExpression) {
            return getMultiLineMacroUsage(castExpression);
        }
        if (expression instanceof IASTFieldReference fieldReference) {
            return getMultiLineMacroUsage(fieldReference);
        }
        return new ArrayList<>();
    }

    public static ArrayList<IGNUASTCompoundStatementExpression>
    getMultiLineMacroUsage(IASTUnaryExpression unaryExpression) {
        return getMultiLineMacroUsage(unaryExpression.getOperand());
    }

    public static ArrayList<IGNUASTCompoundStatementExpression>
    getMultiLineMacroUsage(IASTBinaryExpression expression) {
        ArrayList<IGNUASTCompoundStatementExpression> res = new ArrayList<>();
        res.addAll(getMultiLineMacroUsage(expression.getOperand1()));
        res.addAll(getMultiLineMacroUsage(expression.getOperand2()));
        return res;
    }

    public static ArrayList<IGNUASTCompoundStatementExpression>
    getMultiLineMacroUsage(IASTFunctionCallExpression expression) {
        ArrayList<IGNUASTCompoundStatementExpression> res = new ArrayList<>();
        for (IASTInitializerClause argument : expression.getArguments()) {
            IASTExpression arg = (IASTExpression) argument;
            res.addAll(getMultiLineMacroUsage(arg));
        }
        return res;
    }

    public static ArrayList<IGNUASTCompoundStatementExpression>
    getMultiLineMacroUsage(IASTArraySubscriptExpression expression) {
        ArrayList<IGNUASTCompoundStatementExpression> res = new ArrayList<>();
        while (expression.getArrayExpression() instanceof IASTArraySubscriptExpression) {
            IASTExpression subscriptExpression = expression.getSubscriptExpression();
            res.addAll(getMultiLineMacroUsage(subscriptExpression));

            expression = (IASTArraySubscriptExpression) expression.getArrayExpression();
        }
        res.addAll(getMultiLineMacroUsage(expression.getSubscriptExpression()));
        res.addAll(getMultiLineMacroUsage(expression.getArrayExpression()));
        return res;
    }

    public static ArrayList<IGNUASTCompoundStatementExpression>
    getMultiLineMacroUsage(IASTCastExpression expression) {
        return getMultiLineMacroUsage(expression.getOperand());
    }

    public static ArrayList<IGNUASTCompoundStatementExpression>
    getMultiLineMacroUsage(IASTFieldReference expression) {
        return getMultiLineMacroUsage(expression.getFieldOwner());
    }
}
