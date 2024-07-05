package org.c2graph.model.stmt;

import org.c2graph.model.binding.AstBinding;
import org.c2graph.model.JsonNode;
import org.c2graph.util.JsonUtil;
import org.eclipse.cdt.core.dom.ast.IASTCaseStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.json.JSONObject;

import java.util.ArrayList;

public class CaseStatement extends Statement implements JsonNode {
    private IASTCaseStatement caseStatement;

    public CaseStatement(IASTCaseStatement caseStatement) {
        super(caseStatement);
        this.caseStatement = caseStatement;
    }

    @Override
    public String getSignature() {
        return caseStatement.getRawSignature();
    }

    @Override
    public ArrayList<AstBinding> resolveVarRef() {
        return null;
    }

    @Override
    public IASTNode getIASTNode() {
        return this.caseStatement;
    }

    @Override
    public JSONObject buildJson() {
        if (this.jsonObject != null) {
            return this.jsonObject;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("_type", "Case");
        JSONObject exprJson = JsonUtil.getExprJson(caseStatement.getExpression());
        jsonObject.put("value", exprJson);
        return this.jsonObject = jsonObject;
    }
}
