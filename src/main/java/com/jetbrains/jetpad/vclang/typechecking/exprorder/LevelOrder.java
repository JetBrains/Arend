package com.jetbrains.jetpad.vclang.typechecking.exprorder;

import com.jetbrains.jetpad.vclang.term.Preprelude;
import com.jetbrains.jetpad.vclang.term.context.binding.InferenceBinding;
import com.jetbrains.jetpad.vclang.term.expr.*;
import com.jetbrains.jetpad.vclang.term.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;

public class LevelOrder implements ExpressionOrder {
  public static boolean compareLevel(Expression expr1, Expression expr2, CompareVisitor visitor, Equations.CMP expectedCMP) {
    return new LevelOrder().compare(expr1, expr2, visitor, expectedCMP);
  }

  public static Expression maxLevel(Expression expr1, Expression expr2) {
    return new LevelOrder().max(expr1, expr2);
  }

  @Override
  public boolean compare(Expression expr1, Expression expr2, CompareVisitor visitor, Equations.CMP expectedCMP) {
    ReferenceExpression ref1 = expr1.toReference();
    ReferenceExpression ref2 = expr2.toReference();

    if ((ref1 != null && ref1.getBinding() instanceof InferenceBinding) || (ref2 != null && ref2.getBinding() instanceof InferenceBinding)) {
      return visitor.compare(expr1, expr2);
    }

    NewExpression new1 = expr1.toNew();
    NewExpression new2 = expr2.toNew();

    if (new1 == null || new2 == null) {
      return false;
    }

    ClassCallExpression classCall1 = new1.getExpression().toClassCall();
    ClassCallExpression classCall2 = new2.getExpression().toClassCall();

    if (classCall1 == null || classCall2 == null) {
      return false;
    }

    boolean cmp1 = LvlOrder.compareLvl(classCall1.getImplementStatements().get(Preprelude.PLEVEL).term, classCall2.getImplementStatements().get(Preprelude.PLEVEL).term, visitor, expectedCMP);
    boolean cmp2 = CNatOrder.compareCNat(classCall1.getImplementStatements().get(Preprelude.HLEVEL).term, classCall2.getImplementStatements().get(Preprelude.HLEVEL).term, visitor, expectedCMP);

    return cmp1 && cmp2;
  }

  @Override
  public Expression max(Expression expr1, Expression expr2) {
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
