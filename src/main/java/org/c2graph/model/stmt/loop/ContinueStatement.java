package org.c2graph.model.stmt.loop;

import org.c2graph.model.binding.AstBinding;
import org.c2graph.model.JsonNode;
import org.c2graph.model.stmt.Statement;
import org.eclipse.cdt.core.dom.ast.IASTContinueStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.json.JSONObject;

import java.util.ArrayList;

public class ContinueStatement extends Statement implements JsonNode {
    private final IASTContinueStatement continueStatement;

    public ContinueStatement(IASTContinueStatement continueStatement) {
        super(continueStatement);
        this.continueStatement = continueStatement;
    }

    @Override
    public String getSignature() {
        return "continue";
    }

    @Override
    public ArrayList<AstBinding> resolveVarRef() {
        return null;
    }

    @Override
    public IASTNode getIASTNode() {
        return this.continueStatement;
    }

    @Override
    public JSONObject buildJson() {
        if (this.jsonObject != null) {
            return this.jsonObject;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("_type", "Continue");
        return this.jsonObject = jsonObject;
    }
}
