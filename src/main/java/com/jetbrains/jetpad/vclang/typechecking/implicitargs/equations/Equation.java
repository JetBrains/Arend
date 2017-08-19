package com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations;

import com.jetbrains.jetpad.vclang.core.context.binding.inference.InferenceVariable;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.InferenceReferenceExpression;
import com.jetbrains.jetpad.vclang.term.Concrete;

public class Equation<T> implements InferenceVariableListener<T> {
  public final Expression type;
  public final Expression expr;
  public final Equations.CMP cmp;
  public final Concrete.SourceNode<T> sourceNode;

  public Equation(Expression type, Expression expr, Equations.CMP cmp, Concrete.SourceNode<T> sourceNode) {
    this.type = type;
    this.expr = expr;
    this.cmp = cmp;
    this.sourceNode = sourceNode;
  }

  @Override
  public void solved(Equations<T> equations, InferenceReferenceExpression referenceExpression) {
    if (type.isInstance(InferenceReferenceExpression.class)) {
      InferenceVariable<T> variable = type.cast(InferenceReferenceExpression.class).getVariable();
      if (variable != null) {
        variable.removeListener(this);
      }
    }
    if (expr.isInstance(InferenceReferenceExpression.class)) {
      InferenceVariable<T> variable = expr.cast(InferenceReferenceExpression.class).getVariable();
      if (variable != null) {
        variable.removeListener(this);
      }
    }
    equations.remove(this);
    equations.solve(type, expr, cmp, sourceNode);
  }
}
