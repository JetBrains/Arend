package com.jetbrains.jetpad.vclang.typechecking.exprorder;

import com.jetbrains.jetpad.vclang.term.Preprelude;
import com.jetbrains.jetpad.vclang.term.expr.ClassCallExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory;
import com.jetbrains.jetpad.vclang.term.expr.NewExpression;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

public class LevelOrder implements ExpressionOrder {
  public static Boolean compareLevel(Expression expr1, Expression expr2, CompareVisitor visitor, Equations.CMP expectedCMP) {
    return new LevelOrder().compare(expr1, expr2, visitor, expectedCMP);
  }

  public static Expression maxLevel(Expression expr1, Expression expr2) {
    return new LevelOrder().max(expr1, expr2);
  }

  @Override
  public boolean isComparable(Expression expr) {
    Expression type = expr.getType().normalize(NormalizeVisitor.Mode.NF);
    ClassCallExpression classCall = type.toClassCall();

    return classCall != null && classCall.getDefinition() == Preprelude.LEVEL;
  }

  @Override
  public Boolean compare(Expression expr1, Expression expr2, CompareVisitor visitor, Equations.CMP expectedCMP) {
    NewExpression new1 = expr1.toNew();
    NewExpression new2 = expr2.toNew();

    if (new1 == null || new2 == null) {
      return null;
    }

    ClassCallExpression classCall1 = new1.getExpression().toClassCall();
    ClassCallExpression classCall2 = new2.getExpression().toClassCall();

    if (classCall1 == null || classCall2 == null) {
      return null;
    }

    if (!classCall1.getImplementStatements().containsKey(Preprelude.HLEVEL) || !classCall1.getImplementStatements().containsKey(Preprelude.PLEVEL) ||
            !classCall2.getImplementStatements().containsKey(Preprelude.HLEVEL) || !classCall2.getImplementStatements().containsKey(Preprelude.PLEVEL)) {
      return null;
    }

    Expression hlevel1 = classCall1.getImplementStatements().get(Preprelude.HLEVEL).term;
    Expression hlevel2 = classCall2.getImplementStatements().get(Preprelude.HLEVEL).term;

    boolean cmp1 = visitor.compare(hlevel1, hlevel2); // CNatOrder.compareCNat(hlevel1, hlevel2, visitor, expectedCMP);
    boolean cmp2 = visitor.compare(classCall1.getImplementStatements().get(Preprelude.PLEVEL).term, classCall2.getImplementStatements().get(Preprelude.PLEVEL).term);

    if (CNatOrder.isZero(hlevel1) || CNatOrder.isZero(hlevel2)) {
      return cmp1;
    }

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

    NewExpression new1 = expr1.toNew();
    NewExpression new2 = expr2.toNew();
    Expression plevel1, plevel2, hlevel1, hlevel2;

    if (new1 != null) {
      ClassCallExpression classCall = new1.getExpression().toClassCall();
      plevel1 = classCall.getImplementStatements().get(Preprelude.PLEVEL).term;
      hlevel1 = classCall.getImplementStatements().get(Preprelude.HLEVEL).term;
    } else {
      plevel1 = ExpressionFactory.PLevel().applyThis(expr1);
      hlevel1 = ExpressionFactory.HLevel().applyThis(expr1);
    }

    if (new2 != null) {
      ClassCallExpression classCall = new2.getExpression().toClassCall();
      plevel2 = classCall.getImplementStatements().get(Preprelude.PLEVEL).term;
      hlevel2 = classCall.getImplementStatements().get(Preprelude.HLEVEL).term;
    } else {
      plevel2 = ExpressionFactory.PLevel().applyThis(expr2);
      hlevel2 = ExpressionFactory.HLevel().applyThis(expr2);
    }

    return ExpressionFactory.Level(LvlOrder.maxLvl(plevel1, plevel2), CNatOrder.maxCNat(hlevel1, hlevel2));
  }
}
