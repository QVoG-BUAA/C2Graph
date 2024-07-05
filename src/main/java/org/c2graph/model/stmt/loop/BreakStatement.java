package org.c2graph.model.stmt.loop;

import org.c2graph.model.binding.AstBinding;
import org.c2graph.model.JsonNode;
import org.c2graph.model.stmt.Statement;
import org.eclipse.cdt.core.dom.ast.IASTBreakStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.json.JSONObject;

import java.util.ArrayList;

public class BreakStatement extends Statement implements JsonNode {
    private final IASTBreakStatement breakStatement;

    public BreakStatement(IASTBreakStatement breakStatement) {
        super(breakStatement);
        this.breakStatement = breakStatement;
    }

    @Override
    public String getSignature() {
        return "break";
    }

    @Override
    public ArrayList<AstBinding> resolveVarRef() {
        return null;
    }

    @Override
    public IASTNode getIASTNode() {
        return this.breakStatement;
    }

    @Override
    public JSONObject buildJson() {
        if (this.jsonObject != null) {
            return this.jsonObject;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("_type", "Break");
        return this.jsonObject = jsonObject;
    }
}
