package org.arend.core.pattern;

import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.expr.Expression;
import org.arend.core.expr.SigmaExpression;
import org.arend.core.expr.TupleExpression;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.error.ErrorReporter;
import org.arend.term.concrete.Concrete;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class EmptyPattern implements Pattern {
  public final static EmptyPattern INSTANCE = new EmptyPattern();

  private EmptyPattern() {}

  @Override
  public Expression toExpression() {
    return null;
  }

  @Override
  public Expression toPatternExpression() {
    return new TupleExpression(Collections.emptyList(), new SigmaExpression(Sort.PROP, EmptyDependentLink.getInstance()));
  }

  @Override
  public DependentLink getFirstBinding() {
    return EmptyDependentLink.getInstance();
  }

  @Override
  public DependentLink getLastBinding() {
    return EmptyDependentLink.getInstance();
  }

  @Override
  public MatchResult match(Expression expression, List<Expression> result) {
    return MatchResult.FAIL;
  }

  @Override
  public boolean unify(ExprSubstitution idpSubst, Pattern other, ExprSubstitution substitution1, ExprSubstitution substitution2, ErrorReporter errorReporter, Concrete.SourceNode sourceNode) {
    return other instanceof EmptyPattern || other instanceof BindingPattern;
  }

  @Override
  public Pattern subst(ExprSubstitution exprSubst, LevelSubstitution levelSubst, Map<DependentLink, Pattern> patternSubst) {
    return this;
  }
}
