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
    return new CNatOrder().isComparable(expr) || new LvlOrder().isComparable(expr) ||
            new NatOrder().isComparable(expr) ||
            new UniverseOrder().isComparable(expr) || new LevelExprOrder().isComparable(expr);
  }

  @Override
  public Boolean compare(Expression expr1, Expression expr2, CompareVisitor visitor, Equations.CMP expectedCMP) {
    Boolean cmpRes = CNatOrder.compareCNat(expr1, expr2, visitor, expectedCMP);
    if (cmpRes != null) {
      return cmpRes;
    }
    cmpRes = LvlOrder.compareLvl(expr1, expr2, visitor, expectedCMP);
    if (cmpRes != null) {
      return cmpRes;
    }
    cmpRes = NatOrder.compareNat(expr1, expr2, visitor, expectedCMP);
    if (cmpRes != null) {
      return cmpRes;
    }

    return UniverseOrder.compareUni(expr1, expr2, visitor, expectedCMP);
  }

  @Override
  public Expression max(Expression expr1, Expression expr2) {
    Expression type1 = expr1.getType().normalize(NormalizeVisitor.Mode.NF);
    Expression type2 = expr2.getType().normalize(NormalizeVisitor.Mode.NF);
    DataCallExpression dataCall1 = type1.toDataCall();
    DataCallExpression dataCall2 = type2.toDataCall();

    if (dataCall1 != null) {
      if (dataCall2 == null) {
        return null;
      }

      if (dataCall1.getDefinition() == Preprelude.CNAT && dataCall2.getDefinition() == Preprelude.CNAT) {
        return CNatOrder.maxCNat(expr1, expr2);
      }

      if (dataCall1.getDefinition() == Preprelude.LVL && dataCall2.getDefinition() == Preprelude.LVL) {
        return LvlOrder.maxLvl(expr1, expr2);
      }
    }

    UniverseExpression uni1 = expr1.toUniverse();
    UniverseExpression uni2 = expr2.toUniverse();

    if (uni1 == null || uni2 == null) {
      return null;
    }

    return UniverseOrder.maxUni(uni1, uni2);
  }
}
