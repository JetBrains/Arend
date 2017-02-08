package com.jetbrains.jetpad.vclang.typechecking.typeclass.pool;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.term.Abstract;

public class EmptyInstancePool implements ClassViewInstancePool {
  public static EmptyInstancePool INSTANCE = new EmptyInstancePool();

  private EmptyInstancePool() {}

  @Override
  public Expression getInstance(Abstract.DefCallExpression defCall, Expression classifyingExpression, Abstract.ClassView classView) {
    return null;
  }

  @Override
  public Expression getInstance(Abstract.DefCallExpression defCall, int paramIndex, Expression classifyingExpression, Abstract.ClassDefinition classDefinition) {
    return null;
  }
}
