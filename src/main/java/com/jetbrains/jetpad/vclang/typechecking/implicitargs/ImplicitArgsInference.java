package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.core.expr.type.ExpectedType;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;

public interface ImplicitArgsInference {
  CheckTypeVisitor.TResult infer(Abstract.AppExpression expr, ExpectedType expectedType);
  CheckTypeVisitor.TResult infer(Abstract.BinOpExpression expr, ExpectedType expectedType);
  CheckTypeVisitor.TResult inferTail(CheckTypeVisitor.TResult fun, ExpectedType expectedType, Abstract.Expression expr);
}
