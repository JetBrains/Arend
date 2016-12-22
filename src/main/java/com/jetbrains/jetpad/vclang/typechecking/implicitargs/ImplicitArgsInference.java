package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;

public interface ImplicitArgsInference {
  CheckTypeVisitor.PreResult infer(Abstract.AppExpression expr, Type expectedType);
  CheckTypeVisitor.PreResult infer(Abstract.BinOpExpression expr, Type expectedType);
  CheckTypeVisitor.Result inferTail(CheckTypeVisitor.Result fun, Type expectedType, Abstract.Expression expr);
}
