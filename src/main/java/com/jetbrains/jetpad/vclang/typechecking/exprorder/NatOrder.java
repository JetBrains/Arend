package com.jetbrains.jetpad.vclang.typechecking.exprorder;

import com.jetbrains.jetpad.vclang.term.Preprelude;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

public class NatOrder implements ExpressionOrder {
  public static Boolean compareNat(Expression expr1, Expression expr2, CompareVisitor visitor, Equations.CMP expectedCMP) {
    return new NatOrder().compare(expr1, expr2, visitor, expectedCMP);
  }

  @Override
  public boolean isComparable(Expression expr) {
    Expression type = expr.getType().normalize(NormalizeVisitor.Mode.NF);
    DataCallExpression dataCall = type.toDataCall();

    return dataCall != null && dataCall.getDefinition() == Preprelude.NAT;
  }

  @Override
  public Boolean compare(Expression expr1, Expression expr2, CompareVisitor visitor, Equations.CMP expectedCMP) {
    ConCallExpression conCall1 = expr1.toConCall();
    ConCallExpression conCall2 = expr2.toConCall();

    if (conCall1 != null && conCall1.getDefinition() == Preprelude.ZERO) {
      if ((conCall2 != null && conCall2.getDefinition() == Preprelude.ZERO) || expectedCMP == Equations.CMP.LE) {
        return true;
      }
      return null;
    }

    if (conCall2 != null && conCall2.getDefinition() == Preprelude.ZERO) {
      if (expectedCMP == Equations.CMP.GE) {
        return true;
      }
      return null;
    }

    Expression fun1 = expr1.getFunction();
    Expression fun2 = expr2.getFunction();
    boolean isSuc1 = fun1.toConCall() != null && fun1.toConCall().getDefinition() == Preprelude.SUC &&
            expr1.getArguments().size() == 1;
    boolean isSuc2 = fun2.toConCall() != null && fun2.toConCall().getDefinition() == Preprelude.SUC &&
            expr2.getArguments().size() == 1;

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

    return null;
  }

  @Override
  public Expression max(Expression expr1, Expression expr2) {
    return ExpressionFactory.MaxNat(expr1, expr2);
  }
}
