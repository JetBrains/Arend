package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.visitor.CheckTypeVisitor;

public class BaseImplicitArgsInference implements ImplicitArgsInference {
  protected final CheckTypeVisitor myVisitor;

  protected BaseImplicitArgsInference(CheckTypeVisitor visitor) {
    myVisitor = visitor;
  }

  @Override
  public CheckTypeVisitor.DefCallResult infer(Abstract.AppExpression expr, Type expectedType) {
    return null;
  }

  @Override
  public CheckTypeVisitor.DefCallResult infer(Abstract.BinOpExpression expr, Type expectedType) {
    return null;
  }

  @Override
  public CheckTypeVisitor.DefCallResult inferTail(CheckTypeVisitor.DefCallResult fun, Type expectedType, Abstract.Expression expr) {
    return null;
  }
}
