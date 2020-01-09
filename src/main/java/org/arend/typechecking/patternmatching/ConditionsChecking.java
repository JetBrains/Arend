package org.arend.typechecking.patternmatching;

import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.context.param.TypedSingleDependentLink;
import org.arend.core.definition.Constructor;
import org.arend.core.definition.Definition;
import org.arend.core.elimtree.*;
import org.arend.core.expr.*;
import org.arend.core.expr.visitor.CompareVisitor;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.pattern.BindingPattern;
import org.arend.core.pattern.ConstructorPattern;
import org.arend.core.pattern.Pattern;
import org.arend.core.pattern.Patterns;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.error.ErrorReporter;
import org.arend.ext.core.elimtree.CoreBranchKey;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.ConditionsError;
import org.arend.typechecking.implicitargs.equations.Equations;
import org.arend.util.Pair;

import java.util.*;

public class ConditionsChecking {
  private final Equations myEquations;
  private final ErrorReporter myErrorReporter;

  public ConditionsChecking(Equations equations, ErrorReporter errorReporter) {
    myEquations = equations;
    myErrorReporter = errorReporter;
  }

  public boolean check(Body body, List<ExtClause> clauses, Definition definition, Concrete.SourceNode def) {
    boolean ok;
    if (body instanceof IntervalElim) {
      ok = checkIntervals((IntervalElim) body, definition, def);
      for (Clause clause : clauses) {
        if (clause.expression != null && !checkIntervalClause((IntervalElim) body, clause, definition)) {
          ok = false;
        }
      }
    } else {
      ok = true;
    }

    for (ExtClause clause : clauses) {
      if (!checkClause(clause, null, definition)) {
        ok = false;
      }
    }

    return ok;
  }

  private boolean checkIntervals(IntervalElim elim, Definition definition, Concrete.SourceNode def) {
    boolean ok = true;
    DependentLink link = DependentLink.Helper.get(definition.getParameters(), DependentLink.Helper.size(definition.getParameters()) - elim.getCases().size());
    List<IntervalElim.CasePair> cases = elim.getCases();
    for (int i = 0; i < cases.size(); i++) {
      DependentLink link2 = link.getNext();
      for (int j = i + 1; j < cases.size(); j++) {
        ok = checkIntervalCondition(cases.get(i), cases.get(j), true, true, link, link2, definition, def) && ok;
        ok = checkIntervalCondition(cases.get(i), cases.get(j), true, false, link, link2, definition, def) && ok;
        ok = checkIntervalCondition(cases.get(i), cases.get(j), false, true, link, link2, definition, def) && ok;
        ok = checkIntervalCondition(cases.get(i), cases.get(j), false, false, link, link2, definition, def) && ok;
        link2 = link2.getNext();
      }
      link = link.getNext();
    }
    return ok;
  }

  private boolean checkIntervalCondition(Pair<Expression, Expression> pair1, Pair<Expression, Expression> pair2, boolean isLeft1, boolean isLeft2, DependentLink link1, DependentLink link2, Definition definition, Concrete.SourceNode def) {
    Expression case1 = isLeft1 ? pair1.proj1 : pair1.proj2;
    Expression case2 = isLeft2 ? pair2.proj1 : pair2.proj2;
    if (case1 == null || case2 == null) {
      return true;
    }

    ExprSubstitution substitution1 = new ExprSubstitution(link2, isLeft2 ? ExpressionFactory.Left() : ExpressionFactory.Right());
    Expression evaluatedExpr1 = case1.subst(substitution1);
    ExprSubstitution substitution2 = new ExprSubstitution(link1, isLeft1 ? ExpressionFactory.Left() : ExpressionFactory.Right());
    Expression evaluatedExpr2 = case2.subst(substitution2);
    if (!CompareVisitor.compare(myEquations, Equations.CMP.EQ, evaluatedExpr1, evaluatedExpr2, null, def)) {
      List<Expression> defCallArgs1 = new ArrayList<>();
      for (DependentLink link3 = definition.getParameters(); link3.hasNext(); link3 = link3.getNext()) {
        defCallArgs1.add(link3 == link1 ? (isLeft1 ? ExpressionFactory.Left() : ExpressionFactory.Right()) : new ReferenceExpression(link3));
      }
      List<Expression> defCallArgs2 = new ArrayList<>();
      for (DependentLink link3 = definition.getParameters(); link3.hasNext(); link3 = link3.getNext()) {
        defCallArgs2.add(link3 == link2 ? (isLeft2 ? ExpressionFactory.Left() : ExpressionFactory.Right()) : new ReferenceExpression(link3));
      }
      myErrorReporter.report(new ConditionsError(definition.getDefCall(Sort.STD, defCallArgs1), definition.getDefCall(Sort.STD, defCallArgs2), substitution1, substitution2, evaluatedExpr1, evaluatedExpr2, def));
      return false;
    } else {
      return true;
    }
  }

  private boolean checkIntervalClause(IntervalElim elim, Clause clause, Definition definition) {
    boolean ok = true;
    List<IntervalElim.CasePair> cases = elim.getCases();
    int prefixLength = DependentLink.Helper.size(definition.getParameters()) - elim.getCases().size();
    for (int i = 0; i < cases.size(); i++) {
      ok = checkIntervalClauseCondition(cases.get(i), true, prefixLength + i, clause, definition) && ok;
      ok = checkIntervalClauseCondition(cases.get(i), false, prefixLength + i, clause, definition) && ok;
    }
    return ok;
  }

  private boolean checkIntervalClauseCondition(Pair<Expression, Expression> pair, boolean isLeft, int index, Clause clause, Definition definition) {
    Expression expr = isLeft ? pair.proj1 : pair.proj2;
    if (expr == null) {
      return true;
    }

    ExprSubstitution pathSubstitution = new ExprSubstitution();
    collectPaths(clause.patterns, pathSubstitution);

    ExprSubstitution substitution1 = new ExprSubstitution();
    DependentLink link = definition.getParameters();
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
    if (!CompareVisitor.compare(myEquations, Equations.CMP.EQ, evaluatedExpr1, evaluatedExpr2, null, clause.clause)) {
      if (!pathSubstitution.isEmpty()) {
        link = definition.getParameters();
        for (int i = 0; i < clause.patterns.size(); i++) {
          if (i != index) {
            substitution1.add(link, clause.patterns.get(i).toExpression());
          }
          link = link.getNext();
        }
      }

      List<Expression> defCallArgs1 = new ArrayList<>();
      int i = 0;
      for (link = definition.getParameters(); link.hasNext(); link = link.getNext(), i++) {
        defCallArgs1.add(i == index ? (isLeft ? ExpressionFactory.Left() : ExpressionFactory.Right()) : new ReferenceExpression(link));
      }

      List<Expression> defCallArgs2 = new ArrayList<>(clause.patterns.size());
      for (Pattern pattern : clause.patterns) {
        defCallArgs2.add(pattern.toExpression());
      }

      if (!pathSubstitution.isEmpty()) {
        substitution1 = new ExprSubstitution();
        link = definition.getParameters();
        for (i = 0; i < clause.patterns.size(); i++) {
          if (i != index) {
            substitution1.add(link, clause.patterns.get(i).toExpression());
          }
          link = link.getNext();
        }
      }

      myErrorReporter.report(new ConditionsError(definition.getDefCall(Sort.STD, defCallArgs1), definition.getDefCall(Sort.STD, defCallArgs2), substitution1, substitution2, evaluatedExpr1, evaluatedExpr2, clause.clause));
      return false;
    } else {
      return true;
    }
  }

  private static void collectPaths(List<Pattern> patterns, ExprSubstitution substitution) {
    for (Pattern pattern : patterns) {
      if (pattern instanceof ConstructorPattern) {
        ConstructorPattern conPattern = (ConstructorPattern) pattern;
        if (conPattern.getDefinition() == Prelude.PATH_CON) {
          SingleDependentLink lamParam = new TypedSingleDependentLink(true, "i", ExpressionFactory.Interval());
          Expression lamRef = new ReferenceExpression(lamParam);
          Map<CoreBranchKey, ElimTree> children = new HashMap<>();
          children.put(Prelude.LEFT, new LeafElimTree(EmptyDependentLink.getInstance(), conPattern.getDataTypeArguments().get(1)));
          children.put(Prelude.RIGHT, new LeafElimTree(EmptyDependentLink.getInstance(), conPattern.getDataTypeArguments().get(2)));
          children.put(null, new LeafElimTree(lamParam, AppExpression.make(conPattern.getArguments().get(0).toExpression(), lamRef)));
          substitution.add(((BindingPattern) conPattern.getArguments().get(0)).getBinding(), new LamExpression(conPattern.getSortArgument(), lamParam, new CaseExpression(false, lamParam, AppExpression.make(conPattern.getDataTypeArguments().get(0), lamRef), null, new BranchElimTree(EmptyDependentLink.getInstance(), children), Collections.singletonList(lamRef))));
        }
      }
    }
  }

  public boolean check(List<ExtClause> clauses, ElimTree elimTree) {
    boolean ok = true;
    for (ExtClause clause : clauses) {
      if (!checkClause(clause, elimTree, null)) {
        ok = false;
      }
    }
    return ok;
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean checkClause(ExtClause clause, ElimTree elimTree, Definition definition) {
    if (clause.expression == null) {
      return true;
    }
    Expression expr = clause.expression.getUnderlyingExpression();
    while (expr instanceof LetExpression || expr instanceof LamExpression) {
      expr = (expr instanceof LetExpression ? ((LetExpression) expr).getExpression() : ((LamExpression) expr).getBody()).getUnderlyingExpression();
    }
    if (expr instanceof ErrorExpression) {
      return true;
    }

    boolean ok = true;
    for (Pair<List<Expression>, ExprSubstitution> pair : collectPatterns(clause.patterns, clause.substitution, clause.clause)) {
      if (!clause.substitution.isEmpty()) {
        for (int i = 0; i < pair.proj1.size(); i++) {
          pair.proj1.set(i, pair.proj1.get(i).subst(clause.substitution));
        }
      }

      Expression evaluatedExpr1;
      if (definition == null) {
        evaluatedExpr1 = NormalizeVisitor.INSTANCE.eval(elimTree, pair.proj1, new ExprSubstitution(), LevelSubstitution.EMPTY);
      } else {
        evaluatedExpr1 = definition.getDefCall(Sort.STD, pair.proj1);
      }
      Expression evaluatedExpr2 = clause.expression.subst(pair.proj2);
      if (evaluatedExpr1 == null || !CompareVisitor.compare(myEquations, Equations.CMP.EQ, evaluatedExpr1, evaluatedExpr2, null, clause.clause)) {
        List<Expression> args = new ArrayList<>(clause.patterns.size());
        for (Pattern pattern : clause.patterns) {
          args.add(pattern.toExpression());
        }
        Expression expr1 = definition == null ? new CaseExpression(false, EmptyDependentLink.getInstance(), new ErrorExpression(null, null), null, new BranchElimTree(EmptyDependentLink.getInstance(), Collections.emptyMap()), args) : definition.getDefCall(Sort.STD, args);
        myErrorReporter.report(new ConditionsError(expr1, clause.expression, pair.proj2, pair.proj2, evaluatedExpr1, evaluatedExpr2, clause.clause));
        ok = false;
      }
    }
    return ok;
  }

  private List<Pair<List<Expression>, ExprSubstitution>> collectPatterns(List<Pattern> patterns, ExprSubstitution idpSubst, Concrete.SourceNode sourceNode) {
    List<Pair<List<Expression>, ExprSubstitution>> result = new ArrayList<>();
    for (int i = 0; i < patterns.size(); i++) {
      for (Pair<Expression, ExprSubstitution> pair : collectPatterns(patterns.get(i), idpSubst, sourceNode)) {
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

  private List<Pair<Expression, ExprSubstitution>> collectPatterns(Pattern pattern, ExprSubstitution idpSubst, Concrete.SourceNode sourceNode) {
    if (pattern instanceof BindingPattern) {
      return Collections.emptyList();
    }
    ConstructorPattern conPattern = (ConstructorPattern) pattern;
    List<Pair<List<Expression>, ExprSubstitution>> collectedPatterns = collectPatterns(conPattern.getArguments(), idpSubst, sourceNode);
    List<Pair<Expression, ExprSubstitution>> result = new ArrayList<>(collectedPatterns.size());
    for (Pair<List<Expression>, ExprSubstitution> pair : collectedPatterns) {
      result.add(new Pair<>(conPattern.toExpression(pair.proj1), pair.proj2));
    }

    if (!(conPattern.getDefinition() instanceof Constructor)) {
      return result;
    }
    Constructor constructor = (Constructor) conPattern.getDefinition();

    if (constructor.getBody() instanceof IntervalElim) {
      IntervalElim elim = (IntervalElim) constructor.getBody();
      int prefixLength = conPattern.getArguments().size() - elim.getCases().size();
      for (int i = 0; i < elim.getCases().size(); i++) {
        if (elim.getCases().get(i).proj1 == null && elim.getCases().get(i).proj2 == null) {
          continue;
        }

        ExprSubstitution substitution = new ExprSubstitution();
        int j = 0;
        for (DependentLink link = constructor.getParameters(); link.hasNext(); link = link.getNext(), j++) {
          substitution.add(link, conPattern.getArguments().get(j).toExpression());
        }
        j = 0;
        for (DependentLink link = constructor.getDataTypeParameters(); link.hasNext(); link = link.getNext(), j++) {
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
        if (pattern1 instanceof ConstructorPattern && (((ConstructorPattern) pattern1).getDefinition() == Prelude.LEFT || ((ConstructorPattern) pattern1).getDefinition() == Prelude.RIGHT)) {
          Expression expr;
          if (((ConstructorPattern) pattern1).getDefinition() == Prelude.LEFT && elim.getCases().get(i).proj1 != null) {
            expr = elim.getCases().get(i).proj1;
          } else
          if (((ConstructorPattern) pattern1).getDefinition() == Prelude.RIGHT && elim.getCases().get(i).proj2 != null) {
            expr = elim.getCases().get(i).proj2;
          } else {
            continue;
          }

          result.add(new Pair<>(expr.subst(substitution), new ExprSubstitution()));
        }
      }
    }

    for (ClauseBase clause : constructor.getClauses()) {
      ExprSubstitution substitution1 = new ExprSubstitution();
      ExprSubstitution substitution2 = new ExprSubstitution();
      if (conPattern.getPatterns().unify(idpSubst, new Patterns(clause.patterns), substitution1, substitution2, myErrorReporter, sourceNode)) {
        result.add(new Pair<>(clause.expression.subst(substitution2), substitution1));
      }
    }

    return result;
  }
}
