package com.jetbrains.jetpad.vclang.typechecking.exprorder;

import com.jetbrains.jetpad.vclang.term.Preprelude;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

public class StandardOrder implements ExpressionOrder {
  private static StandardOrder INSTANCE = new StandardOrder();

  public static StandardOrder getInstance() {
    return INSTANCE;
  }

  @Override
  public boolean isComparable(Expression expr) {
    return expr.toUniverse() != null;
  }

  @Override
  public Boolean compare(Expression expr1, Expression expr2, CompareVisitor visitor, Equations.CMP expectedCMP) {
    return NatOrder.compareNat(expr1, expr2, visitor, expectedCMP);
  }

  @Override
  public Expression max(Expression expr1, Expression expr2) {
    /*
    Expression type1 = expr1.getType().normalize(NormalizeVisitor.Mode.NF);
    Expression type2 = expr2.getType().normalize(NormalizeVisitor.Mode.NF);
    DataCallExpression dataCall1 = type1.toDataCall();
    DataCallExpression dataCall2 = type2.toDataCall();

    if (dataCall1 != null) {
      if (dataCall2 == null) {
        return null;
      }

      if (dataCall1.getDefinition() == Preprelude.NAT && dataCall2.getDefinition() == Preprelude.NAT) {
        return NatOrder.max(expr1, expr2);
      }

      if (dataCall1.getDefinition() == Preprelude.LVL && dataCall2.getDefinition() == Preprelude.LVL) {
        return LvlOrder.maxLvl(expr1, expr2);
      }
    } /**/

    return null;
  }
}
