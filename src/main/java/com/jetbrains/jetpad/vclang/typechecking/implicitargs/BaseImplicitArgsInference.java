package com.jetbrains.jetpad.vclang.typechecking.implicitargs;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CheckTypeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.ListEquations;

public class BaseImplicitArgsInference implements ImplicitArgsInference {
  protected final CheckTypeVisitor myVisitor;

  protected BaseImplicitArgsInference(CheckTypeVisitor visitor) {
    myVisitor = visitor;
  }

  @Override
  public Equations newEquations() {
    return new ListEquations();
  }

  @Override
  public CheckTypeVisitor.Result infer(Abstract.AppExpression expr, Expression expectedType) {
    return null;
  }

  @Override
  public CheckTypeVisitor.Result infer(Abstract.BinOpExpression expr, Expression expectedType) {
    return null;
  }

  @Override
  public CheckTypeVisitor.Result inferTail(CheckTypeVisitor.OKResult fun, Expression expectedType, Abstract.Expression expr) {
    return null;
  }
}
