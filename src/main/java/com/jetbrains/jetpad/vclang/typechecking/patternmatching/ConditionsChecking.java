package com.jetbrains.jetpad.vclang.typechecking.patternmatching;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.elimtree.BindingPattern;
import com.jetbrains.jetpad.vclang.core.elimtree.Body;
import com.jetbrains.jetpad.vclang.core.elimtree.IntervalElim;
import com.jetbrains.jetpad.vclang.core.elimtree.Pattern;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory;
import com.jetbrains.jetpad.vclang.core.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.core.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ConditionsError;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.DummyEquations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;
import com.jetbrains.jetpad.vclang.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ConditionsChecking {
  public static boolean check(Body body, List<ElimTypechecking.ClauseData> clauses, Definition definition, LocalErrorReporter errorReporter) {
    boolean ok;
    if (body instanceof IntervalElim) {
      ok = checkIntervals((IntervalElim) body, definition, errorReporter);
      for (ElimTypechecking.ClauseData clause : clauses) {
        if (!checkIntervalClause((IntervalElim) body, clause, definition, errorReporter)) {
          ok = false;
        }
      }
    } else {
      ok = true;
    }

    for (ElimTypechecking.ClauseData clause : clauses) {
      if (!checkClause(clause, body, errorReporter)) {
        ok = false;
      }
    }

    return ok;
  }

  private static boolean checkIntervals(IntervalElim elim, Definition definition, LocalErrorReporter errorReporter) {
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
      errorReporter.report(new ConditionsError(definition.getDefCall(Sort.STD, null, defCallArgs1), definition.getDefCall(Sort.STD, null, defCallArgs2), evaluatedExpr1, evaluatedExpr2, definition.getAbstractDefinition()));
    }
  }

  private static boolean checkIntervalClause(IntervalElim elim, ElimTypechecking.ClauseData clause, Definition definition, LocalErrorReporter errorReporter) {
    boolean ok = true;
    List<Pair<Expression, Expression>> cases = elim.getCases();
    int prefixLength = DependentLink.Helper.size(elim.getParameters()) - elim.getCases().size();
    for (int i = 0; i < cases.size(); i++) {
      if (!checkIntervalClauseCondition(cases.get(i), true, elim.getParameters(), prefixLength + i, clause, definition, clause.clause, errorReporter)) {
        ok = false;
      }
      if (!checkIntervalClauseCondition(cases.get(i), false, elim.getParameters(), prefixLength + i, clause, definition, clause.clause, errorReporter)) {
        ok = false;
      }
    }
    return ok;
  }

  private static boolean checkIntervalClauseCondition(Pair<Expression, Expression> pair, boolean isLeft, DependentLink parameters, int index, ElimTypechecking.ClauseData clause, Definition definition, Abstract.SourceNode sourceNode, LocalErrorReporter errorReporter) {
    Expression expr = isLeft ? pair.proj1 : pair.proj2;
    if (expr == null || clause.expression == null) {
      return true;
    }

    ExprSubstitution substitution = new ExprSubstitution();
    DependentLink link = parameters;
    for (int i = 0; i < clause.patterns.size(); i++) {
      if (i != index) {
        substitution.add(link, clause.patterns.get(i).toExpression());
      }
      link = link.getNext();
    }
    Expression evaluatedExpr1 = expr.subst(substitution).normalize(NormalizeVisitor.Mode.NF);
    Expression evaluatedExpr2 = clause.expression.subst(new ExprSubstitution(((BindingPattern) clause.patterns.get(index)).getBinding(), isLeft ? ExpressionFactory.Left() : ExpressionFactory.Right())).normalize(NormalizeVisitor.Mode.NF);
    if (!CompareVisitor.compare(DummyEquations.getInstance(), Equations.CMP.EQ, evaluatedExpr1, evaluatedExpr2, null)) {
      List<Expression> defCallArgs1 = new ArrayList<>();
      int i = 0;
      for (link = parameters; link.hasNext(); link = link.getNext(), i++) {
        defCallArgs1.add(i == index ? (isLeft ? ExpressionFactory.Left() : ExpressionFactory.Right()) : new ReferenceExpression(link));
      }
      List<Expression> defCallArgs2 = clause.patterns.stream().map(Pattern::toExpression).collect(Collectors.toList());
      errorReporter.report(new ConditionsError(definition.getDefCall(Sort.STD, null, defCallArgs1), definition.getDefCall(Sort.STD, null, defCallArgs2), evaluatedExpr1, evaluatedExpr2, clause.clause));
    }

    return true;
  }

  private static boolean checkClause(ElimTypechecking.ClauseData clause, Body body, LocalErrorReporter errorReporter) {

    return true;
  }
}
