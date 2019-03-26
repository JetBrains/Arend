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

  public Expression getLowerBound() {
    return cmp != Equations.CMP.GE ? type : expr;
  }

  public Expression getUpperBound() {
    return cmp != Equations.CMP.GE ? expr : type;
  }

  @Override
  public void solved(Equations equations, InferenceReferenceExpression referenceExpression) {
    InferenceVariable var1 = type.getInferenceVariable();
    InferenceVariable var2 = expr.getInferenceVariable();
    if (var1 != null) {
      var1.removeListener(this);
    }
    if (var2 != null) {
      var2.removeListener(this);
    }
    equations.remove(this);
    equations.solve(type, expr, cmp, sourceNode);
  }
}
