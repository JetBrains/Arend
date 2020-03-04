package org.arend.typechecking.patternmatching;

import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.context.param.TypedSingleDependentLink;
import org.arend.core.definition.Constructor;
import org.arend.core.definition.Definition;
import org.arend.core.definition.Function;
import org.arend.core.elimtree.*;
import org.arend.core.expr.*;
import org.arend.core.expr.visitor.CompareVisitor;
import org.arend.core.expr.visitor.NormalizeVisitor;
import org.arend.core.pattern.*;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.ext.core.ops.CMP;
import org.arend.ext.error.ErrorReporter;
import org.arend.prelude.Prelude;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.ConditionsError;
import org.arend.typechecking.implicitargs.equations.Equations;
import org.arend.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ConditionsChecking {
  private final Equations myEquations;
  private final ErrorReporter myErrorReporter;
  private final Concrete.SourceNode mySourceNode;

  public ConditionsChecking(Equations equations, ErrorReporter errorReporter, Concrete.SourceNode sourceNode) {
    myEquations = equations;
    myErrorReporter = errorReporter;
    mySourceNode = sourceNode;
  }

  public boolean check(Body body, List<ExtElimClause> clauses, List<? extends Concrete.FunctionClause> cClauses, Definition definition) {
    assert clauses.size() <= cClauses.size();

    boolean ok;
    if (body instanceof IntervalElim) {
      ok = checkIntervals((IntervalElim) body, definition);
      for (int i = 0; i < clauses.size(); i++) {
        if (clauses.get(i).getExpression() != null && !checkIntervalClause((IntervalElim) body, clauses.get(i), cClauses.get(i), definition)) {
          ok = false;
        }
      }
    } else {
      ok = true;
    }

    for (int i = 0; i < clauses.size(); i++) {
      if (!checkClause(clauses.get(i), cClauses.get(i), null, definition)) {
        ok = false;
      }
    }

    return ok;
  }

  private boolean checkIntervals(IntervalElim elim, Definition definition) {
    boolean ok = true;
    DependentLink link = DependentLink.Helper.get(definition.getParameters(), DependentLink.Helper.size(definition.getParameters()) - elim.getCases().size());
    List<IntervalElim.CasePair> cases = elim.getCases();
    for (int i = 0; i < cases.size(); i++) {
      DependentLink link2 = link.getNext();
      for (int j = i + 1; j < cases.size(); j++) {
        ok = checkIntervalCondition(cases.get(i), cases.get(j), true, true, link, link2, definition) && ok;
        ok = checkIntervalCondition(cases.get(i), cases.get(j), true, false, link, link2, definition) && ok;
        ok = checkIntervalCondition(cases.get(i), cases.get(j), false, true, link, link2, definition) && ok;
        ok = checkIntervalCondition(cases.get(i), cases.get(j), false, false, link, link2, definition) && ok;
        link2 = link2.getNext();
      }
      link = link.getNext();
    }
    return ok;
  }

  private boolean checkIntervalCondition(Pair<Expression, Expression> pair1, Pair<Expression, Expression> pair2, boolean isLeft1, boolean isLeft2, DependentLink link1, DependentLink link2, Definition definition) {
    Expression case1 = isLeft1 ? pair1.proj1 : pair1.proj2;
    Expression case2 = isLeft2 ? pair2.proj1 : pair2.proj2;
    if (case1 == null || case2 == null) {
      return true;
    }

    ExprSubstitution substitution1 = new ExprSubstitution(link2, isLeft2 ? ExpressionFactory.Left() : ExpressionFactory.Right());
    Expression evaluatedExpr1 = case1.subst(substitution1);
    ExprSubstitution substitution2 = new ExprSubstitution(link1, isLeft1 ? ExpressionFactory.Left() : ExpressionFactory.Right());
    Expression evaluatedExpr2 = case2.subst(substitution2);
    if (!CompareVisitor.compare(myEquations, CMP.EQ, evaluatedExpr1, evaluatedExpr2, null, mySourceNode)) {
      List<Expression> defCallArgs1 = new ArrayList<>();
      for (DependentLink link3 = definition.getParameters(); link3.hasNext(); link3 = link3.getNext()) {
        defCallArgs1.add(link3 == link1 ? (isLeft1 ? ExpressionFactory.Left() : ExpressionFactory.Right()) : new ReferenceExpression(link3));
      }
      List<Expression> defCallArgs2 = new ArrayList<>();
      for (DependentLink link3 = definition.getParameters(); link3.hasNext(); link3 = link3.getNext()) {
        defCallArgs2.add(link3 == link2 ? (isLeft2 ? ExpressionFactory.Left() : ExpressionFactory.Right()) : new ReferenceExpression(link3));
      }
      myErrorReporter.report(new ConditionsError(new Condition(definition.getDefCall(Sort.STD, defCallArgs1), substitution1, evaluatedExpr1), new Condition(definition.getDefCall(Sort.STD, defCallArgs2), substitution2, evaluatedExpr2), mySourceNode));
      return false;
    } else {
      return true;
    }
  }

  private boolean checkIntervalClause(IntervalElim elim, ElimClause<ExpressionPattern> clause, Concrete.FunctionClause cClause, Definition definition) {
    boolean ok = true;
    List<IntervalElim.CasePair> cases = elim.getCases();
    int prefixLength = DependentLink.Helper.size(definition.getParameters()) - elim.getCases().size();
    for (int i = 0; i < cases.size(); i++) {
      ok = checkIntervalClauseCondition(cases.get(i), true, prefixLength + i, clause, cClause, definition) && ok;
      ok = checkIntervalClauseCondition(cases.get(i), false, prefixLength + i, clause, cClause, definition) && ok;
    }
    return ok;
  }

  private boolean checkIntervalClauseCondition(Pair<Expression, Expression> pair, boolean isLeft, int index, ElimClause<ExpressionPattern> clause, Concrete.FunctionClause cClause, Definition definition) {
    Expression expr = isLeft ? pair.proj1 : pair.proj2;
    if (expr == null || clause.getExpression() == null) {
      return true;
    }

    ExprSubstitution pathSubstitution = new ExprSubstitution();
    collectPaths(clause.getPatterns(), pathSubstitution);

    ExprSubstitution substitution1 = new ExprSubstitution();
    DependentLink link = definition.getParameters();
    for (int i = 0; i < clause.getPatterns().size(); i++) {
      if (i != index) {
        substitution1.add(link, clause.getPatterns().get(i).toExpression().subst(pathSubstitution));
      }
      link = link.getNext();
    }

    ExprSubstitution substitution2 = new ExprSubstitution(((BindingPattern) clause.getPatterns().get(index)).getBinding(), isLeft ? ExpressionFactory.Left() : ExpressionFactory.Right());
    pathSubstitution.addAll(substitution2);

    Expression evaluatedExpr1 = expr.subst(substitution1);
    Expression evaluatedExpr2 = clause.getExpression().subst(pathSubstitution);
    if (!CompareVisitor.compare(myEquations, CMP.EQ, evaluatedExpr1, evaluatedExpr2, null, cClause)) {
      if (!pathSubstitution.isEmpty()) {
        link = definition.getParameters();
        for (int i = 0; i < clause.getPatterns().size(); i++) {
          if (i != index) {
            substitution1.add(link, clause.getPatterns().get(i).toExpression());
          }
          link = link.getNext();
        }
      }

      List<Expression> defCallArgs1 = new ArrayList<>();
      int i = 0;
      for (link = definition.getParameters(); link.hasNext(); link = link.getNext(), i++) {
        defCallArgs1.add(i == index ? (isLeft ? ExpressionFactory.Left() : ExpressionFactory.Right()) : new ReferenceExpression(link));
      }

      List<Expression> defCallArgs2 = new ArrayList<>(clause.getPatterns().size());
      for (ExpressionPattern pattern : clause.getPatterns()) {
        defCallArgs2.add(pattern.toExpression());
      }

      if (!pathSubstitution.isEmpty()) {
        substitution1 = new ExprSubstitution();
        link = definition.getParameters();
        for (i = 0; i < clause.getPatterns().size(); i++) {
          if (i != index) {
            substitution1.add(link, clause.getPatterns().get(i).toExpression());
          }
          link = link.getNext();
        }
      }

      myErrorReporter.report(new ConditionsError(new Condition(definition.getDefCall(Sort.STD, defCallArgs1), substitution1, evaluatedExpr1), new Condition(definition.getDefCall(Sort.STD, defCallArgs2), substitution2, evaluatedExpr2), cClause));
      return false;
    } else {
      return true;
    }
  }

  private static void collectPaths(List<ExpressionPattern> patterns, ExprSubstitution substitution) {
    for (ExpressionPattern pattern : patterns) {
      if (pattern instanceof ConstructorExpressionPattern) {
        ConstructorExpressionPattern conPattern = (ConstructorExpressionPattern) pattern;
        if (conPattern.getDefinition() == Prelude.PATH_CON) {
          SingleDependentLink lamParam = new TypedSingleDependentLink(true, "i", ExpressionFactory.Interval());
          Expression lamRef = new ReferenceExpression(lamParam);
          List<ElimClause<Pattern>> clauses = new ArrayList<>(3);
          clauses.add(new ElimClause<>(Collections.singletonList(ConstructorPattern.make(Prelude.LEFT, Collections.emptyList())), conPattern.getDataTypeArguments().get(1)));
          clauses.add(new ElimClause<>(Collections.singletonList(ConstructorPattern.make(Prelude.RIGHT, Collections.emptyList())), conPattern.getDataTypeArguments().get(2)));
          clauses.add(new ElimClause<>(Collections.singletonList(new BindingPattern(lamParam)), AppExpression.make(conPattern.getSubPatterns().get(0).toExpression(), lamRef)));
          BranchElimTree elimTree = new BranchElimTree(0, true);
          elimTree.addChild(Prelude.LEFT, new LeafElimTree(0, Collections.emptyList(), 0));
          elimTree.addChild(Prelude.RIGHT, new LeafElimTree(0, Collections.emptyList(), 1));
          elimTree.addChild(null, new LeafElimTree(0, null, 1));
          substitution.add(((BindingPattern) conPattern.getSubPatterns().get(0)).getBinding(), new LamExpression(conPattern.getSortArgument(), lamParam, new CaseExpression(false, lamParam, AppExpression.make(conPattern.getDataTypeArguments().get(0), lamRef), null, new ElimBody(clauses, elimTree), Collections.singletonList(lamRef))));
        }
      }
    }
  }

  public boolean check(List<ExtElimClause> clauses, List<? extends Concrete.FunctionClause> cClauses, ElimBody elimBody) {
    assert cClauses == null || clauses.size() <= cClauses.size();

    boolean ok = true;
    for (int i = 0; i < clauses.size(); i++) {
      if (!checkClause(clauses.get(i), cClauses == null ? mySourceNode : cClauses.get(i), elimBody, null)) {
        ok = false;
      }
    }
    return ok;
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean checkClause(ExtElimClause clause, Concrete.SourceNode sourceNode, ElimBody elimBody, Definition definition) {
    if (clause.getExpression() == null) {
      return true;
    }
    Expression expr = clause.getExpression().getUnderlyingExpression();
    while (expr instanceof LetExpression || expr instanceof LamExpression) {
      expr = (expr instanceof LetExpression ? ((LetExpression) expr).getExpression() : ((LamExpression) expr).getBody()).getUnderlyingExpression();
    }
    if (expr.isError()) {
      return true;
    }

    if (elimBody == null && definition instanceof Function) {
      Body body = ((Function) definition).getBody();
      if (body instanceof ElimBody) {
        elimBody = (ElimBody) body;
      }
    }

    boolean ok = true;
    for (Pair<List<Expression>, ExprSubstitution> pair : collectPatterns(clause.getPatterns(), clause.getSubstitution(), sourceNode)) {
      if (!clause.getSubstitution().isEmpty()) {
        for (int i = 0; i < pair.proj1.size(); i++) {
          pair.proj1.set(i, pair.proj1.get(i).subst(clause.getSubstitution()));
        }
      }

      Expression evaluatedExpr1;
      if (elimBody != null && (definition == null || expr instanceof GoalErrorExpression)) {
        evaluatedExpr1 = NormalizeVisitor.INSTANCE.eval(elimBody, pair.proj1, new ExprSubstitution(), LevelSubstitution.EMPTY);
        if (evaluatedExpr1 == null && definition != null) {
          evaluatedExpr1 = definition.getDefCall(Sort.STD, pair.proj1);
        }
      } else {
        evaluatedExpr1 = definition.getDefCall(Sort.STD, pair.proj1);
      }

      if (expr instanceof GoalErrorExpression) {
        if (evaluatedExpr1 != null) {
          ((GoalErrorExpression) expr).goalError.addCondition(new Condition(null, pair.proj2, evaluatedExpr1));
        }
      } else {
        Expression evaluatedExpr2 = expr.subst(pair.proj2);
        if (evaluatedExpr1 == null || !CompareVisitor.compare(myEquations, CMP.EQ, evaluatedExpr1, evaluatedExpr2, null, sourceNode)) {
          List<Expression> args = new ArrayList<>();
          for (ExpressionPattern pattern : clause.getPatterns()) {
            args.add(pattern.toExpression());
          }
          Expression expr1 = definition == null ? new CaseExpression(false, EmptyDependentLink.getInstance(), new ErrorExpression(), null, new ElimBody(Collections.emptyList(), new BranchElimTree(0, false)), args) : definition.getDefCall(Sort.STD, args);
          myErrorReporter.report(new ConditionsError(new Condition(expr1, pair.proj2, evaluatedExpr1), new Condition(clause.getExpression(), pair.proj2, evaluatedExpr2), sourceNode));
          ok = false;
        }
      }
    }
    return ok;
  }

  private List<Pair<List<Expression>, ExprSubstitution>> collectPatterns(List<? extends ExpressionPattern> patterns, ExprSubstitution idpSubst, Concrete.SourceNode sourceNode) {
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

  private List<Pair<Expression, ExprSubstitution>> collectPatterns(ExpressionPattern pattern, ExprSubstitution idpSubst, Concrete.SourceNode sourceNode) {
    if (pattern instanceof BindingPattern) {
      return Collections.emptyList();
    }
    ConstructorExpressionPattern conPattern = (ConstructorExpressionPattern) pattern;
    List<Pair<List<Expression>, ExprSubstitution>> collectedPatterns = collectPatterns(conPattern.getSubPatterns(), idpSubst, sourceNode);
    List<Pair<Expression, ExprSubstitution>> result = new ArrayList<>(collectedPatterns.size());
    for (Pair<List<Expression>, ExprSubstitution> pair : collectedPatterns) {
      result.add(new Pair<>(conPattern.toExpression(pair.proj1), pair.proj2));
    }

    if (!(conPattern.getDefinition() instanceof Constructor)) {
      return result;
    }
    Constructor constructor = (Constructor) conPattern.getDefinition();

    ElimBody elimBody;
    if (constructor.getBody() instanceof IntervalElim) {
      IntervalElim elim = (IntervalElim) constructor.getBody();
      elimBody = elim.getOtherwise();
      int prefixLength = conPattern.getSubPatterns().size() - elim.getCases().size();
      for (int i = 0; i < elim.getCases().size(); i++) {
        if (elim.getCases().get(i).proj1 == null && elim.getCases().get(i).proj2 == null) {
          continue;
        }

        ExprSubstitution substitution = new ExprSubstitution();
        int j = 0;
        for (DependentLink link = constructor.getParameters(); link.hasNext(); link = link.getNext(), j++) {
          substitution.add(link, conPattern.getSubPatterns().get(j).toExpression());
        }
        j = 0;
        for (DependentLink link = constructor.getDataTypeParameters(); link.hasNext(); link = link.getNext(), j++) {
          substitution.add(link, conPattern.getDataTypeArguments().get(j));
        }

        ExpressionPattern pattern1 = conPattern.getSubPatterns().get(prefixLength + i);
        if (pattern1 instanceof BindingPattern) {
          if (elim.getCases().get(i).proj1 != null) {
            result.add(new Pair<>(elim.getCases().get(i).proj1.subst(substitution), new ExprSubstitution(((BindingPattern) pattern1).getBinding(), ExpressionFactory.Left())));
          }
          if (elim.getCases().get(i).proj2 != null) {
            result.add(new Pair<>(elim.getCases().get(i).proj2.subst(substitution), new ExprSubstitution(((BindingPattern) pattern1).getBinding(), ExpressionFactory.Right())));
          }
        } else
        if (pattern1 instanceof ConstructorExpressionPattern && (pattern1.getDefinition() == Prelude.LEFT || pattern1.getDefinition() == Prelude.RIGHT)) {
          Expression expr;
          if (pattern1.getDefinition() == Prelude.LEFT && elim.getCases().get(i).proj1 != null) {
            expr = elim.getCases().get(i).proj1;
          } else
          if (pattern1.getDefinition() == Prelude.RIGHT && elim.getCases().get(i).proj2 != null) {
            expr = elim.getCases().get(i).proj2;
          } else {
            continue;
          }

          result.add(new Pair<>(expr.subst(substitution), new ExprSubstitution()));
        }
      }
    } else {
      elimBody = constructor.getBody() instanceof ElimBody ? (ElimBody) constructor.getBody() : null;
    }

    if (elimBody != null) {
      for (ElimClause<Pattern> clause : elimBody.getClauses()) {
        if (clause.getExpression() == null) {
          continue;
        }
        ExprSubstitution substitution1 = new ExprSubstitution();
        ExprSubstitution substitution2 = new ExprSubstitution();
        if (ExpressionPattern.unify(conPattern.getSubPatterns(), Objects.requireNonNull(Pattern.toExpressionPatterns(clause.getPatterns(), constructor.getParameters())), idpSubst, substitution1, substitution2, myErrorReporter, sourceNode)) {
          result.add(new Pair<>(clause.getExpression().subst(substitution2), substitution1));
        }
      }
    }

    return result;
  }
}
