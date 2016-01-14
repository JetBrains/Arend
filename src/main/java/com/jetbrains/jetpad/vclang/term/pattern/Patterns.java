package com.jetbrains.jetpad.vclang.term.pattern;

import com.jetbrains.jetpad.vclang.term.context.param.DependentLink;
import com.jetbrains.jetpad.vclang.term.expr.ArgumentExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;

import java.util.ArrayList;
import java.util.List;

public class Patterns {
  private final List<PatternArgument> myPatterns;

  public Patterns(List<PatternArgument> patterns) {
    myPatterns = patterns;
  }

  public List<PatternArgument> getPatterns() {
    return myPatterns;
  }

  public DependentLink getParameters() {
    for (PatternArgument pattern : myPatterns) {
      DependentLink result = pattern.getPattern().getParameters();
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  public Pattern.MatchResult match(List<Expression> exprs) {
    assert myPatterns.size() == exprs.size();
    List<Expression> result = new ArrayList<>();

    Pattern.MatchMaybeResult maybe = null;
    for (int i = 0; i < myPatterns.size(); i++) {
      Pattern.MatchResult subMatch = myPatterns.get(i).getPattern().match(exprs.get(i));
      if (subMatch instanceof Pattern.MatchFailedResult) {
        return subMatch;
      } else
      if (subMatch instanceof Pattern.MatchMaybeResult) {
        if (maybe == null) {
          maybe = (Pattern.MatchMaybeResult) subMatch;
        }
      } else
      if (subMatch instanceof Pattern.MatchOKResult) {
        result.addAll(((Pattern.MatchOKResult) subMatch).expressions);
      }
    }

    return maybe != null ? maybe : new Pattern.MatchOKResult(result);
  }

  public List<ArgumentExpression> toExpressions() {
    List<ArgumentExpression> result = new ArrayList<>(myPatterns.size());
    for (PatternArgument pattern : myPatterns) {
      result.add(new ArgumentExpression(pattern.getPattern().toExpression(), pattern.isExplicit(), pattern.isHidden()));
    }
    return result;
  }
}
