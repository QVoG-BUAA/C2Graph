package org.c2graph.model.stmt;

import org.c2graph.model.binding.AstBinding;
import org.c2graph.model.JsonNode;
import org.eclipse.cdt.core.dom.ast.IASTLabelStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.json.JSONObject;

import java.util.ArrayList;

public class LabelStatement extends Statement implements JsonNode {
    private final IASTLabelStatement labelStatement;

    public LabelStatement(IASTLabelStatement labelStatement) {
        super(labelStatement);
        this.labelStatement = labelStatement;
    }

    @Override
    public JSONObject buildJson() {
        if (this.jsonObject != null) {
            return this.jsonObject;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("_type", "label");
        jsonObject.put("label", this.labelStatement.getName());
        return this.jsonObject = jsonObject;
    }

    @Override
    public String getSignature() {
        return "label " + this.labelStatement.getName().toString();
    }

    @Override
    public ArrayList<AstBinding> resolveVarRef() {
        return null;
    }

    @Override
    public IASTNode getIASTNode() {
        return this.labelStatement;
    }
}
