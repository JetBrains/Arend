package com.jetbrains.jetpad.vclang.typechecking.typeclass.pool;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.term.Abstract;

public interface ClassViewInstancePool {
  Expression getInstance(Abstract.DefCallExpression defCall, Expression classifyingExpression, Abstract.ClassView classView);
  Expression getInstance(Abstract.DefCallExpression defCall, int paramIndex, Expression classifyingExpression, Abstract.ClassDefinition classDefinition);
}
