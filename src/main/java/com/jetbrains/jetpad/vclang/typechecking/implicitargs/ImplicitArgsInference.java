package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.core.expr.type.ExpectedType;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;

public interface ImplicitArgsInference<T> {
  CheckTypeVisitor.TResult infer(Concrete.AppExpression<T> expr, ExpectedType expectedType);
  CheckTypeVisitor.TResult infer(Concrete.BinOpExpression<T> expr, ExpectedType expectedType);
  CheckTypeVisitor.TResult inferTail(CheckTypeVisitor.TResult fun, ExpectedType expectedType, Concrete.Expression<T> expr);
}
