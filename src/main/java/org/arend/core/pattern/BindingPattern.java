package org.arend.core.pattern;

import org.arend.core.context.param.DependentLink;
import org.arend.core.definition.Definition;
import org.arend.core.expr.Expression;
import org.arend.core.expr.ReferenceExpression;
import org.arend.core.subst.ExprSubstitution;
import org.arend.core.subst.LevelSubstitution;
import org.arend.ext.error.ErrorReporter;
import org.arend.term.concrete.Concrete;
import org.arend.typechecking.error.local.PatternUnificationError;
import org.arend.util.Decision;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BindingPattern implements ExpressionPattern {
  private final DependentLink myBinding;

  public BindingPattern(DependentLink binding) {
    myBinding = binding;
  }

  public DependentLink getBinding() {
    return myBinding;
  }

  @Override
  public Expression toExpression() {
    return new ReferenceExpression(myBinding);
  }

  @Override
  public DependentLink getFirstBinding() {
    return myBinding;
  }

  @Override
  public DependentLink getLastBinding() {
    return myBinding;
  }

  @Override
  public Definition getDefinition() {
    return null;
  }

  @Override
  public List<? extends Pattern> getSubPatterns() {
    return Collections.emptyList();
  }

  @Override
  public DependentLink replaceBindings(DependentLink link, List<Pattern> result) {
    result.add(new BindingPattern(link));
    return link.getNext();
  }

  @Override
  public ExpressionPattern toExpressionPattern(Expression type) {
    return this;
  }

  @Override
  public Decision match(Expression expression, List<Expression> result) {
    if (result != null) {
      result.add(expression);
    }
    return Decision.YES;
  }

  @Override
  public boolean unify(ExprSubstitution idpSubst, ExpressionPattern other, ExprSubstitution substitution1, ExprSubstitution substitution2, ErrorReporter errorReporter, Concrete.SourceNode sourceNode) {
    Expression substExpr = idpSubst == null ? null : idpSubst.get(myBinding);
    if (substExpr != null) {
      if (other instanceof BindingPattern || other instanceof EmptyPattern) {
        substitution1.add(myBinding, substExpr);
        return true;
      } else {
        errorReporter.report(new PatternUnificationError(myBinding, other, sourceNode));
        return false;
      }
    }

    if (!(other instanceof EmptyPattern) && substitution1 != null) {
      substitution1.add(myBinding, other.toExpression());
    }
    return true;
  }

  @Override
  public ExpressionPattern subst(ExprSubstitution exprSubst, LevelSubstitution levelSubst, Map<DependentLink, ExpressionPattern> patternSubst) {
    if (patternSubst == null) {
      return this;
    }
    ExpressionPattern result = patternSubst.get(myBinding);
    assert result != null;
    return result;
  }

  @Override
  public Pattern removeExpressions() {
    return this;
  }
}
