package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;

public class EmptyInstancePool implements ClassViewInstancePool {
  public static EmptyInstancePool INSTANCE = new EmptyInstancePool();

  private EmptyInstancePool() {}

  @Override
  public Expression getInstance(Expression classifyingExpression, Abstract.ClassView classView) {
    return null;
  }
}
