package org.c2graph.util;

import javafx.util.Pair;
import org.apache.commons.collections.map.HashedMap;
import org.c2graph.model.binding.AstBinding;
import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.internal.core.dom.parser.cpp.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class PointerUtil {
    public static HashMap<Pair<IASTFunctionCallExpression, ArrayList<AstBinding>>, Boolean> amperCache = new HashMap<>();
    public static boolean checkIfArgUseAmper(IASTFunctionCallExpression expression,
                                             ArrayList<AstBinding> refBindings) {
//        Pair<IASTFunctionCallExpression, ArrayList<AstBinding>> cacheKey
//                = new Pair<>(expression, refBindings);
//        Boolean res = amperCache.get(cacheKey);
//        if (res != null) {
//            return res;
//        }
        for (IASTInitializerClause argument : expression.getArguments()) {
            if (argument instanceof IASTUnaryExpression unaryArg) {
                int operator = unaryArg.getOperator();
                if (operator != IASTUnaryExpression.op_amper) {
                    continue;
                }


                ArrayList<AstBinding> astBindings = ExprUtil.resolveVarRef(unaryArg.getOperand());
                if (refBindings.size() > astBindings.size()) {
                    continue;
                }

                int minSiz = refBindings.size() - 1;
                for (int i = astBindings.size() - 1; minSiz >= 0; i --, minSiz --) {
                    if (!astBindings.get(i).toString().equals(refBindings.get(minSiz).toString())) {
                        break;
                    }
                }
                if (minSiz == -1) {
//                    amperCache.put(cacheKey, true);
                    return true;
                }
            }
        }
//        amperCache.put(cacheKey, false);
        return false;
    }
}
