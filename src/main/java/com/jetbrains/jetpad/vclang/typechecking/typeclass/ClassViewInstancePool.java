package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.term.expr.Expression;

public interface ClassViewInstancePool {
  Expression getInstance(Expression classifyingExpression);
}
