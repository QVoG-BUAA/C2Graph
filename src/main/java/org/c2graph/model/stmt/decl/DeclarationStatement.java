package org.c2graph.model.stmt.decl;

import lombok.Data;
import org.c2graph.model.JsonNode;
import org.c2graph.model.binding.AstBinding;
import org.c2graph.model.stmt.Statement;
import org.c2graph.util.ExprUtil;
import org.c2graph.util.StatementUtil;
import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.gnu.IGNUASTCompoundStatementExpression;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

@Data
public class DeclarationStatement extends Statement implements JsonNode {
    private IASTSimpleDeclaration simpleDecl;
    private DeclSpecifier declSpecifier;
    private ArrayList<VarDeclarator> varDeclarators;

    /**
     * 左部变量定义
     */
    private ArrayList<String> vars;
    private String varType;

    private boolean isFunDecl;
    private boolean isFunPointer;
    private boolean hasArgs;
    private Boolean isParamHasPointer = false;

    private IASTName functionName;


    public ArrayList<IASTName> getTypeDeclAstName() {
        ArrayList<IASTName> names = new ArrayList<>();
        if (this.declSpecifier.getTypeASTName() != null) {
            names.add(this.declSpecifier.getTypeASTName());
        }
        if (this.declSpecifier.getIsEnum()) {
            HashMap<IASTName, IASTExpression> enumFields = this.declSpecifier.getEnumFields();
            enumFields.forEach((key, value) -> {
                names.add(key);
            });
        }
        return names;
    }

    public ArrayList<IGNUASTCompoundStatementExpression> getMultiLineMacroUsage() {
        ArrayList<IGNUASTCompoundStatementExpression> res = new ArrayList<>();
        for (VarDeclarator varDeclarator : varDeclarators) {
            res.addAll(varDeclarator.getMultiLineMacroUsage());
        }
        return res;
    }

    public DeclarationStatement(IASTDeclarationStatement stmt) {
        this(((IASTSimpleDeclaration) stmt.getDeclaration()), false);
    }

    public DeclarationStatement(IASTSimpleDeclaration declaration, boolean isGlobal) {
        super(declaration);

        this.simpleDecl = declaration;
        this.declSpecifier = new DeclSpecifier(this.simpleDecl.getDeclSpecifier());

        String rawSignature = this.declSpecifier.getSignature();
        this.isExtern = StatementUtil.
                checkIfIsExtern(rawSignature, isGlobal);
        if (isGlobal && !rawSignature.startsWith("static") && !rawSignature.startsWith("extern")) {
            this.isDefinition = true;
        }
        this.varType = declSpecifier.getType();

        this.varDeclarators = new ArrayList<>();
        for (IASTDeclarator declarator : simpleDecl.getDeclarators()) {
            if (declarator instanceof IASTStandardFunctionDeclarator funDecl) {
                this.isFunDecl = funDecl.getInitializer() == null;
                this.isFunPointer = funDecl.getInitializer() != null;

                this.hasArgs = funDecl.getParameters().length > 0;
                for (IASTParameterDeclaration parameter : funDecl.getParameters()) {
                    isParamHasPointer |= parameter.getDeclarator().getPointerOperators().length > 0;
                }
            }

            VarDeclarator varDeclarator = new VarDeclarator(declarator);
            if (varDeclarator.getInitializer() != null && this.isExtern) {
                this.isDefinition = true;
            }
            varDeclarators.add(varDeclarator);
        }
    }

    @Override
    public String getSignature() {
        if (this.declSpecifier.getIsEnum()) {
            return this.declSpecifier.getSignature();
        }
        if (isFunDecl) {
            String rawSignature = this.simpleDecl.getRawSignature();
            if (rawSignature.startsWith("extern")) {
                rawSignature = rawSignature.substring(rawSignature.indexOf(' ')).trim();
            }
            // ;
            rawSignature = rawSignature.substring(0, rawSignature.length() - 1);
            if (this.functionName == null) {
                return rawSignature;
            }
            return functionName.resolveBinding().toString();
        }
        StringBuilder decl = new StringBuilder();
        for (VarDeclarator varDeclarator : varDeclarators) {
            decl.append(varDeclarator.getSignature());
            decl.append(" ");
        }
        return this.varType + " " + decl;
    }

    @Override
    public ArrayList<AstBinding> resolveVarRef() {
        if (this.astBindings != null) {
            return this.astBindings;
        }
        this.astBindings = ExprUtil.resolveVarRef(this.simpleDecl);
        return this.astBindings;
    }

    @Override
    public IASTNode getIASTNode() {
        return this.simpleDecl;
    }

    public ArrayList<IASTName> getDeclVarName() {
        ArrayList<IASTName> list = new ArrayList<IASTName>();
        for (VarDeclarator varDeclarator : varDeclarators) {
            list.add(varDeclarator.getVarName());
        }
        return list;
    }

    public ArrayList<Integer> getDeclVarNamePointers() {
        ArrayList<Integer> list = new ArrayList<>();
        for (VarDeclarator varDeclarator : varDeclarators) {
            list.add(varDeclarator.getPointerLevels());
        }
        return list;
    }

    public JSONObject buildJson() {
        if (this.jsonObject != null) {
            return jsonObject;
        }
        JSONObject jsonObject = new JSONObject();
        if (this.declSpecifier.getIsNamedType()) {
            jsonObject.put("_type", "Call");
            jsonObject.put("args", new JSONArray());

            JSONObject functionNode = new JSONObject();
            functionNode.put("_type", "Name");
            functionNode.put("id", this.declSpecifier.getType());
            jsonObject.put("func", functionNode);
            return jsonObject;
        }
        if (this.declSpecifier.getIsTypeDef()) {
            jsonObject.put("_type", "Alias");
            jsonObject.put("name", this.declSpecifier.getType());
            ArrayList<AstBinding> astBindings = this.resolveVarRef();
            if (astBindings.size() > 1) {
                jsonObject.put("alias", astBindings.get(1).toString());
            } else {
                jsonObject.put("alias", "NULL");
            }
            return jsonObject;
        }
        if (this.varDeclarators.isEmpty()) {
            return this.declSpecifier.buildJson();
        }

        jsonObject.put("_type", "Decl");
        JSONArray targets = new JSONArray();
        for (int i = 0; i < varDeclarators.size(); i++) {
            VarDeclarator varDeclarator = varDeclarators.get(i);
            JSONObject declJson = varDeclarator.buildJson();
            targets.put(i, declJson);
        }
        jsonObject.put("targets", targets);
        jsonObject.put("scope", StatementUtil.getStorageClass(this.declSpecifier.getDeclSpecifier()));
        this.jsonObject = jsonObject;
        return jsonObject;
    }
}
