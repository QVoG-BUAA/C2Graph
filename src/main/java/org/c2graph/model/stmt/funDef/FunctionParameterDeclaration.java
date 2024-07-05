package org.c2graph.model.stmt.funDef;

import javafx.util.Pair;
import org.c2graph.model.JsonNode;
import org.c2graph.model.stmt.decl.DeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.json.JSONObject;

public class FunctionParameterDeclaration implements JsonNode {
    private JSONObject jsonObject;
    private IASTParameterDeclaration parameterDeclaration;
    private Boolean hasPointer;

    public Boolean getHasPointer() {
        return hasPointer;
    }

    public FunctionParameterDeclaration(IASTParameterDeclaration parameterDeclaration) {
        this.parameterDeclaration = parameterDeclaration;
        this.hasPointer = this.parameterDeclaration.getDeclarator().getPointerOperators().length > 0;
    }

    public Pair<IASTName, Boolean> getParamName() {
        return new Pair<>(this.parameterDeclaration.getDeclarator().getName(), this.hasPointer);
    }

    public JSONObject getParamType() {
        DeclSpecifier declSpecifier = new DeclSpecifier(this.parameterDeclaration.getDeclSpecifier());
        return declSpecifier.buildJson();
    }

    public JSONObject buildJson() {
        if (this.jsonObject != null) {
            return this.jsonObject;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("_type", "Name");
        jsonObject.put("id", getParamName().getKey().toString());

        jsonObject.put("type", getParamType());
        return this.jsonObject = jsonObject;
    }
}
