package com.jetbrains.jetpad.vclang.typechecking.error.local;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.Abstract;

public class ConditionsError extends LocalTypeCheckingError {
  public final Expression expr1;
  public final Expression expr2;
  public final ExprSubstitution substitution1;
  public final ExprSubstitution substitution2;
  public final Expression evaluatedExpr1;
  public final Expression evaluatedExpr2;

  public ConditionsError(Expression expr1, Expression expr2, ExprSubstitution substitution1, ExprSubstitution substitution2, Expression evaluatedExpr1, Expression evaluatedExpr2, Abstract.SourceNode sourceNode) {
    super("Conditions check failed:", sourceNode);
    this.expr1 = expr1;
    this.expr2 = expr2;
    this.substitution1 = substitution1;
    this.substitution2 = substitution2;
    this.evaluatedExpr1 = evaluatedExpr1;
    this.evaluatedExpr2 = evaluatedExpr2;
  }
}
