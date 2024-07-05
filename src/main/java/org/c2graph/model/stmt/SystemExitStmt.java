package org.c2graph.model.stmt;

import org.c2graph.model.binding.AstBinding;
import org.c2graph.model.JsonNode;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Developer define System exit.
 */
public class SystemExitStmt extends Statement implements JsonNode {
    public int functionDefId;
    public int functionDefGlobalId;
    public SystemExitStmt() {
        this.isExtern = false;
        this.isDefinition = true;
    }

    @Override
    public JSONObject buildJson() {
        if (this.jsonObject != null) {
            return this.jsonObject;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("_type", "SysExit");
        return this.jsonObject = jsonObject;
    }

    @Override
    public String getSignature() {
        return "Developer define System exit.";
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
