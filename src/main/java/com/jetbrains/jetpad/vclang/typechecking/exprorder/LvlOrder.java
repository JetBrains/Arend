package com.jetbrains.jetpad.vclang.typechecking.exprorder;

import com.jetbrains.jetpad.vclang.term.Preprelude;
import com.jetbrains.jetpad.vclang.term.context.binding.InferenceBinding;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

public class LvlOrder implements ExpressionOrder {
  public static Boolean compareLvl(Expression expr1, Expression expr2, CompareVisitor visitor, Equations.CMP expectedCMP) {
    return new LvlOrder().compare(expr1, expr2, visitor, expectedCMP);
  }

  public static Expression maxLvl(Expression expr1, Expression expr2) {
    return new LvlOrder().max(expr1, expr2);
  }

  @Override
  public boolean isComparable(Expression expr) {
    Expression type = expr.getType().normalize(NormalizeVisitor.Mode.NF);
    DataCallExpression dataCall = type.toDataCall();

    return dataCall != null && dataCall.getDefinition() == Preprelude.LVL;
  }

  @Override
  public Boolean compare(Expression expr1, Expression expr2, CompareVisitor visitor, Equations.CMP expectedCMP) {
    ConCallExpression conCall1 = expr1.toConCall();
    ConCallExpression conCall2 = expr2.toConCall();

    if (conCall1 != null && conCall1.getDefinition() == Preprelude.ZERO_LVL) {
      return (conCall2 != null && conCall2.getDefinition() == Preprelude.ZERO_LVL) || expectedCMP == Equations.CMP.LE;
    }

    if (conCall2 != null && conCall2.getDefinition() == Preprelude.ZERO_LVL) {
      return expectedCMP == Equations.CMP.GE;
    }

    Expression fun1 = expr1.getFunction();
    Expression fun2 = expr2.getFunction();
    boolean isSuc1 = fun1.toConCall() != null && fun1.toConCall().getDefinition() == Preprelude.SUC_LVL &&
            expr1.getArguments().size() == 1;
    boolean isSuc2 = fun2.toConCall() != null && fun2.toConCall().getDefinition() == Preprelude.SUC_LVL &&
            expr2.getArguments().size() == 1;

    if (isSuc1) {
      if (isSuc2) {
        return visitor.compare(expr1.getArguments().get(0), expr2.getArguments().get(0));
      }
      return expectedCMP == Equations.CMP.GE && visitor.compare(expr1.getArguments().get(0), expr2);
    }

    if (isSuc2) {
      return expectedCMP == Equations.CMP.LE && visitor.compare(expr1, expr2.getArguments().get(0));
    }

    return null;
  }

  @Override
  public Expression max(Expression expr1, Expression expr2) {
    if (Expression.compare(expr1, expr2, Equations.CMP.GE)) {
      return expr1;
    }
    if (Expression.compare(expr1, expr2, Equations.CMP.LE)) {
      return expr2;
    }
    return ExpressionFactory.MaxLvl(expr1, expr2);
  }
}
