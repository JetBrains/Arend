package com.jetbrains.jetpad.vclang.typechecking.exprorder;

import com.jetbrains.jetpad.vclang.term.Preprelude;
import com.jetbrains.jetpad.vclang.term.context.binding.InferenceBinding;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

public class NatOrder implements ExpressionOrder {
  public static Boolean compareNat(Expression expr1, Expression expr2, CompareVisitor visitor, Equations.CMP expectedCMP) {
    return new NatOrder().compare(expr1, expr2, visitor, expectedCMP);
  }

  @Override
  public Boolean compare(Expression expr1, Expression expr2, CompareVisitor visitor, Equations.CMP expectedCMP) {
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

    if (app1 == null && app2 == null) {
      return null;
    }

    if (app1 != null && (app1.getFunction().toConCall() == null || app1.getFunction().toConCall().getDefinition() != Preprelude.SUC ||
              app1.getArguments().size() != 1)) {
        return null;
    }

    if (app2 != null && (app2.getFunction().toConCall() == null || app2.getFunction().toConCall().getDefinition() != Preprelude.SUC ||
            app2.getArguments().size() != 1)) {
      return null;
    }

    if (app1 != null) {
      if (app2 != null) {
        return visitor.compare(app1.getArguments().get(0), app2.getArguments().get(0));
      }
      return expectedCMP == Equations.CMP.GE && visitor.compare(app1.getArguments().get(0), expr2);
    }

    return expectedCMP == Equations.CMP.LE && visitor.compare(expr1, app2.getArguments().get(0));
  }

  @Override
  public Expression max(Expression expr1, Expression expr2) {
    return ExpressionFactory.MaxNat(expr1, expr2);
  }
}
