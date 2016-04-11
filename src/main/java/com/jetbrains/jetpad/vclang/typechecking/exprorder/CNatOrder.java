package com.jetbrains.jetpad.vclang.typechecking.exprorder;

import com.jetbrains.jetpad.vclang.term.Preprelude;
import com.jetbrains.jetpad.vclang.term.context.binding.InferenceBinding;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

public class CNatOrder implements ExpressionOrder {
  public static boolean compareCNat(Expression expr1, Expression expr2, CompareVisitor visitor, Equations.CMP expectedCMP) {
    return new CNatOrder().compare(expr1, expr2, visitor, expectedCMP);
  }

  public static Expression maxCNat(Expression expr1, Expression expr2) {
    return new CNatOrder().max(expr1, expr2);
  }

  public static boolean isZero(Expression expr) {
    AppExpression app = expr.toApp();

    if (app == null || app.getFunction().toConCall() == null ||
            app.getFunction().toConCall().getDefinition() != Preprelude.FIN ||
            app.getArguments().size() != 1) {
      return false;
    }

    ConCallExpression mbZero = app.getArguments().get(0).toConCall();
    return mbZero != null && mbZero.getDefinition() == Preprelude.ZERO;
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

    if (conCall1 != null && conCall1.getDefinition() == Preprelude.INF) {
      return (conCall2 != null && conCall2.getDefinition() == Preprelude.INF) || expectedCMP == Equations.CMP.GE;
    }

    if (conCall2 != null && conCall2.getDefinition() == Preprelude.INF) {
      return expectedCMP == Equations.CMP.LE;
    }

    if (isZero(expr2)) {
      return expectedCMP == Equations.CMP.GE || (expectedCMP == Equations.CMP.EQ && isZero(expr1));
    }

    if (isZero(expr1)) {
      return expectedCMP == Equations.CMP.LE;
    }

    AppExpression app1 = expr1.toApp();
    AppExpression app2 = expr2.toApp();

    if (app1 == null || app2 == null || app1.getFunction().toConCall() == null || app2.getFunction().toConCall() == null ||
            app1.getFunction().toConCall().getDefinition() != Preprelude.FIN || app2.getFunction().toConCall().getDefinition() != Preprelude.FIN ||
            app1.getArguments().size() != 1 || app2.getArguments().size() != 1) {
      return false;
    }

    return NatOrder.compareNat(app1.getArguments().get(0), app2.getArguments().get(0), visitor, expectedCMP);
  }

  @Override
  public Expression max(Expression expr1, Expression expr2) {
    if (expr1.toApp() != null && expr1.toApp().getFunction().toConCall() != null && expr1.toApp().getFunction().toConCall().getDefinition() == Preprelude.FIN &&
            expr1.toApp().getArguments().size() == 1 &&
            expr1.toApp().getArguments().get(0).toConCall() != null && expr1.toApp().getArguments().get(0).toConCall().getDefinition() == Preprelude.ZERO) {
      return expr2;
    }
    if (expr2.toApp() != null && expr2.toApp().getFunction().toConCall() != null && expr2.toApp().getFunction().toConCall().getDefinition() == Preprelude.FIN &&
            expr2.toApp().getArguments().size() == 1 &&
            expr2.toApp().getArguments().get(0).toConCall() != null && expr2.toApp().getArguments().get(0).toConCall().getDefinition() == Preprelude.ZERO) {
      return expr1;
    }

    if (Expression.compare(expr1, expr2, Equations.CMP.GE)) {
      return expr1;
    }

    if (Expression.compare(expr1, expr2, Equations.CMP.LE)) {
      return expr2;
    }

    return ExpressionFactory.MaxCNat(expr1, expr2);
  }
}
