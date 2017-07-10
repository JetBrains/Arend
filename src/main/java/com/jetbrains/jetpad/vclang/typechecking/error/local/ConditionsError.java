package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.term.Abstract;

public class ConditionsError extends LocalTypeCheckingError {
  public final Abstract.Definition definition;
  public final Expression expr1;
  public final Expression expr2;
  public final Expression evaluatedExpr1;
  public final Expression evaluatedExpr2;

  public ConditionsError(Abstract.Definition definition, Expression expr1, Expression expr2, Expression evaluatedExpr1, Expression evaluatedExpr2) {
    super("Conditions check failed:", definition);
    this.definition = definition;
    this.expr1 = expr1;
    this.expr2 = expr2;
    this.evaluatedExpr1 = evaluatedExpr1;
    this.evaluatedExpr2 = evaluatedExpr2;
  }
}
