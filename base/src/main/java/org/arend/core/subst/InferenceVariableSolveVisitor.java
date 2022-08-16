package org.arend.core.subst;

import org.arend.core.expr.Expression;
import org.arend.core.expr.InferenceReferenceExpression;
import org.arend.core.expr.visitor.VoidExpressionVisitor;
import org.arend.ext.core.ops.NormalizationMode;
import org.arend.typechecking.visitor.CheckTypeVisitor;

public class InferenceVariableSolveVisitor extends VoidExpressionVisitor<Void> {
  private final CheckTypeVisitor myTypechecker;

  public InferenceVariableSolveVisitor(CheckTypeVisitor typechecker) {
    myTypechecker = typechecker;
  }

  @Override
  public Void visitInferenceReference(InferenceReferenceExpression expr, Void params) {
    if (expr.getVariable() != null) {
      Expression solution = InferenceReferenceExpression.makeUnique(expr.getVariable().getType().normalize(NormalizationMode.WHNF));
      if (solution != null) {
        expr.getVariable().solve(myTypechecker, solution);
      }
    }
    return super.visitInferenceReference(expr, params);
  }
}
