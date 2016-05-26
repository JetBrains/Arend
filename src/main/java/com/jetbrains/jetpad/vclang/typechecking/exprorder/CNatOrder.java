package com.jetbrains.jetpad.vclang.typechecking.exprorder;

import com.jetbrains.jetpad.vclang.term.Preprelude;
import com.jetbrains.jetpad.vclang.term.definition.TypeUniverse;
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

  @Override
  public boolean isComparable(Expression expr) {
    Expression type = expr.getType().normalize(NormalizeVisitor.Mode.NF);
    DataCallExpression dataCall = type.toDataCall();

    return dataCall != null && dataCall.getDefinition() == Preprelude.CNAT;
  }

  @Override
  public Boolean compare(Expression expr1, Expression expr2, CompareVisitor visitor, Equations.CMP expectedCMP) {
    DefCallExpression type1 = expr1.getType().normalize(NormalizeVisitor.Mode.NF).toDefCall();
    DefCallExpression type2 = expr2.getType().normalize(NormalizeVisitor.Mode.NF).toDefCall();
    if (type1 == null || type2 == null || type1.getDefinition() != Preprelude.CNAT || type2.getDefinition() != Preprelude.CNAT) {
      return null;
    }
    return LevelExprOrder.compareLevel(TypeUniverse.exprToHLevel(expr1), TypeUniverse.exprToHLevel(expr2), visitor, expectedCMP);
  }

  @Override
  public Expression max(Expression expr1, Expression expr2) {
    if (Expression.compare(expr1, expr2, Equations.CMP.GE)) {
      return expr1;
    }

    if (Expression.compare(expr1, expr2, Equations.CMP.LE)) {
      return expr2;
    }/**/

    return LevelExprOrder.maxLevel(TypeUniverse.exprToHLevel(expr1), TypeUniverse.exprToHLevel(expr2));
  }
}
