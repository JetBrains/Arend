package com.jetbrains.jetpad.vclang.typechecking.exprorder;

import com.jetbrains.jetpad.vclang.term.Preprelude;
import com.jetbrains.jetpad.vclang.term.context.binding.InferenceBinding;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

public class CNatOrder implements ExpressionOrder {
  public static Boolean compareCNat(Expression expr1, Expression expr2, CompareVisitor visitor, Equations.CMP expectedCMP) {
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
  public boolean isComparable(Expression expr) {
    Expression type = expr.getType().normalize(NormalizeVisitor.Mode.NF);
    DataCallExpression dataCall = type.toDataCall();

    return dataCall != null && dataCall.getDefinition() == Preprelude.CNAT;
  }

  @Override
  public Boolean compare(Expression expr1, Expression expr2, CompareVisitor visitor, Equations.CMP expectedCMP) {
    ConCallExpression conCall1 = expr1.toConCall();
    ConCallExpression conCall2 = expr2.toConCall();

    if (conCall1 != null && conCall1.getDefinition() == Preprelude.INF) {
      if ((conCall2 != null && conCall2.getDefinition() == Preprelude.INF) || expectedCMP == Equations.CMP.GE) {
        return true;
      }
      return null;
    }

    if (conCall2 != null && conCall2.getDefinition() == Preprelude.INF) {
      if (expectedCMP == Equations.CMP.LE) {
        return true;
      }
      return null;
    }

    if (isZero(expr2)) {
      if(expectedCMP == Equations.CMP.GE || (expectedCMP == Equations.CMP.EQ && isZero(expr1))) {
        return true;
      }
      return null;
    }

    if (isZero(expr1)) {
      if (expectedCMP == Equations.CMP.LE) {
        return true;
      }
      return null;
    }

    Expression fun1 = expr1.getFunction();
    Expression fun2 = expr2.getFunction();
    boolean isSuc1 = fun1.toFunCall() != null && fun1.toFunCall().getDefinition() == Preprelude.SUC_CNAT &&
            expr1.getArguments().size() == 1;
    boolean isSuc2 = fun2.toFunCall() != null && fun2.toFunCall().getDefinition() == Preprelude.SUC_CNAT &&
            expr2.getArguments().size() == 1;
    boolean isFin1 = fun1.toConCall() != null && fun1.toConCall().getDefinition() == Preprelude.FIN &&
            expr1.getArguments().size() == 1;
    boolean isFin2 = fun2.toConCall() != null && fun2.toConCall().getDefinition() == Preprelude.FIN &&
            expr2.getArguments().size() == 1;
    boolean isMax1 = fun1.toFunCall() != null && fun1.toFunCall().getDefinition() == Preprelude.MAX_CNAT &&
            expr1.getArguments().size() == 2;
    boolean isMax2 = fun2.toFunCall() != null && fun2.toFunCall().getDefinition() == Preprelude.MAX_CNAT &&
            expr2.getArguments().size() == 2;

    if (isMax1) {
      if (expectedCMP == Equations.CMP.LE || expectedCMP == Equations.CMP.EQ) {
        return visitor.compare(expr1.getArguments().get(0), expr2) && visitor.compare(expr1.getArguments().get(1), expr2);
      }
      if (expectedCMP == Equations.CMP.GE) {
        return visitor.compare(expr1.getArguments().get(0), expr2) || visitor.compare(expr1.getArguments().get(1), expr2);
      }
    }

    if (isMax2) {
      if (expectedCMP == Equations.CMP.GE || expectedCMP == Equations.CMP.EQ) {
        return visitor.compare(expr1, expr2.getArguments().get(0)) && visitor.compare(expr1, expr2.getArguments().get(1));
      }
      if (expectedCMP == Equations.CMP.LE) {
        return visitor.compare(expr1, expr2.getArguments().get(0)) || visitor.compare(expr1, expr2.getArguments().get(1));
      }
    }

    if (isSuc1) {
      if (isSuc2) {
        return visitor.compare(expr1.getArguments().get(0), expr2.getArguments().get(0));
      }
      if (expectedCMP == Equations.CMP.GE) {
        return visitor.compare(expr1.getArguments().get(0), expr2);
      }
      return null;
    }

    if (isSuc2) {
      if (expectedCMP == Equations.CMP.LE) {
        return visitor.compare(expr1, expr2.getArguments().get(0));
      }
    }

    if (isFin1) {
      if (!isFin2) {
        return null;
      }
      return visitor.compare(expr1.getArguments().get(0), expr2.getArguments().get(0));
    }

    return null;
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
    }/**/

    return ExpressionFactory.MaxCNat(expr1, expr2);
  }
}
