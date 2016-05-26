package com.jetbrains.jetpad.vclang.typechecking.exprorder;

import com.jetbrains.jetpad.vclang.term.Preprelude;
import com.jetbrains.jetpad.vclang.term.definition.TypeUniverse;
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
    DefCallExpression type1 = expr1.getType().normalize(NormalizeVisitor.Mode.NF).toDefCall();
    DefCallExpression type2 = expr2.getType().normalize(NormalizeVisitor.Mode.NF).toDefCall();
    if (type1 == null || type2 == null || type1.getDefinition() != Preprelude.LVL || type2.getDefinition() != Preprelude.LVL) {
      return null;
    }
    return LevelExprOrder.compareLevel(TypeUniverse.exprToPLevel(expr1), TypeUniverse.exprToPLevel(expr2), visitor, expectedCMP);
  }

  @Override
  public Expression max(Expression expr1, Expression expr2) {
    if (Expression.compare(expr1, expr2, Equations.CMP.GE)) {
      return expr1;
    }
    if (Expression.compare(expr1, expr2, Equations.CMP.LE)) {
      return expr2;
    }/**/
    return LevelExprOrder.maxLevel(TypeUniverse.exprToPLevel(expr1), TypeUniverse.exprToPLevel(expr2));
  }
}
