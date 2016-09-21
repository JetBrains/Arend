package com.jetbrains.jetpad.vclang.typechecking.typeclass;

import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.typeclass.ClassView;

public interface ClassViewInstancePool {
  Expression getInstance(Expression classifyingExpression, ClassView classView);
}
