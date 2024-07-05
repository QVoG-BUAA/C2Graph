package org.c2graph.model.edge.impl;

import lombok.Data;
import org.c2graph.model.CASTNode;
import org.c2graph.model.edge.CASTEdge;

@Data
public class DfgEdge extends CASTEdge {
    public String symbol;
    public String defineOperationLike;

    public DfgEdge(String symbol, CASTNode node) {
        this.symbol = symbol;
        this.toNode = node;
        this.defineOperationLike = "none";
    }

    public DfgEdge(String symbol, CASTNode node, String defineOperationLike) {
        this.symbol = symbol;
        this.toNode = node;
        this.defineOperationLike = defineOperationLike;
    }
}
