package org.c2graph.model.binding;

import org.eclipse.cdt.core.dom.ast.IASTName;

public record FunctionCallBinding(IASTName functionName, Boolean hasArgs) {}
