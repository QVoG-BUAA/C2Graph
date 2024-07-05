package org.c2graph.model.stmt;

import org.c2graph.model.binding.AstBinding;
import org.c2graph.model.JsonNode;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.json.JSONObject;

import java.util.ArrayList;

public class CompoundStatement extends Statement implements JsonNode {
    private final IASTCompoundStatement compoundStatement;

    public CompoundStatement(IASTCompoundStatement compoundStatement) {
        super(compoundStatement);
        this.compoundStatement = compoundStatement;
    }

    @Override
    public String getSignature() {
        return "Developer define empty compound stmt.";
    }

    @Override
    public ArrayList<AstBinding> resolveVarRef() {
        return null;
    }

    @Override
    public IASTNode getIASTNode() {
        return this.compoundStatement;
    }

    @Override
    public JSONObject buildJson() {
        if (this.jsonObject != null) {
            return this.jsonObject;
        }
        this.jsonObject = new JSONObject();
        jsonObject.put("_type", "EmptyBlock");
        return this.jsonObject;
    }
}
