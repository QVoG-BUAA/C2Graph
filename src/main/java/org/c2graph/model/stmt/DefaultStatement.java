package org.c2graph.model.stmt;

import org.c2graph.model.binding.AstBinding;
import org.c2graph.model.JsonNode;
import org.eclipse.cdt.core.dom.ast.IASTDefaultStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.json.JSONObject;

import java.util.ArrayList;

public class DefaultStatement extends Statement implements JsonNode {
    private IASTDefaultStatement defaultStatement;

    public DefaultStatement(IASTDefaultStatement defaultStatement) {
        super(defaultStatement);
        this.defaultStatement = defaultStatement;
    }

    @Override
    public String getSignature() {
        return "default";
    }

    @Override
    public ArrayList<AstBinding> resolveVarRef() {
        return null;
    }

    @Override
    public IASTNode getIASTNode() {
        return this.defaultStatement;
    }

    @Override
    public JSONObject buildJson() {
        if (this.jsonObject != null) {
            return this.jsonObject;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("_type", "Default");
        return this.jsonObject = jsonObject;
    }
}
