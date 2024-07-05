package org.c2graph.model.stmt.loop;

import org.c2graph.model.binding.AstBinding;
import org.c2graph.model.JsonNode;
import org.c2graph.model.stmt.Statement;
import org.c2graph.util.ExprUtil;
import org.c2graph.util.JsonUtil;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.json.JSONObject;

import java.util.ArrayList;

public class DoWhileStatement extends Statement implements JsonNode {
    private IASTExpression condition;

    public DoWhileStatement(IASTExpression condition) {
        super(condition);
        this.condition = condition;
    }

    @Override
    public String getSignature() {
        return "doWhile: " + this.condition.getRawSignature();
    }

    @Override
    public ArrayList<AstBinding> resolveVarRef() {
        if (this.astBindings != null) {
            return this.astBindings;
        }
        return this.astBindings = ExprUtil.resolveVarRef(condition);
    }

    @Override
    public IASTNode getIASTNode() {
        return this.condition;
    }

    @Override
    public JSONObject buildJson() {
        if (this.jsonObject != null) {
            return this.jsonObject;
        }
        JSONObject exprJson = JsonUtil.getExprJson(this.condition);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("_type", "If");
        jsonObject.put("operand", exprJson);
        return this.jsonObject = jsonObject;
    }
}
