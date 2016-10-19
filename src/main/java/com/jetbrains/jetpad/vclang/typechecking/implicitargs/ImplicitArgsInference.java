package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;

public interface ImplicitArgsInference {
  CheckTypeVisitor.PreResult infer(Abstract.AppExpression expr, Type expectedType);
  CheckTypeVisitor.PreResult infer(Abstract.BinOpExpression expr, Type expectedType);
  CheckTypeVisitor.Result inferTail(CheckTypeVisitor.Result fun, Type expectedType, Abstract.Expression expr);
}
