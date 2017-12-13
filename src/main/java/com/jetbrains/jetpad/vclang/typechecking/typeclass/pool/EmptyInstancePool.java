package com.jetbrains.jetpad.vclang.typechecking.typeclass.pool;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;

public class EmptyInstancePool implements InstancePool {
  public static EmptyInstancePool INSTANCE = new EmptyInstancePool();

  private EmptyInstancePool() {}

  @Override
  public Expression getInstance(Expression classifyingExpression, Concrete.ClassSynonym classSyn, boolean isView) {
    return null;
  }
}
