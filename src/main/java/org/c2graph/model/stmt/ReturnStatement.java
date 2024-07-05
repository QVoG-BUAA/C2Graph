package org.c2graph.model.stmt;

import lombok.Data;
import org.c2graph.model.binding.AstBinding;
import org.c2graph.model.JsonNode;
import org.c2graph.util.ExprUtil;
import org.c2graph.util.JsonUtil;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTReturnStatement;
import org.json.JSONObject;

import java.util.ArrayList;

@Data
public class ReturnStatement extends Statement implements JsonNode {
    private final IASTReturnStatement returnStatement;

    public ReturnStatement(IASTReturnStatement returnStatement) {
        super(returnStatement);
        this.returnStatement = returnStatement;
    }

    @Override
    public String getSignature() {
        return returnStatement.getRawSignature();
    }

    @Override
    public ArrayList<AstBinding> resolveVarRef() {
        if (this.astBindings != null) {
            return this.astBindings;
        }
        return this.astBindings = ExprUtil.resolveVarRef(this.returnStatement.getReturnValue());
    }

    @Override
    public IASTNode getIASTNode() {
        return this.returnStatement;
    }

    @Override
    public JSONObject buildJson() {
        if (this.jsonObject != null) {
            return this.jsonObject;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("_type", "Return");

        IASTExpression returnValue = this.returnStatement.getReturnValue();
        JSONObject literalRet;
        if (returnValue != null) {
            literalRet = JsonUtil.getExprJson(returnValue);
        } else {
            literalRet = JsonUtil.getIdExprJson("Void");
        }
        jsonObject.put("value", literalRet);
        return this.jsonObject = jsonObject;
    }
}
