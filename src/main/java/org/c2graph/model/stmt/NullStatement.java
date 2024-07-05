package org.c2graph.model.stmt;

import org.c2graph.model.JsonNode;
import org.c2graph.model.binding.AstBinding;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.json.JSONObject;

import java.util.ArrayList;

public class NullStatement extends Statement implements JsonNode {
    public NullStatement(IASTNode node) {
        super(node);
    }

    @Override
    public String getSignature() {
        return ";";
    }

    @Override
    public ArrayList<AstBinding> resolveVarRef() {
        return new ArrayList<>();
    }

    @Override
    public IASTNode getIASTNode() {
        return null;
    }

    @Override
    public JSONObject buildJson() {
        jsonObject = new JSONObject();
        jsonObject.put("_type", "null");
        return jsonObject;
    }
}
