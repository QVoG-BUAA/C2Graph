package org.c2graph.model.stmt;

import lombok.Data;
import org.c2graph.model.binding.AstBinding;
import org.c2graph.model.JsonNode;
import org.c2graph.util.ExprUtil;
import org.c2graph.util.JsonUtil;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.json.JSONObject;

import java.util.ArrayList;

@Data
public class IfStatement extends Statement implements JsonNode {

    private ArrayList<String> varRefs;
    private IASTExpression iastExpression;

    public IfStatement(IASTExpression condition) {
        super(condition);
        this.iastExpression = condition;
    }

    @Override
    public String getSignature() {
        return "if: " + this.iastExpression.getRawSignature();
    }

    @Override
    public ArrayList<AstBinding> resolveVarRef() {
        if (this.astBindings != null) {
            return this.astBindings;
        }
        return this.astBindings = ExprUtil.resolveVarRef(this.iastExpression);
    }

    @Override
    public IASTNode getIASTNode() {
        return this.iastExpression;
    }

    @Override
    public JSONObject buildJson() {
        if (this.jsonObject != null) {
            return this.jsonObject;
        }
        JSONObject exprJson = JsonUtil.getExprJson(this.iastExpression);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("_type", "If");
        jsonObject.put("operand", exprJson);
        return this.jsonObject = jsonObject;
    }
}
