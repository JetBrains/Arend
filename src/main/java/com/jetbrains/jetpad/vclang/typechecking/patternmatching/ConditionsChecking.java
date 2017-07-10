package com.jetbrains.jetpad.vclang.typechecking.patternmatching;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.elimtree.Body;
import com.jetbrains.jetpad.vclang.core.elimtree.ElimTree;
import com.jetbrains.jetpad.vclang.core.elimtree.IntervalElim;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory;
import com.jetbrains.jetpad.vclang.core.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.core.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.error.ErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ConditionsError;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.DummyEquations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;
import com.jetbrains.jetpad.vclang.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class ConditionsChecking {
  public static boolean check(Body body, Definition definition, LocalErrorReporter errorReporter) {
    boolean ok;
    ElimTree elimTree;
    if (body instanceof IntervalElim) {
      ok = checkInterval((IntervalElim) body, definition, errorReporter);
      elimTree = ((IntervalElim) body).getOtherwise();
    } else {
      ok = true;
      elimTree = (ElimTree) body;
    }

    return checkElimTree(elimTree, errorReporter) && ok;
  }

  private static boolean checkInterval(IntervalElim elim, Definition definition, LocalErrorReporter errorReporter) {
    DependentLink link = DependentLink.Helper.get(elim.getParameters(), DependentLink.Helper.size(elim.getParameters()) - elim.getCases().size());
    List<Pair<Expression, Expression>> cases = elim.getCases();
    for (int i = 0; i < cases.size(); i++) {
      DependentLink link2 = link.getNext();
      for (int j = i + 1; j < cases.size(); j++) {
        checkIntervalCondition(cases.get(i), cases.get(j), true, true, link, link2, elim.getParameters(), definition, errorReporter);
        checkIntervalCondition(cases.get(i), cases.get(j), true, false, link, link2, elim.getParameters(), definition, errorReporter);
        checkIntervalCondition(cases.get(i), cases.get(j), false, true, link, link2, elim.getParameters(), definition, errorReporter);
        checkIntervalCondition(cases.get(i), cases.get(j), false, false, link, link2, elim.getParameters(), definition, errorReporter);
        link2 = link2.getNext();
      }
      link = link.getNext();
    }
    return true;
  }

  private static void checkIntervalCondition(Pair<Expression, Expression> pair1, Pair<Expression, Expression> pair2, boolean isLeft1, boolean isLeft2, DependentLink link1, DependentLink link2, DependentLink parameters, Definition definition, LocalErrorReporter errorReporter) {
    Expression case1 = isLeft1 ? pair1.proj1 : pair1.proj2;
    Expression case2 = isLeft2 ? pair2.proj1 : pair2.proj2;
    if (case1 == null || case2 == null) {
      return;
    }

    Expression evaluatedExpr1 = case1.subst(new ExprSubstitution(link2, isLeft2 ? ExpressionFactory.Left() : ExpressionFactory.Right())).normalize(NormalizeVisitor.Mode.NF);
    Expression evaluatedExpr2 = case2.subst(new ExprSubstitution(link1, isLeft1 ? ExpressionFactory.Left() : ExpressionFactory.Right())).normalize(NormalizeVisitor.Mode.NF);
    if (!CompareVisitor.compare(DummyEquations.getInstance(), Equations.CMP.EQ, evaluatedExpr1, evaluatedExpr2, null)) {
      List<Expression> defCallArgs1 = new ArrayList<>();
      for (DependentLink link3 = parameters; link3.hasNext(); link3 = link3.getNext()) {
        defCallArgs1.add(link3 == link1 ? (isLeft1 ? ExpressionFactory.Left() : ExpressionFactory.Right()) : new ReferenceExpression(link3));
      }
      List<Expression> defCallArgs2 = new ArrayList<>();
      for (DependentLink link3 = parameters; link3.hasNext(); link3 = link3.getNext()) {
        defCallArgs2.add(link3 == link2 ? (isLeft2 ? ExpressionFactory.Left() : ExpressionFactory.Right()) : new ReferenceExpression(link3));
      }
      errorReporter.report(new ConditionsError(definition.getAbstractDefinition(), definition.getDefCall(Sort.STD, null, defCallArgs1), definition.getDefCall(Sort.STD, null, defCallArgs2), evaluatedExpr1, evaluatedExpr2));
    }
  }

  private static boolean checkElimTree(ElimTree elimTree, ErrorReporter errorReporter) {
    return true;
  }
}
