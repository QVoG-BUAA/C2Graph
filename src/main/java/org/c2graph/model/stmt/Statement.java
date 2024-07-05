package org.c2graph.model.stmt;

import lombok.Data;
import org.c2graph.model.binding.AstBinding;
import org.c2graph.util.NodeUtil;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * 对标 Neo4j 节点的公共属性
 */
@Data
public abstract class Statement {
    /**
     * handle the case of var and fun decl with `extern` or `static`
     */
    public boolean isExtern;
    public boolean isDefinition;

    private Integer lineno;
    private String belongFile;
    private String functionDefName;

    protected JSONObject jsonObject;
    protected ArrayList<AstBinding> astBindings;

    /**
     * 返回 code
     * @return Statement 对应的 code
     */
    public abstract String getSignature();

    public abstract ArrayList<AstBinding> resolveVarRef();

    public abstract IASTNode getIASTNode();

    public Statement() {
    }

    public Statement(IASTNode node) {
        this.setLineno(NodeUtil.getLineno(node));
        this.setBelongFile(NodeUtil.getBelongFile(node));
    }

    public abstract JSONObject buildJson();
}
