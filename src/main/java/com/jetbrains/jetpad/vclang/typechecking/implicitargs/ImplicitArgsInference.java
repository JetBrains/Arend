package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.core.expr.type.ExpectedType;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;

public interface ImplicitArgsInference {
  CheckTypeVisitor.TResult infer(Concrete.AppExpression expr, ExpectedType expectedType);
  CheckTypeVisitor.TResult infer(Concrete.BinOpExpression expr, ExpectedType expectedType);
  CheckTypeVisitor.TResult inferTail(CheckTypeVisitor.TResult fun, ExpectedType expectedType, Concrete.Expression expr);
}
