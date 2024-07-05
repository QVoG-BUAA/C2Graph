package org.c2graph.util;

import org.c2graph.model.binding.AstBinding;
import org.c2graph.model.CASTNode;
import org.c2graph.model.stmt.ExpressionStatement;
import org.c2graph.model.stmt.Statement;
import org.c2graph.util.debug.AnsiColors;
import org.c2graph.util.debug.DebugUtil;
import org.c2graph.util.exceptions.IllegalException;
import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.gnu.IGNUASTCompoundStatementExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTDeleteExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTInitializerList;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTNewExpression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class ExprUtil {
    // compare the left part
    public static boolean checkIfAssignLeftV(ArrayList<AstBinding> refBindings, CASTNode castNode) {
        Statement parent = castNode.getStmt();
        if (parent instanceof ExpressionStatement expressionStatement) {
            IASTExpression expression = expressionStatement.getExpression();
            // FIXME this api is wired, it seems everything is true
//            if (!expression.isLValue()) {
//                return false;
//            }
            if (!(expression instanceof IASTBinaryExpression binaryExpression)) {
                return false;
            }
            if (!ExprUtil.isAssign(binaryExpression.getOperator())) {
                return false;
            }

            ArrayList<AstBinding> lvalues = expressionStatement.getLvalues();
            if (lvalues == null) {
                return false;
            }
            // a[i][j] = 1
            // ... = a[i + 1][j + 1], just a left, should be forbidden
            // a[i][j] = 1 vs a[i][k] = 1
            if (refBindings.size() < refBindings.get(0).getInitialSize()
                    && lvalues.size() > refBindings.size()) {
                return false;
            }
            // yes
            // a[i][j] = 1
            // .. = a[i]
            // yes
            // a[i] = 1
            // .. = a[i][j]

            int i;
            for (i = 0; i < Math.min(refBindings.size(), lvalues.size()); i++) {
                AstBinding astBinding = lvalues.get(lvalues.size() - 1 - i);
                AstBinding ref = refBindings.get(refBindings.size() - 1 - i);
                if (!astBinding.toString().equals(ref.toString())) {
                    break;
                }
                String otherOperation = astBinding.getOtherOperation();

                String otherOperation1 = ref.getOtherOperation();
                if (otherOperation1 == null && otherOperation == null) {
                    continue;
                }
                if (otherOperation == null || otherOperation1 == null) {
                    break;
                }

                if (Objects.equals(otherOperation1, otherOperation)) {
                    if (otherOperation1.startsWith("_") || otherOperation1.endsWith("_")) {
                        break;
                    }
                } else if (!(otherOperation.startsWith("_") && otherOperation1.endsWith("_"))) {
                    break;
                }
            }
            return i == Math.min(refBindings.size(), lvalues.size());
        }
        return false;
    }

    public static boolean checkIfSelfOperation(ArrayList<AstBinding> astBindings, IASTNode node) {
        ArrayList<AstBinding> refBindings = checkIfSelfOperation(node);
        if (astBindings == null && refBindings != null || astBindings != null && refBindings == null) {
            return false;
        }
        assert refBindings != null;
        if (refBindings.size() != astBindings.size()) {
            return false;
        }
        for (int i = 0; i < refBindings.size(); i++) {
            if (!Objects.equals(refBindings.get(i).toString(), astBindings.get(i).toString())) {
                return false;
            }
        }
        return true;
    }

    public static HashMap<IASTNode, ArrayList<AstBinding>> selfCache = new HashMap<>();
    public static ArrayList<AstBinding> checkIfSelfOperation(IASTNode node) {
        ArrayList<AstBinding> astBindings1 = selfCache.get(node);
        if (astBindings1 != null) {
            return astBindings1;
        }
        if (node instanceof IASTDeclarationStatement declarationStatement) {
            IASTDeclaration declaration = declarationStatement.getDeclaration();
            if (declaration instanceof IASTSimpleDeclaration simpleDeclaration) {
                ArrayList<AstBinding> astBindings = new ArrayList<>();
                for (IASTDeclarator declarator : simpleDeclaration.getDeclarators()) {
                    IASTInitializer initializer = declarator.getInitializer();
                    if (initializer instanceof IASTExpression expression) {
                        astBindings.addAll(checkIfSelfOperation(expression));
                    }
                }
                selfCache.put(node, astBindings);
                return astBindings;
            }
        }
        if (node instanceof IASTExpressionStatement expressionStatement) {
            return checkIfSelfOperation(expressionStatement.getExpression());
        }
        if (node instanceof IASTExpression expression) {
            return checkIfSelfOperation(expression);
        }
        return new ArrayList<>();
    }

    public static ArrayList<AstBinding> checkIfSelfOperation(IASTExpression expression) {
        ArrayList<AstBinding> astBindings1 = selfCache.get(expression);
        if (astBindings1 != null) {
            return astBindings1;
        }
        ArrayList<AstBinding> astBindings = new ArrayList<>();
        if (expression instanceof IASTUnaryExpression unaryExpression) {
            String unaryOperator = getUnaryOperatorById(unaryExpression.getOperator());
            if (unaryOperator.startsWith("_") || unaryOperator.endsWith("_")) {
                return resolveVarRef(unaryExpression);
            }
        } else if (expression instanceof IASTBinaryExpression binaryExpression) {
            astBindings.addAll(checkIfSelfOperation(binaryExpression.getOperand1()));
            astBindings.addAll(checkIfSelfOperation(binaryExpression.getOperand2()));
        } else if (expression instanceof IASTFunctionCallExpression functionCallExpression) {
            for (IASTInitializerClause argument : functionCallExpression.getArguments()) {
                if (argument instanceof IASTExpression expression1) {
                    astBindings.addAll(checkIfSelfOperation(expression1));
                }
            }
        } else if (expression instanceof IASTCastExpression castExpression) {
            return checkIfSelfOperation(castExpression.getOperand());
        } else if (expression instanceof IASTArraySubscriptExpression arraySubscriptExpression) {
            astBindings.addAll(checkIfSelfOperation(arraySubscriptExpression.getArrayExpression()));
            astBindings.addAll(checkIfSelfOperation(arraySubscriptExpression.getSubscriptExpression()));
        }
        selfCache.put(expression, astBindings);
        return astBindings;
    }

    public static String getBinaryOperandById(int kind) {
        return switch (kind) {
            case IASTBinaryExpression.op_multiply -> "*";
            case IASTBinaryExpression.op_divide -> "/";
            case IASTBinaryExpression.op_modulo -> "%";
            case IASTBinaryExpression.op_plus -> "+";
            case IASTBinaryExpression.op_minus -> "-";
            case IASTBinaryExpression.op_shiftLeft -> "<<";
            case IASTBinaryExpression.op_shiftRight -> ">>";
            case IASTBinaryExpression.op_lessThan -> "<";
            case IASTBinaryExpression.op_greaterThan -> ">";
            case IASTBinaryExpression.op_lessEqual -> "<=";
            case IASTBinaryExpression.op_greaterEqual -> ">=";
            case IASTBinaryExpression.op_binaryAnd -> "&&";
            case IASTBinaryExpression.op_binaryXor -> "^";
            case IASTBinaryExpression.op_binaryOr -> "||";
            case IASTBinaryExpression.op_logicalAnd -> "&";
            case IASTBinaryExpression.op_logicalOr -> "|";
            case IASTBinaryExpression.op_assign -> "=";
            case IASTBinaryExpression.op_multiplyAssign -> "*=";
            case IASTBinaryExpression.op_divideAssign -> "/=";
            case IASTBinaryExpression.op_moduloAssign -> "%=";
            case IASTBinaryExpression.op_plusAssign -> "+=";
            case IASTBinaryExpression.op_minusAssign -> "-=";
            case IASTBinaryExpression.op_shiftLeftAssign -> "<<=";
            case IASTBinaryExpression.op_shiftRightAssign -> ">>=";
            case IASTBinaryExpression.op_binaryAndAssign -> "&=";
            case IASTBinaryExpression.op_binaryXorAssign -> "^=";
            case IASTBinaryExpression.op_binaryOrAssign -> "|=";
            case IASTBinaryExpression.op_equals -> "==";
            case IASTBinaryExpression.op_notequals -> "!=";
            default -> throw new IllegalException(IllegalException.NOT_IMPLEMENT_YET + "Binary operator " + kind);
        };
    }

    public static String getUnaryOperatorById(int kind) {
        return switch (kind) {
            case IASTUnaryExpression.op_prefixDecr -> "_--";
            case IASTUnaryExpression.op_prefixIncr -> "_++";
            case IASTUnaryExpression.op_plus -> "+";
            case IASTUnaryExpression.op_minus -> "-";
            case IASTUnaryExpression.op_star -> "*";
            case IASTUnaryExpression.op_amper -> "&";
            case IASTUnaryExpression.op_tilde -> "~";
            case IASTUnaryExpression.op_not -> "!";
            case IASTUnaryExpression.op_sizeof -> "sizeof";
            case IASTUnaryExpression.op_postFixIncr -> "++_";
            case IASTUnaryExpression.op_postFixDecr -> "--_";
            case IASTUnaryExpression.op_bracketedPrimary -> "()";
            default -> throw new IllegalException(IllegalException.NOT_IMPLEMENT_YET + "Unary operator:" + kind);
        };
    }

    // cache cause e v ranges
    public static ArrayList<AstBinding> resolveVarRef(IASTExpression expression) {
        ArrayList<AstBinding> astBindings = resolveVarRef(expression, false);
//        astBindings.get(0).addValue(1);
//        astBindings.get(astBindings.size() - 1).addValue(-1);
        return astBindings;
    }

    public static ArrayList<AstBinding> resolveVarRef(IASTExpression expression, boolean isInCollection) {
        if (expression == null || expression instanceof IASTProblemExpression) {
            return new ArrayList<>();
        }
        if (expression instanceof IASTTypeIdExpression) {
            return new ArrayList<>();
        }

        if (expression instanceof IASTIdExpression idExpression) {
            return resolveVarRef(idExpression, isInCollection);
        }
        if (expression instanceof IASTLiteralExpression literal) {
            return resolveVarRef(literal, isInCollection);
        }
        if (expression instanceof IASTCastExpression castExpression) {
            return resolveVarRef(castExpression.getOperand(), isInCollection);
        }
        if (expression instanceof IASTUnaryExpression unaryExpression) {
            return resolveVarRef(unaryExpression, isInCollection);
        }
        if (expression instanceof IASTBinaryExpression binaryExpression) {
            return resolveVarRef(binaryExpression, isInCollection);
        }
        if (expression instanceof IASTFunctionCallExpression functionCallExpression) {
            return resolveVarRef(functionCallExpression, isInCollection);
        }
        if (expression instanceof IASTArraySubscriptExpression arraySubscriptExpression) {
            return resolveVarRef(arraySubscriptExpression, isInCollection);
        }
        if (expression instanceof IASTFieldReference fieldReference) {
            return resolveVarRef(fieldReference, isInCollection);
        }
        if (expression instanceof IASTConditionalExpression conditionalExpression) {
            return resolveVarRef(conditionalExpression, isInCollection);
        }
        if (expression instanceof IASTExpressionList list) {
            return resolveVarRef(list, isInCollection);
        }
        if (expression instanceof CPPASTNewExpression newExpr) {
            return resolveVarRef(newExpr, isInCollection);
        }
        if (expression instanceof CPPASTDeleteExpression deleteExpr) {
            return resolveVarRef(deleteExpr, isInCollection);
        }
        if (expression instanceof IGNUASTCompoundStatementExpression compoundStatementExpression) {
            return resolveVarRef(compoundStatementExpression, isInCollection);
        }
        if (expression instanceof IASTTypeIdInitializerExpression typeIdInitializerExpression) {
            return resolveVarRef(typeIdInitializerExpression, isInCollection);
        }
        System.out.println(AnsiColors.ANSI_RED + "there is something expr type unknown: " + expression
                + AnsiColors.ANSI_RESET);
        return new ArrayList<>();
    }

    public static ArrayList<AstBinding> resolveVarRef(IASTTypeIdInitializerExpression expr, boolean isInCollection) {
        ArrayList<AstBinding> astBindings = resolveVarRef(expr.getTypeId().getAbstractDeclarator());
        astBindings.addAll(resolveVarRef((IASTInitializerClause) expr.getInitializer()));
        return new ArrayList<>();
    }

    // the compound must have returnValue, it will be id expr definitely
    public static ArrayList<AstBinding> resolveVarRef(IGNUASTCompoundStatementExpression expr, boolean isInCollection) {
        ArrayList<AstBinding> astBindings = new ArrayList<>();
        IASTStatement[] statements = expr.getCompoundStatement().getStatements();
        IASTStatement lastStmt = statements[statements.length - 1];
        if (lastStmt instanceof IASTExpressionStatement exprStmt) {
            astBindings.addAll(resolveVarRef(exprStmt.getExpression(), isInCollection));
        }
        return astBindings;
    }

    public static ArrayList<AstBinding> resolveVarRef(CPPASTNewExpression expr, boolean isInCollection) {
        return resolveVarRef(expr.getTypeId().getAbstractDeclarator());
    }

    public static ArrayList<AstBinding> resolveVarRef(CPPASTDeleteExpression expr, boolean isInCollection) {
        return resolveVarRef(expr.getOperand(), isInCollection);
    }

    public static ArrayList<AstBinding> resolveVarRef(IASTExpressionList list, boolean isInCollection) {
        ArrayList<AstBinding> astBindings = new ArrayList<>();
        for (IASTExpression expression : list.getExpressions()) {
            astBindings.addAll(resolveVarRef(expression, isInCollection));
        }
        return astBindings;
    }

    public static ArrayList<AstBinding> resolveVarRef(IASTLiteralExpression literalExpression , boolean isInCollection) {
        ArrayList<AstBinding> names = new ArrayList<>();
        AstBinding astBinding = new AstBinding(literalExpression.toString(), literalExpression);
        astBinding.setIsLiteral(true);
        names.add(astBinding);
        return names;
    }

    public static ArrayList<AstBinding> resolveVarRef(IASTConditionalExpression conditionalExpression, boolean isInCollection) {
        ArrayList<AstBinding> names = new ArrayList<>();
        names.addAll(resolveVarRef(conditionalExpression.getLogicalConditionExpression(), isInCollection));
        names.add(new AstBinding("?", null));
        names.addAll(resolveVarRef(conditionalExpression.getNegativeResultExpression(), isInCollection));
        names.add(new AstBinding(":", null));
        names.addAll(resolveVarRef(conditionalExpression.getPositiveResultExpression(), isInCollection));
        return names;
    }

    public static ArrayList<AstBinding> resolveVarRef(IASTFieldReference expression, boolean isInCollection) {
        ArrayList<AstBinding> names = new ArrayList<>();

        AstBinding astBinding = new AstBinding(expression.getFieldName(), 1, expression);
        names.add(astBinding);

        int i = 1;
        while (expression.getFieldOwner() instanceof IASTFieldReference fieldOwner) {
            i ++;
            names.add(new AstBinding(fieldOwner.getFieldName(), 1, expression));
            expression = fieldOwner;
        }

        names.addAll(resolveVarRef(expression.getFieldOwner(), isInCollection));
        names.get(names.size() - 1).addValue(-i);
        return names;
    }

    public static ArrayList<AstBinding> resolveVarRef(IASTArraySubscriptExpression expression, boolean isInCollection) {
        ArrayList<AstBinding> astBindings = new ArrayList<>();

        IASTExpression subscriptExpression = expression.getSubscriptExpression();
        astBindings.addAll(resolveVarRef(subscriptExpression, true));

        int i = 1;
        while (expression.getArrayExpression() instanceof IASTArraySubscriptExpression arraySubscriptExpression) {
            i ++;
            astBindings.addAll(resolveVarRef(arraySubscriptExpression, true));
            astBindings.remove(astBindings.size() - 1);
            expression = arraySubscriptExpression;
        }

        astBindings.addAll(resolveVarRef(expression.getArrayExpression(), isInCollection));
        astBindings.get(0).addValue(1);
        astBindings.get(astBindings.size() - 1).addValue(-i);
        return astBindings;

//        int lastSize = 0;
//        if (!names.isEmpty()) {
//            lastSize = names.size();
//            names.get(0).addValue(1);
//            names.get(0).setIsCollection(true);
//            names.get(names.size() - 1).addValue(-1);
//        }
//        names.addAll(resolveVarRef(expression.getArrayExpression(), isInCollection));
//        if (names.size() != lastSize) {
//            for (int i = lastSize; i < names.size(); i ++) {
//                AstBinding astBinding = names.get(i);
//                if (i == lastSize && lastSize == 0) {
//                    astBinding.addValue(1);
//                }
//                if (i == names.size() - 1) {
//                    astBinding.addValue(-1);
//                }
//                astBinding.setIsCollection(true);
//            }
//        }
//        return names;
    }

    public static ArrayList<AstBinding> resolveVarRef(IASTIdExpression expression, boolean isInCollection) {
        IASTName name = expression.getName();
        ArrayList<AstBinding> names = new ArrayList<>();
        names.add(new AstBinding(name, expression));
        return names;
    }

    public static ArrayList<AstBinding> resolveVarRef(IASTUnaryExpression expression, boolean isInCollection) {
        ArrayList<AstBinding> astBindings = new ArrayList<>();
        String operator = getUnaryOperatorById(expression.getOperator());

        ArrayList<AstBinding> operandList = resolveVarRef(expression.getOperand(), isInCollection);
        if (isInCollection && !operandList.isEmpty()) {
            operandList.get(operandList.size() - 1).setOtherOperation(operator);
        }
        astBindings.addAll(operandList);

        return astBindings;
    }

    public static boolean isAssign(int operator) {
        return operator == IASTBinaryExpression.op_assign ||
                operator == IASTBinaryExpression.op_multiplyAssign ||
                operator == IASTBinaryExpression.op_divideAssign ||
                operator == IASTBinaryExpression.op_moduloAssign ||
                operator == IASTBinaryExpression.op_plusAssign ||
                operator == IASTBinaryExpression.op_minusAssign ||
                operator == IASTBinaryExpression.op_shiftLeftAssign ||
                operator == IASTBinaryExpression.op_shiftRightAssign ||
                operator == IASTBinaryExpression.op_binaryAndAssign ||
                operator == IASTBinaryExpression.op_binaryXorAssign ||
                operator == IASTBinaryExpression.op_binaryOrAssign;
    }

    public static ArrayList<AstBinding> resolveVarRef(IASTBinaryExpression expression, boolean isInCollection) {
        ArrayList<AstBinding> names = new ArrayList<>();
        if (!isAssign(expression.getOperator())) {
            names.addAll(resolveVarRef(expression.getOperand1(), isInCollection));
        }
        else {
            //handledecl2usage fix this
//            IASTExpression leftOp = expression.getOperand1();
//            // *p, p should be record
//            if (leftOp instanceof IASTUnaryExpression unary) {
//                names.addAll(resolveVarRef(unary, false));
//            } else if (leftOp instanceof IASTArraySubscriptExpression array) {
//                // a[i][j] = , we should not care a, but i j
//                names.addAll(resolveVarRef(array));
//                if (names.size() > 1) {
//                    names.remove(names.size() - 1);
//                    names.get(names.size() - 1).addValue(-1);
//                } else if (names.size() == 1) {
//                    names.remove(0);
//                }
//            }
        }
        if (isInCollection && !names.isEmpty()) {
            names.get(names.size() - 1).setOtherOperation(getBinaryOperandById(expression.getOperator()));
        }
        names.addAll(resolveVarRef(expression.getOperand2(), isInCollection));
        return names;
    }

    public static ArrayList<AstBinding> resolveVarRef(IASTFunctionCallExpression expression, boolean isInCollection) {
        ArrayList<AstBinding> names = new ArrayList<>();
//        names.add(new AstBinding(String.valueOf(expression.hashCode()), null));

        IASTInitializerClause[] arguments = expression.getArguments();
        for (IASTInitializerClause argument : arguments) {
            if (argument instanceof IASTExpression) {
                names.addAll(resolveVarRef(((IASTExpression) argument)));
            } else {
                System.out.println(AnsiColors.ANSI_RED +  "this type of parameter has not been implemented yet:");
                DebugUtil.printNodeInfo(argument, "not implemented yet" + AnsiColors.ANSI_RESET);
            }
        }
        String functionCallName;
        IASTExpression functionNameExpression = expression.getFunctionNameExpression();
        if (functionNameExpression instanceof IASTIdExpression idExpression) {
            functionCallName = idExpression.getName().toString();
        } else {
            // TODO it must be the functionCall used by pointer
            functionCallName = "function pointer call";
//            DebugUtil.printNodeInfo(functionNameExpression, "function pointer call");
//            throw new IllegalException("function name is not id expression: " + functionNameExpression);
        }

        for (AstBinding name : names) {
            name.setDefineOperationLike(functionCallName);
        }
        if (isInCollection) {
            for (AstBinding name : names) {
                name.setOtherOperation(functionCallName);
            }
        }
        return names;
    }

    public static ArrayList<AstBinding> resolveVarRef(IASTDeclarator declarator) {
        ArrayList<AstBinding> names = new ArrayList<>();
        if (declarator instanceof IASTArrayDeclarator arrayDeclarator) {
            for (IASTArrayModifier modifier : arrayDeclarator.getArrayModifiers()) {
                names.addAll(resolveVarRef(modifier.getConstantExpression()));
            }
        }

        IASTInitializer initializer = declarator.getInitializer();
        if (initializer != null) {
            for (IASTNode child : initializer.getChildren()) {
                if (child instanceof IASTExpression) {
                    names.addAll(resolveVarRef(((IASTExpression) child)));
                } else if (child instanceof IASTInitializerList list) {
                    names.addAll(resolveVarRef(list));
                } else {
                    System.out.println(AnsiColors.ANSI_RED +  "decl initializer in expr not implement yet:");
                    DebugUtil.printNodeInfo(child, "not implemented yet" + AnsiColors.ANSI_RESET);
                }
            }
        }
        return names;
    }

    public static ArrayList<AstBinding> resolveVarRef(IASTInitializerClause clause) {
        ArrayList<AstBinding> names = new ArrayList<>();
        if (clause instanceof CPPASTInitializerList list) {
            names.addAll(resolveVarRef(list));
        } else if (clause instanceof IASTExpression expr) {
            names.addAll(resolveVarRef(expr));
        }
        return names;
    }

    public static ArrayList<AstBinding> resolveVarRef(IASTInitializerList list) {
        ArrayList<AstBinding> names = new ArrayList<>();
        for (IASTInitializerClause clause : list.getClauses()) {
            names.addAll(resolveVarRef(clause));
        }
        return names;
    }

    public static ArrayList<AstBinding> resolveVarRef(IASTSimpleDeclaration declaration) {
        ArrayList<AstBinding> names = new ArrayList<>();

        // type-dfg has already been added in complex type decl.

//        IASTDeclSpecifier declSpecifier = declaration.getDeclSpecifier();
//        if (declSpecifier instanceof CPPASTElaboratedTypeSpecifier elaboratedTypeSpecifier) {
//            names.add(new AstBinding(elaboratedTypeSpecifier.getName()));
//            IASTDeclarator[] declarators = declaration.getDeclarators();
//            for (IASTDeclarator declarator : declarators) {
//                names.add(new AstBinding(declarator.getName()));
//            }
//        }

        IASTDeclarator[] declarators = declaration.getDeclarators();
        for (IASTDeclarator declarator : declarators) {
            names.addAll(resolveVarRef(declarator));
        }
        return names;
    }

    public static ArrayList<AstBinding> resolveVarRef(IASTDeclarationStatement stmt) {
        IASTDeclaration declaration = stmt.getDeclaration();
        if (declaration instanceof IASTSimpleDeclaration simpleDeclaration) {
            return resolveVarRef(simpleDeclaration);
        } else {
            System.out.println(AnsiColors.ANSI_RED +  "the type of decl in resolveVarRef is unrecognized:");
            DebugUtil.printNodeInfo(declaration, "not implemented yet" + AnsiColors.ANSI_RESET);
        }
        return new ArrayList<>();
    }
}
