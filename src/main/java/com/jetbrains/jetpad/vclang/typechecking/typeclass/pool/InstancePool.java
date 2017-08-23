package com.jetbrains.jetpad.vclang.typechecking.typeclass.pool;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.term.Concrete;

public interface InstancePool {
  Expression getInstance(Expression classifyingExpression, Concrete.ClassView classView, boolean isView);
}
