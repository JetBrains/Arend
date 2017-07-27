package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.term.Abstract;

public class SolveEquationError extends LocalTypeCheckingError {
  public final Expression expr1;
  public final Expression expr2;

  public SolveEquationError(Expression expr1, Expression expr2, Abstract.SourceNode expression) {
    super("Cannot solve equation", expression);
    this.expr1 = expr1;
    this.expr2 = expr2;
  }
}
