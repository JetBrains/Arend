package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;

public interface ImplicitArgsInference {
  CheckTypeVisitor.TResult infer(Abstract.AppExpression expr, Type expectedType);
  CheckTypeVisitor.TResult infer(Abstract.BinOpExpression expr, Type expectedType);
  boolean inferTail(CheckTypeVisitor.TResult fun, Type expectedType, Abstract.Expression expr);
}
