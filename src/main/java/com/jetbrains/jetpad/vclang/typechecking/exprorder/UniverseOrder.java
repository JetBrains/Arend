package com.jetbrains.jetpad.vclang.typechecking.exprorder;

import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

public class UniverseOrder implements ExpressionOrder {
  public static Boolean compareUni(Expression expr1, Expression expr2, CompareVisitor visitor, Equations.CMP expectedCMP) {
    return new UniverseOrder().compare(expr1, expr2, visitor, expectedCMP);
  }

  public static Expression maxUni(Expression expr1, Expression expr2) {
    return new UniverseOrder().max(expr1, expr2);
  }

  @Override
  public boolean isComparable(Expression expr) {
    return expr.toUniverse() != null;
  }

  @Override
  public Boolean compare(Expression expr1, Expression expr2, CompareVisitor visitor, Equations.CMP expectedCMP) {
    UniverseExpression uni1 = expr1.toUniverse();
    UniverseExpression uni2 = expr2.toUniverse();

    if (uni1 == null || uni2 == null) {
      return null;
    }

    Expression hlevel1 = uni1.getUniverse().getHLevel();
    Expression hlevel2 = uni2.getUniverse().getHLevel();
    Expression plevel1 = uni1.getUniverse().getPLevel();
    Expression plevel2 = uni2.getUniverse().getPLevel();

    Boolean cmp1 = visitor.compare(hlevel1, hlevel2);//LevelExprOrder.compareLevel(hlevel1, hlevel2, visitor, expectedCMP); // CNatOrder.compareCNat(hlevel1, hlevel2, visitor, expectedCMP);
    Boolean cmp2 = visitor.compare(plevel1, plevel2);//LevelExprOrder.compareLevel(plevel1, plevel2, visitor, expectedCMP); // CNatOrder.compareCNat(hlevel1, hlevel2, visitor, expectedCMP);

    if (LevelExprOrder.isZero(hlevel1) || LevelExprOrder.isZero(hlevel2)) {
      return cmp1;
    }

    if (cmp1 == null || cmp2 == null) return null;

    return cmp1 && cmp2;
  }

  @Override
  public Expression max(Expression expr1, Expression expr2) {
    if (Expression.compare(expr1, expr2, Equations.CMP.GE)) {
      return expr1;
    }

    if (Expression.compare(expr1, expr2, Equations.CMP.LE)) {
      return expr2;
    }/**/

    UniverseExpression uni1 = expr1.toUniverse();
    UniverseExpression uni2 = expr2.toUniverse();

    return ExpressionFactory.Universe(LvlOrder.maxLvl(uni1.getUniverse().getPLevel(), uni2.getUniverse().getPLevel()), CNatOrder.maxCNat(uni1.getUniverse().getHLevel(), uni2.getUniverse().getHLevel()));
  }
}
