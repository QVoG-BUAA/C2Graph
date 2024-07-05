package org.c2graph.model.stmt;

import org.c2graph.model.binding.AstBinding;
import org.c2graph.model.JsonNode;
import org.eclipse.cdt.core.dom.ast.IASTGotoStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.json.JSONObject;

import java.util.ArrayList;

public class GotoStatement extends Statement implements JsonNode {
    private IASTGotoStatement gotoStatement;

    public GotoStatement(IASTGotoStatement gotoStatement) {
        super(gotoStatement);
        this.gotoStatement = gotoStatement;
    }

    @Override
    public JSONObject buildJson() {
        if (this.jsonObject != null) {
            return this.jsonObject;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("_type", "goto");
        jsonObject.put("label", this.gotoStatement.getName());
        return this.jsonObject = jsonObject;
    }

    @Override
    public String getSignature() {
        return this.gotoStatement.getRawSignature();
    }

    @Override
    public ArrayList<AstBinding> resolveVarRef() {
        return null;
    }

    @Override
    public IASTNode getIASTNode() {
        return this.gotoStatement;
    }
}
