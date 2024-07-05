package org.c2graph.model.stmt.decl;

import lombok.Data;
import org.c2graph.model.JsonNode;
import org.c2graph.util.JsonUtil;
import org.eclipse.cdt.core.dom.ast.IASTArrayModifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.gnu.IGNUASTCompoundStatementExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTArrayDeclarator;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTCompoundStatementExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTDeclarator;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

@Data
public class VarDeclarator implements JsonNode {
    private JSONObject jsonObject;
    private IASTDeclarator declarator;

    private IASTName varName;
    private DeclInitializer initializer;

    public int getPointerLevels() {
        return this.declarator.getPointerOperators().length;
    }

    public ArrayList<IGNUASTCompoundStatementExpression> getMultiLineMacroUsage() {
        if (this.initializer == null) {
            return new ArrayList<>();
        }
        return this.initializer.getMultiLineMacros();
    }

    public VarDeclarator(IASTDeclarator declarator) {
        this.declarator = declarator;

        this.varName = declarator.getName();
        if (declarator.getInitializer() != null) {
            this.initializer = new DeclInitializer(declarator.getInitializer());
        } else {
            this.initializer = null;
        }
    }

    public String getSignature() {
        String signature = this.declarator.getRawSignature();
        while (signature.startsWith("(*")) {
            signature = signature.substring(1);
        }
        return signature;
    }


    public JSONObject buildJson() {
        if (this.jsonObject != null) {
            return this.jsonObject;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("_type", "Name");

        // TODO sync with exprUtil
        jsonObject.put("id", varName.toString());
        if (this.declarator instanceof CPPASTArrayDeclarator arrayDeclarator) {
            JSONArray jsonArray = new JSONArray();

            IASTArrayModifier[] arrayModifiers = arrayDeclarator.getArrayModifiers();
            for (int i = 0; i < arrayModifiers.length; i++) {
                jsonArray.put(i, JsonUtil.getExprJson(arrayModifiers[i].getConstantExpression()));
            }

            jsonObject.put("arrayMod", jsonArray);
        }
        if (this.initializer != null) {
            jsonObject.put("value", this.initializer.buildJson());
        }

        this.jsonObject = jsonObject;
        return jsonObject;
    }
}
