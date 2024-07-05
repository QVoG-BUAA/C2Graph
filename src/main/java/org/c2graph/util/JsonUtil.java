package org.c2graph.util;

import org.c2graph.model.stmt.decl.DeclSpecifier;
import org.c2graph.util.debug.AnsiColors;
import org.c2graph.util.debug.DebugUtil;
import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.c.ICASTDesignator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDesignator;
import org.eclipse.cdt.core.dom.ast.gnu.IGNUASTCompoundStatementExpression;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTDesignatedInitializer;
import org.eclipse.cdt.internal.core.dom.parser.c.CASTFieldDesignator;
import org.eclipse.cdt.internal.core.dom.parser.c.ICInternalBinding;
import org.eclipse.cdt.internal.core.dom.parser.cpp.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.ArrayList;

public class JsonUtil {
    public static JSONObject getExprJson(IASTExpression expression) {
        if (expression == null) {
            return new JSONObject();
        }
        if (expression instanceof IASTLiteralExpression) {
            return getLiteralExprJson(((IASTLiteralExpression) expression));
        }
        if (expression instanceof IASTIdExpression) {
            return getIdExprJson(((IASTIdExpression) expression));
        }
        if (expression instanceof IASTBinaryExpression) {
            return getBinaryExprJson(((IASTBinaryExpression) expression));
        }
        if (expression instanceof IASTFunctionCallExpression) {
            return getFunctionCallExprJson(((IASTFunctionCallExpression) expression));
        }
        if (expression instanceof IASTUnaryExpression) {
            return getUnaryExprJson(((IASTUnaryExpression) expression));
        }
        if (expression instanceof IASTTypeIdExpression) {
            return getTypeIdExprJson(((IASTTypeIdExpression) expression));
        }
        if (expression instanceof IASTArraySubscriptExpression) {
            return getArraySubExprJson(((IASTArraySubscriptExpression) expression));
        }
        if (expression instanceof IASTCastExpression castExpression) {
            return getCastJson(castExpression);
        }
        if (expression instanceof IASTFieldReference fieldReference) {
            return getFieldJson(fieldReference);
        }
        if (expression instanceof IGNUASTCompoundStatementExpression compoundStatementExpression) {
            return getCompoundExprJson(compoundStatementExpression);
        }
        if (expression instanceof CPPASTNewExpression newExpression) {
            return getNewJson(newExpression);
        }
        if (expression instanceof CPPASTDeleteExpression deleteExpression) {
            return getDeleteJson(deleteExpression);
        }
        if (expression instanceof IASTExpressionList exprList) {
            return getExprListJson(exprList);
        }
        if (expression instanceof IASTConditionalExpression conditionalExpression) {
            return getConditionExpr(conditionalExpression);
        }
        if (expression instanceof IASTTypeIdInitializerExpression typeIdInitializerExpression) {
            return getTypeIdInitializerJson(typeIdInitializerExpression);
        }
        if (expression instanceof IASTProblemExpression) {
            return new JSONObject();
        }
        System.out.println(AnsiColors.ANSI_RED + "build json with unknown expr: " + expression + AnsiColors.ANSI_RESET);
        return new JSONObject();
    }

    private static JSONObject getTypeIdInitializerJson(IASTTypeIdInitializerExpression typeIdInitializerExpression) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("_type", "TypeIdInitializer");

        JSONObject exprJson = getInitializerListJson((IASTInitializerList) typeIdInitializerExpression.getInitializer());
        jsonObject.put("initializer", exprJson);

        DeclSpecifier declSpecifier = new DeclSpecifier(typeIdInitializerExpression.getTypeId().getDeclSpecifier());
        IASTName typeASTName = declSpecifier.getTypeASTName();
        IBinding iBinding = typeASTName.resolveBinding();
        if (iBinding instanceof ICInternalBinding internalBinding) {
            IASTNode definition = internalBinding.getDefinition();
            IASTNode statementParent = definition.getParent();
            if (statementParent instanceof IASTDeclSpecifier declSpecifier1) {
                jsonObject.put("cast2type", new DeclSpecifier(declSpecifier1).buildJson());
            } else {
                jsonObject.put("cast2type",  getTypeJson(typeIdInitializerExpression.getExpressionType(), null));
            }
        } else {
            jsonObject.put("cast2type",  getTypeJson(typeIdInitializerExpression.getExpressionType(), null));
        }
        declSpecifier.buildJson();
        return jsonObject;
    }

    private static JSONObject getConditionExpr(IASTConditionalExpression conditionalExpression) {
        return getExprJson(conditionalExpression.getLogicalConditionExpression());
    }

    private static JSONObject getExprListJson(IASTExpressionList exprList) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("_type", "ExprList");
        JSONArray jsonArray = new JSONArray();

        int i = 0;
        for (IASTExpression expression : exprList.getExpressions()) {
            JSONObject exprJson = getExprJson(expression);
            jsonArray.put(i++, exprJson);
        }
        jsonObject.put("list", jsonArray);
        return jsonObject;
    }

    private static JSONObject getDeleteJson(CPPASTDeleteExpression deleteExpr) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("_type", "Delete");

        jsonObject.put("isVectored", deleteExpr.isVectored());
        jsonObject.put("target", getExprJson(deleteExpr.getOperand()));
        return jsonObject;
    }

    private static JSONObject getNewJson(CPPASTNewExpression newExpr) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("_type", "New");

        IASTTypeId typeId = newExpr.getTypeId();
        jsonObject.put("type", typeId.getDeclSpecifier().toString());

        IASTDeclarator abstractDeclarator = typeId.getAbstractDeclarator();
        if (abstractDeclarator instanceof CPPASTArrayDeclarator arrayDeclarator) {
            int i = 0;
            JSONArray jsonArray = new JSONArray();
            for (IASTArrayModifier arrayModifier : arrayDeclarator.getArrayModifiers()) {
                jsonArray.put(i++, JsonUtil.getExprJson(arrayModifier.getConstantExpression()));
            }
            jsonObject.put("value", jsonArray);
            jsonObject.put("isVectored", true);
        } else {
            jsonObject.put("isVectored", false);
        }
        return jsonObject;
    }

    // FIXME, it only for multi line's macro
    private static JSONObject getCompoundExprJson(IGNUASTCompoundStatementExpression compoundStatementExpression) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("_type", "MacroFunction");
        return jsonObject;
    }

    private static JSONObject getCastJson(IASTCastExpression castExpression) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("_type", "TypeCast");

        IASTTypeId typeId = castExpression.getTypeId();
        DeclSpecifier declSpecifier = new DeclSpecifier(castExpression.getTypeId().getDeclSpecifier());
        JSONObject obj = declSpecifier.buildJson();
        obj.put("pointerLevel", typeId.getAbstractDeclarator().getPointerOperators().length);

        jsonObject.put("type", obj);
        jsonObject.put("operand", JsonUtil.getExprJson(castExpression.getOperand()));
        return jsonObject;
    }

    private static JSONObject getFieldJson(IASTFieldReference fieldReference) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("_type", "FieldRef");
        jsonObject.put("field", fieldReference.getFieldName().toString());
        jsonObject.put("parent", JsonUtil.getExprJson(fieldReference.getFieldOwner()));
        return jsonObject;
    }

    private static JSONObject getArraySubExprJson(IASTArraySubscriptExpression expression) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("_type", "ArraySub");

        int count = 0;
        JSONArray subArg = new JSONArray();
        while (expression.getArrayExpression() instanceof IASTArraySubscriptExpression) {
            IASTExpression subscriptExpression = expression.getSubscriptExpression();
            subArg.put(count++, JsonUtil.getExprJson(subscriptExpression));

            expression = (IASTArraySubscriptExpression) expression.getArrayExpression();
        }
        subArg.put(count++, JsonUtil.getExprJson(expression.getSubscriptExpression()));

        JSONArray reverseSubArg = new JSONArray();
        for (int i = count - 1; i >= 0; i--) {
            reverseSubArg.put(count - 1 - i, subArg.get(i));
        }
        jsonObject.put("subs", reverseSubArg);
        jsonObject.put("arr", JsonUtil.getExprJson(expression.getArrayExpression()));
        return jsonObject;
    }

    // TODO jsonObject.put("_type", "TypeId");
    private static JSONObject getTypeIdExprJson(IASTTypeIdExpression expression) {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("_type", "UnaryExpr");
        String operator = switch (expression.getOperator()) {
            case IASTTypeIdExpression.op_sizeof -> "sizeof";
            case IASTTypeIdExpression.op_typeid -> "typeid";
            case IASTTypeIdExpression.op_alignof -> "alignof";
            case IASTTypeIdExpression.op_typeof -> "typeof";
            default -> "unknown";
        };
        jsonObject.put("operator", operator);

        JSONObject operand = new JSONObject();
        operand.put("_type", "Literal");
        operand.put("value", expression.getTypeId().getDeclSpecifier().getRawSignature());
        jsonObject.put("operand", operand);
        return jsonObject;
    }

    private static JSONObject getUnaryExprJson(IASTUnaryExpression expression) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("_type", "UnaryExpr");
        jsonObject.put("operator", ExprUtil.getUnaryOperatorById(expression.getOperator()));

        JSONObject operand = getExprJson(expression.getOperand());
        jsonObject.put("operand", operand);
        return jsonObject;
    }

    public static ArrayList<JSONObject> getArgsList(IASTFunctionCallExpression expression) {
        ArrayList<JSONObject> arrayList = new ArrayList<>();
        for (IASTInitializerClause argument : expression.getArguments()) {
            IASTExpression arg = (IASTExpression) argument;
            JSONObject jsonObject = JsonUtil.getExprJson(arg);
            arrayList.add(jsonObject);
        }
        return arrayList;
    }

    public static JSONObject getFunctionCallExprJson(IASTFunctionCallExpression expression) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("_type", "Call");

        JSONArray args = new JSONArray();
        ArrayList<JSONObject> argsList = JsonUtil.getArgsList(expression);
        for (int i = 0; i < argsList.size(); i++) {
            args.put(i, argsList.get(i));
        }
        jsonObject.put("args", args);

        JSONObject func = JsonUtil.getExprJson(expression.getFunctionNameExpression());
        jsonObject.put("func", func);
        return jsonObject;
    }

    public static JSONObject getLiteralExprJson(IASTLiteralExpression expression) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("_type", "Literal");
        if (expression.getKind() == IASTLiteralExpression.lk_string_literal) {
            String value = new String(expression.getValue());
            value = value.substring(1, value.length() - 1);
            jsonObject.put("value", value);
        } else if (expression.getKind() == IASTLiteralExpression.lk_integer_constant) {
            String value = String.valueOf(expression.getValue());
            BigInteger bigIntegerValue;

            if (value.endsWith("L") || value.endsWith("u") || value.endsWith("U")) {
                value = value.substring(0, value.length() - 1);
            }

            try {
                bigIntegerValue = new BigInteger(value);
            } catch (NumberFormatException e) {
                bigIntegerValue = null;
            }

            jsonObject.put("value", bigIntegerValue != null ? bigIntegerValue : value);

            // FIXME c/c++ unsigned long long will overflow
//            String value = String.valueOf(expression.getValue());
//            jsonObject.put("value", value);
//            if (value.endsWith("L")) {
//                value = value.substring(0, value.indexOf('L'));
//            }
//            if (value.endsWith("u")) {
//                value = value.substring(0, value.indexOf('u'));
//            }
//            if (value.endsWith("U")) {
//                value = value.substring(0, value.indexOf('U'));
//            }
//            try {
//                Long decode = Long.decode(value);
//                jsonObject.put("value", decode);
//            } catch (Exception e) {
//                jsonObject.put("value", value);
//            }
        } else if (expression.getKind() == IASTLiteralExpression.lk_char_constant) {
            jsonObject.put("value", String.valueOf(expression.getValue()));
        } else if (expression.getKind() == IASTLiteralExpression.lk_float_constant) {
            // FIXME, the same with int
            jsonObject.put("value", String.valueOf(expression.getValue()));
        } else if (expression.getKind() == IASTLiteralExpression.lk_true) {
            jsonObject.put("value", "true");
        } else if (expression.getKind() == IASTLiteralExpression.lk_false) {
            jsonObject.put("value", "false");
        } else if (expression.getKind() == IASTLiteralExpression.lk_nullptr) {
            jsonObject.put("value", "nullptr");
        } else {
            throw new RuntimeException("literal type not implemented yet!");
        }
        return jsonObject;
    }

    public static JSONObject getIdExprJson(IASTIdExpression expression) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("_type", "Name");

        IASTName varName = expression.getName();
        int roleForName = expression.getRoleForName(varName);
        String role = switch (roleForName) {
            case IASTNameOwner.r_declaration -> "DECLARATION";
            case IASTNameOwner.r_reference -> "REFERENCE";
            case IASTNameOwner.r_definition -> "DEFINITION";
            default -> "UNCLEAR";
        };
        jsonObject.put("id", varName.toString());
        jsonObject.put("role", role);
        jsonObject.put("type", getTypeJson(expression.getExpressionType(), varName));
        return jsonObject;
    }

    public static JSONObject getTypeJson(IType type, IASTName name) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("_type", "Type");
        if (type instanceof IBasicType) {
            jsonObject.put("type", "basic");
            jsonObject.put("name", type.toString());
        } else if (type instanceof ICompositeType compositeType) {
            String typeName = "unimplemented composite type";
            if (compositeType.getKey() == ICompositeType.k_struct) {
                typeName = "struct";
            } else if (compositeType.getKey() == ICompositeType.k_union) {
                typeName = "union";
            }
            jsonObject.put("type", typeName);
            jsonObject.put("name", type.toString());
        } else if (type instanceof IArrayType arrayType) {
            jsonObject.put("type", "array");
            IValue size = arrayType.getSize();
            if (size != null) {
                jsonObject.put("size", size.numberValue());
            }
            jsonObject.put("name", type.toString());
        } else if (type instanceof IPointerType pointerType) {
            jsonObject.put("type", "pointer");
            jsonObject.put("isConst", pointerType.isConst());
            jsonObject.put("isVolatile", pointerType.isVolatile());
            jsonObject.put("isRestrict", pointerType.isRestrict());
            jsonObject.put("name", type.toString());
        } else if (type instanceof IFunctionType functionType) {
            jsonObject.put("type", "function");
            jsonObject.put("isTakeVarArgs", functionType.takesVarArgs());
            JSONArray argsTypeArray = new JSONArray();
            IType[] parameterTypes = functionType.getParameterTypes();
            for (int i = 0; i < parameterTypes.length; i++) {
                IType parameterType = parameterTypes[i];
                argsTypeArray.put(i, getTypeJson(parameterType, null));
            }
            jsonObject.put("argTypes", argsTypeArray);
            jsonObject.put("returnType", getTypeJson(functionType.getReturnType(), null));
            jsonObject.put("name", name.resolveBinding().toString());
        } else if (type instanceof ITypedef typedef) {
            jsonObject.put("name", typedef.getName());
            jsonObject.put("type", getTypeJson(typedef.getType(), name));
        } else if (type instanceof IProblemType problemType) {
            return new JSONObject();
        }
        return jsonObject;
    }

    public static JSONObject getIdExprJson(String name) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("_type", "Name");
        jsonObject.put("id", name);
        return jsonObject;
    }

    public static JSONObject getBinaryExprJson(IASTBinaryExpression expression) {
        if (expression.getOperator() == IASTBinaryExpression.op_assign) {
            return getBinaryAssignExprJson(expression);
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("_type", "BinaryExpr");

        String basicType = expression.getExpressionType().toString();
        jsonObject.put("basicType", basicType);
        jsonObject.put("operator", ExprUtil.getBinaryOperandById(expression.getOperator()));

        JSONObject leftOperand = getExprJson(expression.getOperand1());
        JSONObject rightOperand = getExprJson(expression.getOperand2());

        jsonObject.put("leftOp", leftOperand);
        jsonObject.put("rightOp", rightOperand);

        return jsonObject;
    }

    public static JSONObject getBinaryAssignExprJson(IASTBinaryExpression expression) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("_type", "Assign");

        JSONObject leftOperand = getExprJson(expression.getOperand1());
        JSONObject rightOperand = getExprJson(expression.getOperand2());

        jsonObject.put("target", leftOperand);
        jsonObject.put("value", rightOperand);
        return jsonObject;
    }

    // it seems CPP only
    public static JSONObject getDesignatedInitializerJson(CPPASTDesignatedInitializer initializer) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("value", getExprJson(((IASTExpression) initializer.getOperand())));
        for (ICPPASTDesignator designator : initializer.getDesignators()) {
            if (designator instanceof CPPASTFieldDesignator fieldDesignator) {
                jsonObject.put("field", fieldDesignator.getName().toString());
            }
        }
        return jsonObject;
    }

    public static JSONObject getDesignatedInitializerJson(CASTDesignatedInitializer initializer) {
        JSONObject jsonObject = new JSONObject();
        IASTInitializerClause operand = initializer.getOperand();
        if (operand instanceof IASTExpression expression) {
            jsonObject.put("value", getExprJson(expression));
        } else if (operand instanceof IASTInitializerList list) {
            jsonObject.put("value", getInitializerListJson(list));
        } else {
            System.out.println(AnsiColors.ANSI_RED + "unknown type in getDesignatedInitializerJson: " + operand);
            DebugUtil.printNodeInfo(operand, "not implemented yet" + AnsiColors.ANSI_RESET);
        }
        for (ICASTDesignator designator : initializer.getDesignators()) {
            if (designator instanceof CASTFieldDesignator fieldDesignator) {
                jsonObject.put("field", fieldDesignator.getName().toString());
            }
        }
        return jsonObject;
    }

    public static JSONObject getInitializerListJson(IASTInitializerList initializerList) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("_type", "InitializerList");

        JSONArray jsonArray = new JSONArray();
        int i = 0;
        for (IASTInitializerClause clause : initializerList.getClauses()) {
            JSONObject exprJson;
            if (clause instanceof IASTInitializerList list) {
                exprJson = getInitializerListJson(list);
            } else {
                if (clause instanceof CPPASTDesignatedInitializer initializer) {
                    exprJson = getDesignatedInitializerJson(initializer);
                } else if (clause instanceof CASTDesignatedInitializer initializer) {
                    exprJson = getDesignatedInitializerJson(initializer);
                } else {
                    exprJson = getExprJson(((IASTExpression) clause));
                }
            }
            jsonArray.put(i++, exprJson);
        }
        jsonObject.put("initializerList", jsonArray);

        return jsonObject;
    }
}
