package org.arend.core.pattern;

import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.EmptyDependentLink;
import org.arend.core.definition.Definition;
import org.arend.core.expr.Expression;
import org.arend.core.expr.SigmaExpression;
import org.arend.core.expr.TupleExpression;
import org.arend.core.sort.Sort;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.ext.core.body.CorePattern;
import org.arend.ext.core.context.CoreBinding;
import org.arend.ext.error.ErrorReporter;
import org.arend.term.concrete.Concrete;
import org.arend.util.Decision;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class EmptyPattern implements ExpressionPattern {
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
  public Definition getDefinition() {
    return null;
  }

  @NotNull
  @Override
  public List<ExpressionPattern> getSubPatterns() {
    return Collections.emptyList();
  }

  @Override
  public Concrete.Pattern toConcrete(Object data, boolean isExplicit, Map<DependentLink, Concrete.Pattern> subPatterns) {
    return new Concrete.TuplePattern(data, isExplicit, Collections.emptyList(), Collections.emptyList());
  }

  @Override
  public boolean isAbsurd() {
    return true;
  }

  @Override
  public @NotNull Pattern subst(@NotNull Map<? extends CoreBinding, ? extends CorePattern> map) {
    return this;
  }

  @Override
  public DependentLink replaceBindings(DependentLink link, List<Pattern> result) {
    result.add(INSTANCE);
    return link;
  }

  @Override
  public ExpressionPattern toExpressionPattern(Expression type) {
    return this;
  }

  @Override
  public Decision match(Expression expression, List<Expression> result) {
    return Decision.NO;
  }

  @Override
  public boolean unify(ExprSubstitution idpSubst, ExpressionPattern other, ExprSubstitution substitution1, ExprSubstitution substitution2, ErrorReporter errorReporter, Concrete.SourceNode sourceNode) {
    return other instanceof EmptyPattern || other instanceof BindingPattern;
  }

  @Override
  public @Nullable ExpressionPattern intersect(ExpressionPattern other) {
    return other instanceof EmptyPattern || other instanceof BindingPattern ? this : null;
  }

  @Override
  public ExpressionPattern subst(ExprSubstitution exprSubst, LevelSubstitution levelSubst, Map<DependentLink, ExpressionPattern> patternSubst) {
    return this;
  }

  @Override
  public Pattern removeExpressions() {
    return this;
  }
}
