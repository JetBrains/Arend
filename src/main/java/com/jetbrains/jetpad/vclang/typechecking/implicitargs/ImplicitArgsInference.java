package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.core.expr.type.ExpectedType;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;

public interface ImplicitArgsInference<T> {
  CheckTypeVisitor.TResult<T> infer(Concrete.AppExpression<T> expr, ExpectedType expectedType);
  CheckTypeVisitor.TResult<T> infer(Concrete.BinOpExpression<T> expr, ExpectedType expectedType);
  CheckTypeVisitor.TResult<T> inferTail(CheckTypeVisitor.TResult<T> fun, ExpectedType expectedType, Concrete.Expression<T> expr);
}
