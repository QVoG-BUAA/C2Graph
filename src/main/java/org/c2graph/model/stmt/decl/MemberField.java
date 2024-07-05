package org.c2graph.model.stmt.decl;

import org.c2graph.model.JsonNode;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.json.JSONObject;

public class MemberField
        implements JsonNode {
    private final String name;
    private final Integer pointerLevel;
    private final IASTDeclSpecifier declSpecifier;

    public MemberField(String name, Integer pointerLevel, IASTDeclSpecifier declSpecifier) {
        this.name = name;
        this.pointerLevel = pointerLevel;
        this.declSpecifier = declSpecifier;
    }

    private JSONObject jsonObject;

    @Override
    public JSONObject buildJson() {
        if (jsonObject != null) {
            return jsonObject;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("_type", "field");

        DeclSpecifier specifier = new DeclSpecifier(declSpecifier);
        JSONObject object = specifier.buildJson();
        object.put("pointerLevel", this.pointerLevel);

        jsonObject.put("type", object);
        jsonObject.put("name", name);
        this.jsonObject = jsonObject;
        return jsonObject;
    }
}