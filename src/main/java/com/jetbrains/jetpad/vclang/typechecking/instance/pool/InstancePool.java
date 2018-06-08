package com.jetbrains.jetpad.vclang.typechecking.instance.pool;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;

public interface InstancePool {
  Expression getInstance(Expression classifyingExpression, Concrete.ClassSynonym classSyn, boolean isView);
}
