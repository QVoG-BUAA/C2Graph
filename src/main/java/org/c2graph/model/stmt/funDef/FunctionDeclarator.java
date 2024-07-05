package org.c2graph.model.stmt.funDef;

import javafx.util.Pair;
import lombok.Data;
import org.c2graph.model.JsonArrayNode;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTStandardFunctionDeclarator;
import org.json.JSONArray;

import java.util.ArrayList;

@Data
public class FunctionDeclarator implements JsonArrayNode {
    private JSONArray jsonArray;
    private IASTStandardFunctionDeclarator declarator;
    private ArrayList<FunctionParameterDeclaration> parameterDeclarationArrayList;

    public FunctionDeclarator(IASTFunctionDeclarator declarator) {
        this.declarator = (IASTStandardFunctionDeclarator) declarator;

        this.parameterDeclarationArrayList = new ArrayList<>();
        IASTParameterDeclaration[] parameters = this.declarator.getParameters();
        for (IASTParameterDeclaration parameter : parameters) {
            this.parameterDeclarationArrayList.add(new FunctionParameterDeclaration(parameter));
        }
    }

    public String getSignature() {
        return declarator.getRawSignature();
    }

    public IASTName getFunctionName() {
        return declarator.getName();
    }

    public Boolean isParamHasPointer() {
        for (FunctionParameterDeclaration functionParameterDeclaration : parameterDeclarationArrayList) {
            if (functionParameterDeclaration.getHasPointer()) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<Pair<IASTName, Boolean>> getParamsName() {
        ArrayList<Pair<IASTName, Boolean>> list = new ArrayList<>();
        for (FunctionParameterDeclaration functionParameterDeclaration : parameterDeclarationArrayList) {
            list.add(functionParameterDeclaration.getParamName());
        }
        return list;
    }

    public JSONArray buildJson() {
        if (this.jsonArray != null) {
            return this.jsonArray;
        }
        JSONArray args = new JSONArray();
        for (int i = 0; i < parameterDeclarationArrayList.size(); i++) {
            FunctionParameterDeclaration decl = parameterDeclarationArrayList.get(i);
            args.put(i, decl.buildJson());
        }
        this.jsonArray = args;
        return this.jsonArray;
    }
}
