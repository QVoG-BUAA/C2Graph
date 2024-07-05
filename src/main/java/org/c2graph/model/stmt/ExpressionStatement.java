package org.c2graph.model.stmt;

import lombok.Data;
import org.c2graph.model.binding.AstBinding;
import org.c2graph.model.JsonNode;
import org.c2graph.util.ExprUtil;
import org.c2graph.util.JsonUtil;
import org.eclipse.cdt.core.dom.ast.*;
import org.json.JSONObject;

import java.util.ArrayList;

@Data
public class ExpressionStatement extends Statement implements JsonNode {
    private String var;
    private ArrayList<AstBinding> lvalues;
    private Boolean isLvaluePointer;
    private IASTExpression expression;

    public ExpressionStatement() {
        this.isLvaluePointer = false;
    }

    public ExpressionStatement(IASTExpression expression) {
        super(expression);

        this.expression = expression;
        this.isLvaluePointer = false;
        this.lvalues = extractLvalue();
    }

    public ExpressionStatement(IASTExpressionStatement expr) {
        super(expr);

        this.isLvaluePointer = false;
        this.expression = expr.getExpression();
        this.lvalues = extractLvalue();
    }

    @Override
    public String getSignature() {
        if (this.expression == null) {
            return "null";
        }
        return this.expression.getRawSignature();
    }

    @Override
    public ArrayList<AstBinding> resolveVarRef() {
        if (this.astBindings != null) {
            return this.astBindings;
        }
        ArrayList<AstBinding> names = ExprUtil.resolveVarRef(this.expression);

//        // TODO ? handleDecl2UsageDfgTask fix this.
//        if (this.isLvaluePointer) {
//            names.addAll(lvalues);
//        }
        return this.astBindings = names;
    }

    @Override
    public IASTNode getIASTNode() {
        return this.expression;
    }

    public ArrayList<AstBinding> extractLvalue() {
        if (this.lvalues != null) {
            return this.lvalues;
        }
        if (!(expression instanceof IASTBinaryExpression binaryExpr)) {
            return null;
        }
        int operator = binaryExpr.getOperator();
        if (!ExprUtil.isAssign(operator)) {
            return null;
        }

        // 处理连等
        ArrayList<AstBinding> names = new ArrayList<>();
        IASTExpression copy = binaryExpr;
        while (copy instanceof IASTBinaryExpression binaryExpression) {
            operator = binaryExpression.getOperator();
            if (!ExprUtil.isAssign(operator)) {
                return names;
            }

            IASTExpression leftExpr = binaryExpression.getOperand1();
            names.addAll(ExprUtil.resolveVarRef(leftExpr));
            copy = binaryExpression.getOperand2();
        }
        return names;
    }

    public JSONObject buildJson() {
        if (this.jsonObject != null) {
            return this.jsonObject;
        }
        return this.jsonObject = JsonUtil.getExprJson(expression);
    }
}
