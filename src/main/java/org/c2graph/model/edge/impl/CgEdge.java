package org.c2graph.model.edge.impl;

import lombok.Data;
import org.c2graph.model.CASTNode;
import org.c2graph.model.edge.CASTEdge;

@Data
public class CgEdge extends CASTEdge {
    private String data;
    public CgEdge(String data, CASTNode node) {
        this.data = data;
        this.toNode = node;
    }
}
