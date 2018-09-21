package org.arend.typechecking.implicitargs.equations;

import org.arend.core.context.binding.inference.InferenceVariable;
import org.arend.core.expr.Expression;
import org.arend.core.expr.InferenceReferenceExpression;
import org.arend.term.concrete.Concrete;

public class Equation implements InferenceVariableListener {
  public final Expression type;
  public final Expression expr;
  public final Equations.CMP cmp;
  public final Concrete.SourceNode sourceNode;

  public Equation(Expression type, Expression expr, Equations.CMP cmp, Concrete.SourceNode sourceNode) {
    this.type = type;
    this.expr = expr;
    this.cmp = cmp;
    this.sourceNode = sourceNode;
  }

  @Override
  public void solved(Equations equations, InferenceReferenceExpression referenceExpression) {
    if (type.isInstance(InferenceReferenceExpression.class)) {
      InferenceVariable variable = type.cast(InferenceReferenceExpression.class).getVariable();
      if (variable != null) {
        variable.removeListener(this);
      }
    }
    if (expr.isInstance(InferenceReferenceExpression.class)) {
      InferenceVariable variable = expr.cast(InferenceReferenceExpression.class).getVariable();
      if (variable != null) {
        variable.removeListener(this);
      }
    }
    equations.remove(this);
    equations.solve(type, expr, cmp, sourceNode);
  }
}
