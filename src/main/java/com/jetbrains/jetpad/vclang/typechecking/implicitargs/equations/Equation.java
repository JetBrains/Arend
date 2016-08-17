package com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.InferenceReferenceExpression;
import com.jetbrains.jetpad.vclang.term.expr.type.Type;

public class Equation implements InferenceVariableListener {
  public Type type;
  public Expression expr;
  public Equations.CMP cmp;
  public Abstract.SourceNode sourceNode;

  public Equation(Type type, Expression expr, Equations.CMP cmp, Abstract.SourceNode sourceNode) {
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
