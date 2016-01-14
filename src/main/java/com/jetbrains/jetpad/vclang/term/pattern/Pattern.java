package com.jetbrains.jetpad.vclang.term.pattern;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.context.binding.Binding;
import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.Expression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jetbrains.jetpad.vclang.term.pattern.Utils.prettyPrintPattern;

public abstract class Pattern implements Abstract.Pattern {
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    prettyPrintPattern(this, builder, new ArrayList<String>());
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

  public Expression toExpression() {
    return toExpression(new HashMap<Binding, Expression>());
  }

  public abstract DependentLink getParameters();
  public abstract Expression toExpression(Map<Binding, Expression> substs);
  public abstract MatchResult match(Expression expr);
}
