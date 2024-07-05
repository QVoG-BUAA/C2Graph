package org.c2graph.model.stmt.loop;

import org.c2graph.model.binding.AstBinding;
import org.c2graph.model.JsonNode;
import org.c2graph.model.stmt.Statement;
import org.eclipse.cdt.core.dom.ast.IASTForStatement;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * unlike other {} statement, it is used to handle for(;;)
 */
public class ForStatement extends Statement implements JsonNode {
    private final IASTForStatement forStatement;

    public ForStatement(IASTForStatement forStatement) {
        super(forStatement);
        this.forStatement = forStatement;
    }

    @Override
    public String getSignature() {
        String sig = "for(";
        if (this.forStatement.getInitializerStatement() != null) {
            sig += this.forStatement.getInitializerStatement().getRawSignature();
        } else {
            sig += ";";
        }
        sig += ";";
        if (this.forStatement.getIterationExpression() != null) {
            sig += this.forStatement.getIterationExpression().getRawSignature() + ")";
        } else {
            sig += ")";
        }
        return sig;
    }

    @Override
    public ArrayList<AstBinding> resolveVarRef() {
        return new ArrayList<>();
    }

    @Override
    public IASTNode getIASTNode() {
        return this.forStatement;
    }

    @Override
    public JSONObject buildJson() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("_type", "EmptyFor");
        return jsonObject;
    }
}
