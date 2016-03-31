package com.jetbrains.jetpad.vclang.typechecking.exprorder;

import com.jetbrains.jetpad.vclang.term.Preprelude;
import com.jetbrains.jetpad.vclang.term.context.binding.InferenceBinding;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

public class NatOrder implements ExpressionOrder {
  public static boolean compareNat(Expression expr1, Expression expr2, CompareVisitor visitor, Equations.CMP expectedCMP) {
    return new NatOrder().compare(expr1, expr2, visitor, expectedCMP);
  }

  @Override
  public boolean compare(Expression expr1, Expression expr2, CompareVisitor visitor, Equations.CMP expectedCMP) {
    ReferenceExpression ref1 = expr1.toReference();
    ReferenceExpression ref2 = expr2.toReference();

    if ((ref1 != null && ref1.getBinding() instanceof InferenceBinding) || (ref2 != null && ref2.getBinding() instanceof InferenceBinding)) {
      return visitor.compare(expr1, expr2);
    }

    ConCallExpression conCall1 = expr1.toConCall();
    ConCallExpression conCall2 = expr2.toConCall();

    if (conCall1 != null && conCall1.getDefinition() == Preprelude.ZERO) {
      return (conCall2 != null && conCall2.getDefinition() == Preprelude.ZERO) || expectedCMP == Equations.CMP.LE;
    }

    if (conCall2 != null && conCall2.getDefinition() == Preprelude.ZERO) {
      return expectedCMP == Equations.CMP.GE;
    }

    AppExpression app1 = expr1.toApp();
    AppExpression app2 = expr2.toApp();

    if (app1 == null || app2 == null || app1.getFunction().toConCall() == null || app2.getFunction().toConCall() == null ||
            app1.getFunction().toConCall().getDefinition() != Preprelude.FIN || app2.getFunction().toConCall().getDefinition() != Preprelude.FIN ||
            app1.getArguments().size() != 1 || app2.getArguments().size() != 1) {
      return false;
    }

    return compare(app1.getArguments().get(0), app2.getArguments().get(0), visitor, expectedCMP);
  }

  @Override
  public Expression max(Expression expr1, Expression expr2) {
    return ExpressionFactory.MaxNat(expr1, expr2);
  }
}
