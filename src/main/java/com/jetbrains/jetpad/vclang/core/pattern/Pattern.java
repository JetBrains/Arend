package com.jetbrains.jetpad.vclang.core.pattern;

import com.jetbrains.jetpad.vclang.core.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.core.expr.Expression;
import com.jetbrains.jetpad.vclang.core.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.prettyprint.PrettyPrintVisitor;

import java.util.List;

public abstract class Pattern implements Abstract.Pattern {
  private boolean myExplicit = true;

  @Override
  public boolean isExplicit() {
    return myExplicit;
  }

  public void setExplicit(boolean isExplicit) {
    myExplicit = isExplicit;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    new PrettyPrintVisitor(builder, 0).prettyPrintPattern(this, Abstract.Pattern.PREC);
    return builder.toString();
  }

  @Override
  public void setWellTyped(Pattern pattern) {

  }

  public static class MatchResult {}

  public static class MatchOKResult extends MatchResult {
    public final List<Expression> expressions;

    MatchOKResult(List<Expression> expressions) {
      this.expressions = expressions;
    }
  }

  public static class MatchFailedResult extends MatchResult {
    public final ConstructorPattern failedPattern;
    public final Expression actualExpression;

    MatchFailedResult(ConstructorPattern failedPattern, Expression actualExpression) {
      this.failedPattern = failedPattern;
      this.actualExpression = actualExpression;
    }
  }

  public static class MatchMaybeResult extends MatchResult {
    public final Pattern maybePattern;
    public final Expression actualExpression;

    MatchMaybeResult(Pattern maybePattern, Expression actualExpression) {
      this.maybePattern = maybePattern;
      this.actualExpression = actualExpression;
    }
  }

  public abstract DependentLink getParameters();
  public abstract Expression toExpression(ExprSubstitution subst);
  public MatchResult match(Expression expr) {
    return match(expr, true);
  }
  public abstract MatchResult match(Expression expr, boolean normalize);
}
