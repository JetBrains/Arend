package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.core.expr.Expression;

public interface ClassViewInstancePool {
  Expression getInstance(Expression classifyingExpression, Abstract.ClassView classView);
}
