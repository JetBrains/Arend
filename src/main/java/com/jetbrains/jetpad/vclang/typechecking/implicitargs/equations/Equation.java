package com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations;

import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.InferenceReferenceExpression;
import com.jetbrains.jetpad.vclang.term.Abstract;

public class Equation implements InferenceVariableListener {
  public final Expression type;
  public final Expression expr;
  public final Equations.CMP cmp;
  public final Abstract.SourceNode sourceNode;

  public Equation(Expression type, Expression expr, Equations.CMP cmp, Abstract.SourceNode sourceNode) {
    this.type = type;
    this.expr = expr;
    this.cmp = cmp;
    this.sourceNode = sourceNode;
  }

  @Override
  public void solved(Equations equations, InferenceReferenceExpression referenceExpression) {
    equations.remove(this);
    equations.solve(type, expr, cmp, sourceNode);
  }
}
