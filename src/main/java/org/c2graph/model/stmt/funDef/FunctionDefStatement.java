package org.c2graph.model.stmt.funDef;

import javafx.util.Pair;
import lombok.Data;
import org.c2graph.model.JsonNode;
import org.c2graph.model.binding.AstBinding;
import org.c2graph.model.stmt.Statement;
import org.c2graph.model.stmt.decl.DeclSpecifier;
import org.c2graph.util.StatementUtil;
import org.eclipse.cdt.core.dom.ast.*;
import org.json.JSONObject;

import java.util.ArrayList;

@Data
public class FunctionDefStatement extends Statement implements JsonNode {
    private IASTFunctionDefinition functionDefinition;
    private DeclSpecifier functionDeclSpecifier;
    private FunctionDeclarator functionDeclarator;

    private String returnType;
    private ArrayList<Integer> returnIds;

    public FunctionDefStatement(IASTFunctionDefinition definition) {
        super(definition);

        this.returnIds = new ArrayList<>();
        this.functionDefinition = definition;

        IASTDeclSpecifier declSpecifier = functionDefinition.getDeclSpecifier();
        this.functionDeclSpecifier = new DeclSpecifier(declSpecifier);

        this.isExtern = StatementUtil.checkIfIsExtern(this.functionDeclSpecifier.getSignature(), true);
        this.returnType = StatementUtil.getDeclType(functionDeclSpecifier);

        IASTFunctionDeclarator declarator = functionDefinition.getDeclarator();
        this.functionDeclarator = new FunctionDeclarator(declarator);

        if (this.functionDefinition.getBody() != null) {
            this.isDefinition = true;
        }
    }

    public IASTName getFunctionName() {
        return this.functionDeclarator.getFunctionName();
    }

    public ArrayList<Pair<IASTName, Boolean>> getParamsName() {
        return this.functionDeclarator.getParamsName();
    }


    public Boolean isParamHasPointer() {
        return this.functionDeclarator.isParamHasPointer();
    }

    public JSONObject buildJson() {
        if (this.jsonObject != null) {
            return this.jsonObject;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("_type", "FunctionDef");
        jsonObject.put("args", this.functionDeclarator.buildJson());

        JSONObject func = new JSONObject();
        func.put("_type", "Name");
        func.put("id", this.getFunctionName().toString());

        JSONObject typeObj = this.functionDeclSpecifier.buildJson();
        int pointerLevel = this.functionDeclarator.getDeclarator().getPointerOperators().length;
        typeObj.put("pointerLevel", pointerLevel);
        jsonObject.put("type", typeObj);

        // only cpp has
//        func.put("isConst", this.functionDeclarator.getDeclarator().isConst());
        jsonObject.put("func", func);
        jsonObject.put("scope", StatementUtil.getStorageClass(this.functionDeclSpecifier.getDeclSpecifier()));

        return this.jsonObject = jsonObject;
    }

    @Override
    public String getSignature() {
        return this.getFunctionName().resolveBinding().toString();
    }

    @Override
    public ArrayList<AstBinding> resolveVarRef() {
        if (this.astBindings != null) {
            return this.astBindings;
        }
        IASTStandardFunctionDeclarator declarator = this.functionDeclarator.getDeclarator();
        ArrayList<AstBinding> names = new ArrayList<>();
        for (IASTParameterDeclaration parameter : declarator.getParameters()) {
            IASTDeclarator declarator1 = parameter.getDeclarator();
            IASTName name = declarator1.getName();
            names.add(new AstBinding(name, null));
        }
        return this.astBindings = names;
    }

    @Override
    public IASTNode getIASTNode() {
        return this.functionDefinition;
    }
}
