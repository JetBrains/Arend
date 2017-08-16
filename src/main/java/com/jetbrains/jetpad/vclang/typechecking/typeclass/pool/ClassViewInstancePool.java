package com.jetbrains.jetpad.vclang.typechecking.typeclass.pool;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.term.Abstract;

public interface ClassViewInstancePool {
  Expression getInstance(Expression classifyingExpression, Abstract.ClassView classView, boolean isView);
}
