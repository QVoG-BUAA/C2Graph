package org.c2graph.model;

import lombok.Data;
import org.c2graph.model.edge.impl.CfgEdge;
import org.c2graph.model.edge.impl.CgEdge;
import org.c2graph.model.edge.impl.DfgEdge;
import org.c2graph.model.stmt.Statement;
import org.c2graph.util.NodeUtil;

import java.util.concurrent.CopyOnWriteArrayList;

@Data
public class CASTNode {
    private int id;
    private int functionId = -1;
    private Statement stmt;

    private boolean isLastValidNode;
    private boolean hasMultiLineMacro;

    private CopyOnWriteArrayList<CgEdge> cgEdges;
    private CopyOnWriteArrayList<CfgEdge> cfgEdges;
    private CopyOnWriteArrayList<DfgEdge> dfgEdges;

    public CASTNode() {
        this.cgEdges = new CopyOnWriteArrayList<>();
        this.cfgEdges = new CopyOnWriteArrayList<>();
        this.dfgEdges = new CopyOnWriteArrayList<>();

        this.id = NodeUtil.getCurrentId();
    }

    public CASTNode(Statement stmt) {
        this.stmt = stmt;

        this.cgEdges = new CopyOnWriteArrayList<>();
        this.cfgEdges = new CopyOnWriteArrayList<>();
        this.dfgEdges = new CopyOnWriteArrayList<>();

        this.id = NodeUtil.getCurrentId();
    }

    public void addCgEdge(String data, CASTNode to) {
        CgEdge edge = new CgEdge(data, to);
        if (cgEdges.contains(edge)) {
            return;
        }
        cgEdges.add(edge);
    }

    public void addCfgActually(CfgEdge cfgEdge) {
        for (CfgEdge cfgEdge1 : cfgEdges) {
            if (cfgEdge.value.equals(cfgEdge1.value)
                    && cfgEdge.toNode.id == cfgEdge1.toNode.id) {
                return;
            }
        }
        this.cfgEdges.add(cfgEdge);
    }

    public void addCfgEdge(String value, CASTNode to) {
        to.addCfgActually(new CfgEdge(value, this));
    }

    public void addDfgEdge(String data, CASTNode toNode) {
        DfgEdge dfgEdge = new DfgEdge(data, toNode);
        if (hasDfgEdge(dfgEdge)) {
            return;
        }
        this.dfgEdges.add(dfgEdge);
    }

    public void addDfgEdge(String data, CASTNode toNode, String functionCallName) {
        DfgEdge dfgEdge = new DfgEdge(data, toNode, functionCallName);
        if (hasDfgEdge(dfgEdge)) {
            return;
        }
        this.dfgEdges.add(dfgEdge);
    }

    public boolean hasDfgEdge(DfgEdge dfgEdge) {
        for (DfgEdge dfgEdge1 : dfgEdges) {
            if (dfgEdge.symbol.equals(dfgEdge1.symbol)
                    && dfgEdge.toNode.id == dfgEdge1.toNode.id
                    && dfgEdge.defineOperationLike.equals(dfgEdge1.defineOperationLike)) {
                return true;
            }
        }
        return false;
    }

    public String getSignature() {
        return this.stmt.getSignature().trim();
    }

    @Override
    public int hashCode() {
        return this.id;
    }
}
