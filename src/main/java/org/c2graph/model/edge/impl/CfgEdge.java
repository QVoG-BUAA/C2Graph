package org.c2graph.model.edge.impl;

import lombok.Data;
import org.c2graph.model.CASTNode;
import org.c2graph.model.edge.CASTEdge;

import java.util.Objects;


@Data
public class CfgEdge extends CASTEdge {
    public static final String TRUE = "TRUE";
    public static final String FALSE = "FALSE";
    public static final String EMPTY = "EMPTY";

    /**
     * true/ false/ empty
     */
    public String value;

    public CfgEdge(String value, CASTNode to) {
        this.value = value;
        this.toNode = to;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CfgEdge cfgEdge) {
            return this.value.equals(cfgEdge.value) && this.toNode.equals(cfgEdge.toNode);
        }
        return false;
    }
}
