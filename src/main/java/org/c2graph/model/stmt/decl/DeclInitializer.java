package org.c2graph.model.stmt.decl;

import lombok.Data;
import org.c2graph.model.JsonNode;
import org.c2graph.util.JsonUtil;
import org.c2graph.util.exceptions.IllegalException;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTInitializerList;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.core.dom.ast.gnu.IGNUASTCompoundStatementExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTCompoundStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTCompoundStatementExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTInitializerList;
import org.json.JSONObject;

import java.util.ArrayList;

@Data
public class DeclInitializer implements JsonNode {
    private JSONObject jsonObject;
    private IASTInitializer initializer;
    private ArrayList<IGNUASTCompoundStatementExpression> multiLineMacros = new ArrayList<>();

    public DeclInitializer(IASTInitializer initializer) {
        this.initializer = initializer;
        assert this.initializer.getChildren().length == 1;
        for (IASTNode child : this.initializer.getChildren()) {
            if (child instanceof IGNUASTCompoundStatementExpression expr) {
                multiLineMacros.add(expr);
            }
        }
    }

    public JSONObject buildJson() {
        if (this.jsonObject != null) {
            return jsonObject;
        }
        if (initializer.getChildren().length > 1) {
            System.out.println("child too much.");
        }

        for (IASTNode child : initializer.getChildren()) {
            if (child instanceof IASTExpression) {
                this.jsonObject = JsonUtil.getExprJson(((IASTExpression) child));
            } else if (child instanceof IASTInitializerList initializerList) {
                this.jsonObject = JsonUtil.getInitializerListJson(initializerList);
            } else {
                System.out.println(child + " has not transferred to json yet.");
            }
        }
        // TODO complete all types
        this.jsonObject.put("category", "equal");
        return this.jsonObject;
    }
}
