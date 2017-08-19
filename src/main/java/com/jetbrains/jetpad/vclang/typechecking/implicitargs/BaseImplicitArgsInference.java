package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.core.expr.type.ExpectedType;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;

public class BaseImplicitArgsInference<T> implements ImplicitArgsInference<T> {
  protected final CheckTypeVisitor<T> myVisitor;

  protected BaseImplicitArgsInference(CheckTypeVisitor<T> visitor) {
    myVisitor = visitor;
  }

  @Override
  public CheckTypeVisitor.TResult<T> infer(Concrete.AppExpression<T> expr, ExpectedType expectedType) {
    return null;
  }

  @Override
  public CheckTypeVisitor.TResult<T> infer(Concrete.BinOpExpression<T> expr, ExpectedType expectedType) {
    return null;
  }

  @Override
  public CheckTypeVisitor.TResult<T> inferTail(CheckTypeVisitor.TResult<T> fun, ExpectedType expectedType, Concrete.Expression<T> expr) {
    return null;
  }
}
