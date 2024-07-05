package org.c2graph.util;

import org.c2graph.model.binding.FunctionCallBinding;
import org.c2graph.util.debug.DebugUtil;
import org.eclipse.cdt.core.dom.ast.*;

import java.util.ArrayList;

public class FunctionCallUtil {
    public static ArrayList<FunctionCallBinding> resolveFunRef(IASTNode astNode) {
        if (astNode == null) {
            return new ArrayList<>();
        }
        ArrayList<FunctionCallBinding> functionCallBindings = new ArrayList<>();
        if (astNode instanceof IASTExpression expression) {
            if (expression instanceof IASTFunctionCallExpression functionCallExpression) {
                IASTName functionName = getFunctionNameIastName(functionCallExpression);
                if (functionName == null) {
                    return functionCallBindings;
                }
                IASTInitializerClause[] arguments = functionCallExpression.getArguments();
                FunctionCallBinding functionCallBinding = new FunctionCallBinding(functionName,
                        arguments.length > 0);
                functionCallBindings.add(functionCallBinding);
                for (IASTInitializerClause argument : arguments) {
                    functionCallBindings.addAll(resolveFunRef(argument));
                }
            } else if (expression instanceof IASTCastExpression castExpression) {
                return resolveFunRef(castExpression.getOperand());
            } else if (expression instanceof IASTUnaryExpression unaryExpression) {
                return resolveFunRef(unaryExpression.getOperand());
            } else if (expression instanceof IASTBinaryExpression binaryExpression) {
                functionCallBindings.addAll(resolveFunRef(binaryExpression.getOperand1()));
                functionCallBindings.addAll(resolveFunRef(binaryExpression.getOperand2()));
            } else if (expression instanceof IASTArraySubscriptExpression arraySubscriptExpression) {
                functionCallBindings.addAll(resolveFunRef(arraySubscriptExpression.getArrayExpression()));
                functionCallBindings.addAll(resolveFunRef(arraySubscriptExpression.getSubscriptExpression()));
            } else if (expression instanceof IASTConditionalExpression conditionalExpression) {
                functionCallBindings.addAll(resolveFunRef(conditionalExpression.getLogicalConditionExpression()));
                functionCallBindings.addAll(resolveFunRef(conditionalExpression.getPositiveResultExpression()));
                functionCallBindings.addAll(resolveFunRef(conditionalExpression.getNegativeResultExpression()));
            } else if (expression instanceof IASTExpressionList list) {
                for (IASTExpression listExpression : list.getExpressions()) {
                    functionCallBindings.addAll(resolveFunRef(listExpression));
                }
            }
        }
        else if (astNode instanceof IASTExpressionStatement expressionStatement) {
            functionCallBindings.addAll(resolveFunRef(expressionStatement.getExpression()));
        } else if (astNode instanceof IASTDeclarationStatement declarationStatement) {
            functionCallBindings.addAll(resolveFunRef(declarationStatement.getDeclaration()));
        } else if (astNode instanceof IASTDeclaration declaration) {
            if (declaration instanceof IASTSimpleDeclaration simpleDeclaration) {
                for (IASTDeclarator declarator : simpleDeclaration.getDeclarators()) {
                    functionCallBindings.addAll(resolveFunRef(declarator));
                }
            } else {
                System.out.println("FunctionUtil decl unimplemented: " + declaration);
            }
        } else if (astNode instanceof IASTDeclarator declarator) {
            IASTInitializer initializer = declarator.getInitializer();
            if (initializer != null) {
                for (IASTNode child : initializer.getChildren()) {
                    functionCallBindings.addAll(resolveFunRef(child));
                }
            }
        } else if (astNode instanceof IASTInitializerList list) {
            for (IASTInitializerClause clause : list.getClauses()) {
                functionCallBindings.addAll(resolveFunRef(clause));
            }
        }
        return functionCallBindings;
    }

    private static IASTName getFunctionNameIastName(IASTFunctionCallExpression functionCallExpression) {
        IASTExpression functionNameExpression = functionCallExpression.getFunctionNameExpression();
        IASTName functionName = null;
        if (functionNameExpression instanceof IASTIdExpression idExpression) {
            functionName = idExpression.getName();
        } else if (functionNameExpression instanceof IASTFieldReference fieldReference) {
            functionName = fieldReference.getFieldName();
        } else if (functionNameExpression instanceof IASTUnaryExpression unaryExpression) {
            IASTExpression copy = unaryExpression;
            while (copy instanceof IASTUnaryExpression) {
                copy = ((IASTUnaryExpression) copy).getOperand();
            }
            if (copy instanceof IASTIdExpression idExpression) {
                functionName = idExpression.getName();
            } else if (copy instanceof IASTFieldReference fieldReference) {
                functionName = fieldReference.getFieldName();
            }
        }
        else {
            DebugUtil.printNodeInfo(functionCallExpression);
            // throw new RuntimeException("functionUtil unknown functionNameExpr");
        }
        return functionName;
    }
}