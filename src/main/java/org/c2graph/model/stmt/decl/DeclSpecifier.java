package org.c2graph.model.stmt.decl;

import lombok.Data;
import org.c2graph.model.JsonNode;
import org.c2graph.util.JsonUtil;
import org.c2graph.util.StatementUtil;
import org.c2graph.util.debug.DebugUtil;
import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.c.ICASTTypedefNameSpecifier;
import org.eclipse.osgi.internal.debug.Debug;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Data
public class DeclSpecifier implements JsonNode {
    private JSONObject jsonObject;

    private IASTDeclSpecifier declSpecifier;
    private String signature;

    private Boolean isEnum = false;
    private HashMap<IASTName, IASTExpression> enumFields;

    private Boolean isConst = false;
    private Boolean isSimple = false;
    private Boolean isComplex = false;
    private Boolean isTypeDef = false;

    // TODO it seems like a function call
    private Boolean isNamedType = false;

    private String type;
    private IASTName typeASTName;
    private ArrayList<MemberField> fields;

    public DeclSpecifier(IASTDeclSpecifier declSpecifier) {
        this.declSpecifier = declSpecifier;
        this.signature = this.declSpecifier.getRawSignature();

        this.fields = new ArrayList<>();
        this.enumFields = new HashMap<>();
        if (declSpecifier instanceof IASTSimpleDeclSpecifier) {
            this.isSimple = true;
            this.type = StatementUtil.getDeclType(this);
        } else if (declSpecifier instanceof IASTNamedTypeSpecifier) {
            String value = this.declSpecifier.getRawSignature();
            // FIXME
            if (value.endsWith("srand") || value.equals("time")) {
                this.isNamedType = true;
            } else {
                this.isSimple = true;
            }

            this.type = value;
            if (declSpecifier instanceof ICASTTypedefNameSpecifier typedefNameSpecifier) {
                this.typeASTName = typedefNameSpecifier.getName();
            }
        } else if (declSpecifier instanceof IASTCompositeTypeSpecifier compositeTypeSpecifier) {
            this.isComplex = true;
            this.type = this.declSpecifier.getRawSignature();
            this.typeASTName = compositeTypeSpecifier.getName();
            for (IASTDeclaration member : compositeTypeSpecifier.getMembers()) {
                if (member instanceof IASTProblemDeclaration) {
                    DebugUtil.printNodeInfo(member, "problem decl in declSpecifier");
                    continue;
                }
                this.fields.addAll(extractField(member));
            }
        } else if (declSpecifier instanceof IASTElaboratedTypeSpecifier elaboratedTypeSpecifier) {
            // it seems like it only has this situation
            if (signature.startsWith("typedef")) {
                this.isTypeDef = true;
                this.type = signature.substring("typedef".length()).trim();
            } else {
                this.isSimple = true;
                this.type = signature.trim();
            }
            this.typeASTName = elaboratedTypeSpecifier.getName();
        } else if (declSpecifier instanceof IASTEnumerationSpecifier enumerationSpecifier) {
            this.isEnum = true;
            this.typeASTName = enumerationSpecifier.getName();
            for (IASTEnumerationSpecifier.IASTEnumerator enumerator : enumerationSpecifier.getEnumerators()) {
                // TODO we only care about the idExpr
                IASTExpression value = enumerator.getValue();
                this.enumFields.put(enumerator.getName(), value);
            }
        }
    }

    public ArrayList<MemberField> extractField(IASTDeclaration declaration) {
        DeclarationStatement declarationStatement = new DeclarationStatement((IASTSimpleDeclaration) declaration, false);

        ArrayList<MemberField> memberFields = new ArrayList<>();

        ArrayList<IASTName> declVarName = declarationStatement.getDeclVarName();
        ArrayList<Integer> pointerLevels = declarationStatement.getDeclVarNamePointers();
        for (int i = 0; i < declVarName.size(); i++) {
            IASTName varName = declVarName.get(i);
            Integer varPointerLevel = pointerLevels.get(i);
            MemberField memberField = new MemberField(varName.toString(), varPointerLevel, ((IASTSimpleDeclaration) declaration).getDeclSpecifier());
            memberFields.add(memberField);
        }
        return memberFields;
    }

    /**
     * build type
     *
     * @return 无需考虑 typedef 的情况
     */
    @Override
    public JSONObject buildJson() {
        if (this.jsonObject != null) {
            return this.jsonObject;
        }
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("_type", "Type");

        jsonObject.put("isEnum", this.isEnum);
        jsonObject.put("isSimple", this.isSimple);
        jsonObject.put("isComplex", this.isComplex);

        if (this.isSimple || this.isComplex) {
            if (this.type.startsWith("const")) {
                this.isConst = true;
                this.type = this.type.substring("const".length()).trim();
            }
            jsonObject.put("type", this.type);
        }
        jsonObject.put("isConst", this.isConst);

        JSONArray jsonArray = new JSONArray();
        if (this.isComplex) {
            for (int i = 0; i < fields.size(); i++) {
                jsonArray.put(i, fields.get(i).buildJson());
            }
            jsonObject.put("fields", jsonArray);
        } else if (this.isEnum) {
            int i = 0;
            for (Map.Entry<IASTName, IASTExpression> stringObjectEntry : enumFields.entrySet()) {
                JSONObject jsonObject1 = new JSONObject();
                jsonObject1.put("key", stringObjectEntry.getKey());
                jsonObject1.put("value", JsonUtil.getExprJson(stringObjectEntry.getValue()));
                jsonArray.put(i, jsonObject1);
                i++;
            }
            jsonObject.put("fields", jsonArray);
        } else {
            jsonObject.put("fields", new JSONArray());
        }
        this.jsonObject = jsonObject;
        return jsonObject;
    }
}
