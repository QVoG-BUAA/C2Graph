package org.c2graph.model.stmt;


import lombok.Data;
import org.c2graph.model.binding.AstBinding;
import org.c2graph.model.JsonNode;
import org.c2graph.util.ExprUtil;
import org.c2graph.util.JsonUtil;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.IASTSwitchStatement;
import org.json.JSONObject;

import java.util.ArrayList;

@Data
public class SwitchStatement extends Statement implements JsonNode {
    private IASTSwitchStatement switchStatement;
    private IASTExpression expression;

    public SwitchStatement(IASTExpression expression) {
        super(expression);
        this.expression = expression;
    }

    @Override
    public String getSignature() {
        return "switch " + this.expression.getRawSignature();
    }

    @Override
    public ArrayList<AstBinding> resolveVarRef() {
        if (this.astBindings != null) {
            return this.astBindings;
        }
        return this.astBindings = ExprUtil.resolveVarRef(this.expression);
    }

    @Override
    public IASTNode getIASTNode() {
        return this.expression;
    }

    @Override
    public JSONObject buildJson() {
        if (this.jsonObject != null) {
            return this.jsonObject;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("_type", "Switch");
        jsonObject.put("operand", JsonUtil.getExprJson(this.expression));
        return this.jsonObject = jsonObject;
    }
}
