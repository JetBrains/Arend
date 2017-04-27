package com.jetbrains.jetpad.vclang.typechecking.typeclass.pool;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.term.Abstract;

public interface ClassViewInstancePool {
  Expression getInstance(Abstract.ReferenceExpression defCall, Expression classifyingExpression, Abstract.ClassView classView);
  Expression getInstance(Abstract.ReferenceExpression defCall, int paramIndex, Expression classifyingExpression, Abstract.ClassDefinition classDefinition);
}
