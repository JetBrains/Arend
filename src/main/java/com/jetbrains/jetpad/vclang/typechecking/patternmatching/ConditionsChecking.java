package com.jetbrains.jetpad.vclang.typechecking.patternmatching;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.EmptyDependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.SingleDependentLink;
import com.jetbrains.jetpad.vclang.core.context.param.TypedSingleDependentLink;
import com.jetbrains.jetpad.vclang.core.definition.Constructor;
import com.jetbrains.jetpad.vclang.core.definition.Definition;
import com.jetbrains.jetpad.vclang.core.elimtree.*;
import com.jetbrains.jetpad.vclang.core.expr.*;
import com.jetbrains.jetpad.vclang.core.expr.visitor.CompareVisitor;
import com.jetbrains.jetpad.vclang.core.expr.visitor.NormalizeVisitor;
import com.jetbrains.jetpad.vclang.core.pattern.BindingPattern;
import com.jetbrains.jetpad.vclang.core.pattern.ConstructorPattern;
import com.jetbrains.jetpad.vclang.core.pattern.Pattern;
import com.jetbrains.jetpad.vclang.core.pattern.Patterns;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.core.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.prelude.Prelude;
import com.jetbrains.jetpad.vclang.term.concrete.Concrete;
import com.jetbrains.jetpad.vclang.typechecking.error.LocalErrorReporter;
import com.jetbrains.jetpad.vclang.typechecking.error.local.ConditionsError;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.DummyEquations;
import com.jetbrains.jetpad.vclang.typechecking.implicitargs.equations.Equations;
import com.jetbrains.jetpad.vclang.util.Pair;

import java.util.*;

public class ConditionsChecking {
  public static boolean check(Body body, List<Clause> clauses, Definition definition, Concrete.SourceNode def, LocalErrorReporter errorReporter) {
    boolean ok;
    if (body instanceof IntervalElim) {
      ok = checkIntervals((IntervalElim) body, definition, def, errorReporter);
      for (Clause clause : clauses) {
        if (clause.expression != null && !checkIntervalClause((IntervalElim) body, clause, definition, errorReporter)) {
          ok = false;
        }
      }
    } else {
      ok = true;
    }

    for (Clause clause : clauses) {
      if (!checkClause(clause, null, definition, errorReporter)) {
        ok = false;
      }
    }

    return ok;
  }

  private static boolean checkIntervals(IntervalElim elim, Definition definition, Concrete.SourceNode def, LocalErrorReporter errorReporter) {
    boolean ok = true;
    DependentLink link = DependentLink.Helper.get(elim.getParameters(), DependentLink.Helper.size(elim.getParameters()) - elim.getCases().size());
    List<Pair<Expression, Expression>> cases = elim.getCases();
    for (int i = 0; i < cases.size(); i++) {
      DependentLink link2 = link.getNext();
      for (int j = i + 1; j < cases.size(); j++) {
        ok = checkIntervalCondition(cases.get(i), cases.get(j), true, true, link, link2, elim.getParameters(), definition, def, errorReporter) && ok;
        ok = checkIntervalCondition(cases.get(i), cases.get(j), true, false, link, link2, elim.getParameters(), definition, def, errorReporter) && ok;
        ok = checkIntervalCondition(cases.get(i), cases.get(j), false, true, link, link2, elim.getParameters(), definition, def, errorReporter) && ok;
        ok = checkIntervalCondition(cases.get(i), cases.get(j), false, false, link, link2, elim.getParameters(), definition, def, errorReporter) && ok;
        link2 = link2.getNext();
      }
      link = link.getNext();
    }
    return ok;
  }

  private static boolean checkIntervalCondition(Pair<Expression, Expression> pair1, Pair<Expression, Expression> pair2, boolean isLeft1, boolean isLeft2, DependentLink link1, DependentLink link2, DependentLink parameters, Definition definition, Concrete.SourceNode def, LocalErrorReporter errorReporter) {
    Expression case1 = isLeft1 ? pair1.proj1 : pair1.proj2;
    Expression case2 = isLeft2 ? pair2.proj1 : pair2.proj2;
    if (case1 == null || case2 == null) {
      return true;
    }

    ExprSubstitution substitution1 = new ExprSubstitution(link2, isLeft2 ? ExpressionFactory.Left() : ExpressionFactory.Right());
    Expression evaluatedExpr1 = case1.subst(substitution1);
    ExprSubstitution substitution2 = new ExprSubstitution(link1, isLeft1 ? ExpressionFactory.Left() : ExpressionFactory.Right());
    Expression evaluatedExpr2 = case2.subst(substitution2);
    if (!CompareVisitor.compare(DummyEquations.getInstance(), Equations.CMP.EQ, evaluatedExpr1, evaluatedExpr2, null)) {
      List<Expression> defCallArgs1 = new ArrayList<>();
      for (DependentLink link3 = parameters; link3.hasNext(); link3 = link3.getNext()) {
        defCallArgs1.add(link3 == link1 ? (isLeft1 ? ExpressionFactory.Left() : ExpressionFactory.Right()) : new ReferenceExpression(link3));
      }
      List<Expression> defCallArgs2 = new ArrayList<>();
      for (DependentLink link3 = parameters; link3.hasNext(); link3 = link3.getNext()) {
        defCallArgs2.add(link3 == link2 ? (isLeft2 ? ExpressionFactory.Left() : ExpressionFactory.Right()) : new ReferenceExpression(link3));
      }
      errorReporter.report(new ConditionsError(definition.getDefCall(Sort.STD, defCallArgs1), definition.getDefCall(Sort.STD, defCallArgs2), substitution1, substitution2, evaluatedExpr1, evaluatedExpr2, def));
      return false;
    } else {
      return true;
    }
  }

  private static boolean checkIntervalClause(IntervalElim elim, Clause clause, Definition definition, LocalErrorReporter errorReporter) {
    boolean ok = true;
    List<Pair<Expression, Expression>> cases = elim.getCases();
    int prefixLength = DependentLink.Helper.size(elim.getParameters()) - elim.getCases().size();
    for (int i = 0; i < cases.size(); i++) {
      ok = checkIntervalClauseCondition(cases.get(i), true, elim.getParameters(), prefixLength + i, clause, definition, errorReporter) && ok;
      ok = checkIntervalClauseCondition(cases.get(i), false, elim.getParameters(), prefixLength + i, clause, definition, errorReporter) && ok;
    }
    return ok;
  }

  private static boolean checkIntervalClauseCondition(Pair<Expression, Expression> pair, boolean isLeft, DependentLink parameters, int index, Clause clause, Definition definition, LocalErrorReporter errorReporter) {
    Expression expr = isLeft ? pair.proj1 : pair.proj2;
    if (expr == null) {
      return true;
    }

    ExprSubstitution pathSubstitution = new ExprSubstitution();
    collectPaths(clause.patterns, pathSubstitution);

    ExprSubstitution substitution1 = new ExprSubstitution();
    DependentLink link = parameters;
    for (int i = 0; i < clause.patterns.size(); i++) {
      if (i != index) {
        substitution1.add(link, clause.patterns.get(i).toExpression().subst(pathSubstitution));
      }
      link = link.getNext();
    }

    ExprSubstitution substitution2 = new ExprSubstitution(((BindingPattern) clause.patterns.get(index)).getBinding(), isLeft ? ExpressionFactory.Left() : ExpressionFactory.Right());
    pathSubstitution.addAll(substitution2);

    Expression evaluatedExpr1 = expr.subst(substitution1);
    Expression evaluatedExpr2 = clause.expression.subst(pathSubstitution);
    if (!CompareVisitor.compare(DummyEquations.getInstance(), Equations.CMP.EQ, evaluatedExpr1, evaluatedExpr2, null)) {
      if (!pathSubstitution.isEmpty()) {
        link = parameters;
        for (int i = 0; i < clause.patterns.size(); i++) {
          if (i != index) {
            substitution1.add(link, clause.patterns.get(i).toExpression());
          }
          link = link.getNext();
        }
      }

      List<Expression> defCallArgs1 = new ArrayList<>();
      int i = 0;
      for (link = parameters; link.hasNext(); link = link.getNext(), i++) {
        defCallArgs1.add(i == index ? (isLeft ? ExpressionFactory.Left() : ExpressionFactory.Right()) : new ReferenceExpression(link));
      }

      List<Expression> defCallArgs2 = new ArrayList<>(clause.patterns.size());
      for (Pattern pattern : clause.patterns) {
        defCallArgs2.add(pattern.toExpression());
      }

      if (!pathSubstitution.isEmpty()) {
        substitution1 = new ExprSubstitution();
        link = parameters;
        for (i = 0; i < clause.patterns.size(); i++) {
          if (i != index) {
            substitution1.add(link, clause.patterns.get(i).toExpression());
          }
          link = link.getNext();
        }
      }

      errorReporter.report(new ConditionsError(definition.getDefCall(Sort.STD, defCallArgs1), definition.getDefCall(Sort.STD, defCallArgs2), substitution1, substitution2, evaluatedExpr1, evaluatedExpr2, clause.clause));
      return false;
    } else {
      return true;
    }
  }

  private static void collectPaths(List<Pattern> patterns, ExprSubstitution substitution) {
    for (Pattern pattern : patterns) {
      if (pattern instanceof ConstructorPattern) {
        ConstructorPattern conPattern = (ConstructorPattern) pattern;
        if (conPattern.getConstructor() == Prelude.PATH_CON) {
          SingleDependentLink lamParam = new TypedSingleDependentLink(true, "i", ExpressionFactory.Interval());
          Expression lamRef = new ReferenceExpression(lamParam);
          Map<Constructor, ElimTree> children = new HashMap<>();
          children.put(Prelude.LEFT, new LeafElimTree(EmptyDependentLink.getInstance(), conPattern.getDataTypeArguments().get(1)));
          children.put(Prelude.RIGHT, new LeafElimTree(EmptyDependentLink.getInstance(), conPattern.getDataTypeArguments().get(2)));
          children.put(null, new LeafElimTree(lamParam, new AppExpression(conPattern.getArguments().get(0).toExpression(), lamRef)));
          substitution.add(((BindingPattern) conPattern.getArguments().get(0)).getBinding(), new LamExpression(conPattern.getSortArgument(), lamParam, new CaseExpression(lamParam, new AppExpression(conPattern.getDataTypeArguments().get(0), lamRef), new BranchElimTree(EmptyDependentLink.getInstance(), children), Collections.singletonList(lamRef))));
        }
      }
    }
  }

  @SuppressWarnings("UnusedReturnValue")
  public static boolean check(List<Clause> clauses, ElimTree elimTree, LocalErrorReporter errorReporter) {
    boolean ok = true;
    for (Clause clause : clauses) {
      if (!checkClause(clause, elimTree, null, errorReporter)) {
        ok = false;
      }
    }
    return ok;
  }

  private static boolean checkClause(Clause clause, ElimTree elimTree, Definition definition, LocalErrorReporter errorReporter) {
    if (clause.expression == null || clause.expression.isInstance(ErrorExpression.class)) {
      return true;
    }

    boolean ok = true;
    for (Pair<List<Expression>, ExprSubstitution> pair : collectPatterns(clause.patterns)) {
      Expression evaluatedExpr1;
      if (definition == null) {
        evaluatedExpr1 = NormalizeVisitor.INSTANCE.eval(elimTree, pair.proj1, new ExprSubstitution(), LevelSubstitution.EMPTY);
      } else {
        evaluatedExpr1 = definition.getDefCall(Sort.STD, pair.proj1);
      }
      Expression evaluatedExpr2 = clause.expression.subst(pair.proj2);
      if (evaluatedExpr1 == null || !CompareVisitor.compare(DummyEquations.getInstance(), Equations.CMP.EQ, evaluatedExpr1, evaluatedExpr2, null)) {
        List<Expression> args = new ArrayList<>(clause.patterns.size());
        for (Pattern pattern : clause.patterns) {
          args.add(pattern.toExpression());
        }
        Expression expr1 = definition == null ? new CaseExpression(null, null, null, args) : definition.getDefCall(Sort.STD, args);
        errorReporter.report(new ConditionsError(expr1, clause.expression, pair.proj2, pair.proj2, evaluatedExpr1, evaluatedExpr2, clause.clause));
        ok = false;
      }
    }
    return ok;
  }

  private static List<Pair<List<Expression>, ExprSubstitution>> collectPatterns(List<Pattern> patterns) {
    List<Pair<List<Expression>, ExprSubstitution>> result = new ArrayList<>();
    for (int i = 0; i < patterns.size(); i++) {
      for (Pair<Expression, ExprSubstitution> pair : collectPatterns(patterns.get(i))) {
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
    List<Pair<List<Expression>, ExprSubstitution>> collectedPatterns = collectPatterns(conPattern.getArguments());
    List<Pair<Expression, ExprSubstitution>> result = new ArrayList<>(collectedPatterns.size());
    for (Pair<List<Expression>, ExprSubstitution> pair : collectedPatterns) {
      result.add(new Pair<>(new ConCallExpression(conPattern.getConstructor(), conPattern.getSortArgument(), conPattern.getDataTypeArguments(), pair.proj1), pair.proj2));
    }

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
          substitution.add(link, conPattern.getArguments().get(j).toExpression());
        }
        j = 0;
        for (DependentLink link = conPattern.getConstructor().getDataTypeParameters(); link.hasNext(); link = link.getNext(), j++) {
          substitution.add(link, conPattern.getDataTypeArguments().get(j));
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

    for (ClauseBase clause : conPattern.getConstructor().getClauses()) {
      ExprSubstitution substitution1 = new ExprSubstitution();
      ExprSubstitution substitution2 = new ExprSubstitution();
      if (conPattern.getPatterns().unify(new Patterns(clause.patterns), substitution1, substitution2)) {
        result.add(new Pair<>(clause.expression.subst(substitution2), substitution1));
      }
    }

    return result;
  }
}
