package com.jetbrains.jetpad.vclang.typechecking.patternmatching;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.elimtree.*;
import com.jetbrains.jetpad.vclang.core.expr.ConCallExpression;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.expr.ExpressionFactory;
import com.jetbrains.jetpad.vclang.core.expr.ReferenceExpression;
import com.jetbrains.jetpad.vclang.core.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ConditionsError;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.DummyEquations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;
import com.jetbrains.jetpad.vclang.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ConditionsChecking {
  public static boolean check(Body body, List<ElimTypechecking.ClauseData> clauses, Definition definition, LocalErrorReporter errorReporter) {
    boolean ok;
    if (body instanceof IntervalElim) {
      ok = checkIntervals((IntervalElim) body, definition, errorReporter);
      for (ElimTypechecking.ClauseData clause : clauses) {
        if (clause.expression != null && !checkIntervalClause((IntervalElim) body, clause, definition, errorReporter)) {
          ok = false;
        }
      }
    } else {
      ok = true;
    }

    for (ElimTypechecking.ClauseData clause : clauses) {
      if (clause.expression != null && !checkClause(clause, definition, errorReporter)) {
        ok = false;
      }
    }

    return ok;
  }

  private static boolean checkIntervals(IntervalElim elim, Definition definition, LocalErrorReporter errorReporter) {
    boolean ok = true;
    DependentLink link = DependentLink.Helper.get(elim.getParameters(), DependentLink.Helper.size(elim.getParameters()) - elim.getCases().size());
    List<Pair<Expression, Expression>> cases = elim.getCases();
    for (int i = 0; i < cases.size(); i++) {
      DependentLink link2 = link.getNext();
      for (int j = i + 1; j < cases.size(); j++) {
        ok = checkIntervalCondition(cases.get(i), cases.get(j), true, true, link, link2, elim.getParameters(), definition, errorReporter) && ok;
        ok = checkIntervalCondition(cases.get(i), cases.get(j), true, false, link, link2, elim.getParameters(), definition, errorReporter) && ok;
        ok = checkIntervalCondition(cases.get(i), cases.get(j), false, true, link, link2, elim.getParameters(), definition, errorReporter) && ok;
        ok = checkIntervalCondition(cases.get(i), cases.get(j), false, false, link, link2, elim.getParameters(), definition, errorReporter) && ok;
        link2 = link2.getNext();
      }
      link = link.getNext();
    }
    return ok;
  }

  private static boolean checkIntervalCondition(Pair<Expression, Expression> pair1, Pair<Expression, Expression> pair2, boolean isLeft1, boolean isLeft2, DependentLink link1, DependentLink link2, DependentLink parameters, Definition definition, LocalErrorReporter errorReporter) {
    Expression case1 = isLeft1 ? pair1.proj1 : pair1.proj2;
    Expression case2 = isLeft2 ? pair2.proj1 : pair2.proj2;
    if (case1 == null || case2 == null) {
      return true;
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
      return false;
    } else {
      return true;
    }
  }

  private static boolean checkIntervalClause(IntervalElim elim, ElimTypechecking.ClauseData clause, Definition definition, LocalErrorReporter errorReporter) {
    boolean ok = true;
    List<Pair<Expression, Expression>> cases = elim.getCases();
    int prefixLength = DependentLink.Helper.size(elim.getParameters()) - elim.getCases().size();
    for (int i = 0; i < cases.size(); i++) {
      ok = checkIntervalClauseCondition(cases.get(i), true, elim.getParameters(), prefixLength + i, clause, definition, clause.clause, errorReporter) && ok;
      ok = checkIntervalClauseCondition(cases.get(i), false, elim.getParameters(), prefixLength + i, clause, definition, clause.clause, errorReporter) && ok;
    }
    return ok;
  }

  private static boolean checkIntervalClauseCondition(Pair<Expression, Expression> pair, boolean isLeft, DependentLink parameters, int index, ElimTypechecking.ClauseData clause, Definition definition, Abstract.SourceNode sourceNode, LocalErrorReporter errorReporter) {
    Expression expr = isLeft ? pair.proj1 : pair.proj2;
    if (expr == null) {
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
      return false;
    } else {
      return true;
    }
  }

  private static boolean checkClause(ElimTypechecking.ClauseData clause, Definition definition, LocalErrorReporter errorReporter) {
    boolean ok = true;
    for (Pair<List<Expression>, ExprSubstitution> pair : collectPatterns(clause.patterns)) {
      Expression evaluatedExpr1 = definition.getDefCall(Sort.STD, null, pair.proj1).normalize(NormalizeVisitor.Mode.NF);
      Expression evaluatedExpr2 = clause.expression.subst(pair.proj2).normalize(NormalizeVisitor.Mode.NF);
      if (!CompareVisitor.compare(DummyEquations.getInstance(), Equations.CMP.EQ, evaluatedExpr1, evaluatedExpr2, null)) {
        errorReporter.report(new ConditionsError(definition.getDefCall(Sort.STD, null, clause.patterns.stream().map(Pattern::toExpression).collect(Collectors.toList())), clause.expression, evaluatedExpr1, evaluatedExpr2, clause.clause));
        ok = false;
      }
    }
    return ok;
  }

  private static List<Pair<List<Expression>, ExprSubstitution>> collectPatterns(List<Pattern> patterns) {
    List<Pair<List<Expression>, ExprSubstitution>> result = new ArrayList<>();
    for (int i = 0; i < patterns.size(); i++) {
      List<Pair<Expression, ExprSubstitution>> patternResult = collectPatterns(patterns.get(i));
      for (Pair<Expression, ExprSubstitution> pair : patternResult) {
        List<Expression> list = new ArrayList<>(patterns.size());
        for (int j = 0; j < patterns.size(); j++) {
          if (i == j) {
            list.add(pair.proj1);
          } else {
            list.add(patterns.get(j).toExpression());
          }
        }
        result.add(new Pair<>(list, pair.proj2));
      }
    }
    return result;
  }

  private static List<Pair<Expression, ExprSubstitution>> collectPatterns(Pattern pattern) {
    if (pattern instanceof BindingPattern) {
      return Collections.emptyList();
    }
    ConstructorPattern conPattern = (ConstructorPattern) pattern;
    List<Pair<Expression, ExprSubstitution>> result = collectPatterns(conPattern.getArguments()).stream().map(p -> new Pair<Expression, ExprSubstitution>(new ConCallExpression(conPattern.getConstructor(), conPattern.getSortArgument(), conPattern.getDataTypeArguments(), p.proj1), p.proj2)).collect(Collectors.toList());

    if (conPattern.getConstructor().getBody() != null) {
      if (conPattern.getConstructor().getBody() instanceof IntervalElim) {
        IntervalElim elim = (IntervalElim) conPattern.getConstructor().getBody();
        int prefixLength = conPattern.getArguments().size() - elim.getCases().size();
        for (int i = 0; i < elim.getCases().size(); i++) {
          if (elim.getCases().get(i).proj1 == null && elim.getCases().get(i).proj2 == null) {
            continue;
          }

          ExprSubstitution substitution = new ExprSubstitution();
          int j = 0;
          for (DependentLink link = elim.getParameters(); link.hasNext(); link = link.getNext(), j++) {
            if (j != i) {
              substitution.add(link, conPattern.getArguments().get(j).toExpression());
            }
          }

          Pattern pattern1 = conPattern.getArguments().get(prefixLength + i);
          if (pattern1 instanceof BindingPattern) {
            if (elim.getCases().get(i).proj1 != null) {
              result.add(new Pair<>(elim.getCases().get(i).proj1.subst(substitution), new ExprSubstitution(((BindingPattern) pattern1).getBinding(), ExpressionFactory.Left())));
            }
            if (elim.getCases().get(i).proj2 != null) {
              result.add(new Pair<>(elim.getCases().get(i).proj2.subst(substitution), new ExprSubstitution(((BindingPattern) pattern1).getBinding(), ExpressionFactory.Right())));
            }
          } else
          if (pattern1 instanceof ConstructorPattern && (((ConstructorPattern) pattern1).getConstructor() == Prelude.LEFT || ((ConstructorPattern) pattern1).getConstructor() == Prelude.RIGHT)) {
            Expression expr;
            if (((ConstructorPattern) pattern1).getConstructor() == Prelude.LEFT && elim.getCases().get(i).proj1 != null) {
              expr = elim.getCases().get(i).proj1;
            } else
            if (((ConstructorPattern) pattern1).getConstructor() == Prelude.RIGHT && elim.getCases().get(i).proj2 != null) {
              expr = elim.getCases().get(i).proj2;
            } else {
              continue;
            }

            result.add(new Pair<>(expr.subst(substitution), new ExprSubstitution()));
          }
        }
      }
    }

    return result;
  }
}
