package com.jetbrains.jetpad.vclang.term.pattern;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import com.jetbrains.jetpad.vclang.term.expr.subst.ExprSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.subst.LevelSubstitution;
import com.jetbrains.jetpad.vclang.term.expr.visitor.PrettyPrintVisitor;

import java.util.List;

public abstract class Pattern implements Abstract.Pattern {
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    new PrettyPrintVisitor(builder, 0).prettyPrintPattern(this);
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
  public abstract Expression toExpression(ExprSubstitution subst, LevelSubstitution polyParams);
  public MatchResult match(Expression expr) {
    return match(expr, true);
  }
  public abstract MatchResult match(Expression expr, boolean normalize);
}
