package org.c2graph.model.stmt;

import lombok.Data;
import org.c2graph.model.binding.AstBinding;
import org.c2graph.model.JsonNode;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.json.JSONObject;

import java.util.ArrayList;

@Data
public class FunctionExitStmt extends Statement implements JsonNode {
    public String funSig;
    public int functionDefId;
    public int functionDefGlobalId;

    private Boolean hasFunctionParamPointer = false;

    public FunctionExitStmt() {
        this.isExtern = false;
        this.isDefinition = true;
    }

    @Override
    public JSONObject buildJson() {
        if (this.jsonObject != null) {
            return this.jsonObject;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("_type", "FunExit");
        jsonObject.put("funSig", funSig);
        return this.jsonObject = jsonObject;
    }

    @Override
    public String getSignature() {
        return "Developer define Function exit.";
    }

    @Override
    public ArrayList<AstBinding> resolveVarRef() {
        return null;
    }

    @Override
    public IASTNode getIASTNode() {
        return null;
    }
}
